// package com.holix.android.bottomsheetdialog.compose.org.saltedfish.chatpsg
package org.saltedfish.chatpsg

import android.content.Context
import android.os.Environment
import org.saltedfish.chatbot.saveTextToDocuments
import java.io.File
import kotlin.toString

fun parsePSGText(rawText: String): PSGReport {
    fun extract(pattern: String): MatchResult? = Regex(pattern, RegexOption.DOT_MATCHES_ALL).find(rawText)
    fun extractAll(pattern: String): List<MatchResult> = Regex(pattern, RegexOption.DOT_MATCHES_ALL).findAll(rawText).toList()

    val patient = extract("""Name\s*:\s*(\S+)\s+Age\s*:\s*(\d+)\s+Sex\s*:\s*(\w+).*?Study Date\s*:\s*([\d.]+)""")?.groupValues
        ?: listOf("", "", "0", "", "")

    val vitals = extract("""Height\s*:\s*(\d+)\s+Weight\s*:\s*(\d+)\s+BMI\s*:\s*([\d.]+)\s+NC\s*:\s*([\d.]+)\s+WC\s*:\s*([\d.]+)\s+HipC\s*:\s*([\d.]+)\s+WHR\s*:\s*([\d.]+)""")?.groupValues
        ?: listOf("", "0", "0", "0", "0", "0", "0", "0")

    val sleepEfficiency = extract("""Sleep\s*Efficiency\s*([\d.]+)""")?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0

    val sleepTiming = extractAll("""(Light Out|Light On|TRT|TST|Sleep Latency|WASO|Stage R\s*Latency)\s+(\d+:\d+)(?:\s*/\s*([\d.]+))?""")
        .associate {
            val label = it.groupValues[1].replace("""\s+""".toRegex(), " ")
            val time = it.groupValues[2]
            val percent = it.groupValues.getOrNull(3)?.toDoubleOrNull()
            label to (time to percent)
        }

    val sleepStages = extractAll("""(N1|N2|N3|NREM|REM)\s+(\d+:\d+)\s*/\s*([\d.]+)""")
        .associate {
            val stage = it.groupValues[1]
            val time = it.groupValues[2]
            val percent = it.groupValues[3].toDoubleOrNull() ?: 0.0
            stage to (time to percent)
        }

    val arousalEvents = extractAll("""([A-Za-z ]+Arousal)\s+(\d+)\s+([\d.]+)""")
        .associate {
            val label = it.groupValues[1].trim()
            val count = it.groupValues[2].toIntOrNull() ?: 0
            val index = it.groupValues[3].toDoubleOrNull() ?: 0.0
            label to DurationIndex(count, index)
        }

    val respiratorySections = listOf("Total", "REM", "NREM", "Supine", "Non-Supine")
    val respiratoryLabels = listOf("OSA", "MSA", "CSA", "OSH", "CSH", "ODI", "AHI", "RERA", "RDI")
    val respiratoryEvents = respiratorySections.mapNotNull { section ->
        val pattern = "$section\\s+((?:\\d+\\s*/\\s*[\\d.]+\\s+)+)"
        val block = Regex(pattern).find(rawText)?.groupValues?.get(1)
        block?.let {
            val values = Regex("(\\d+)\\s*/\\s*([\\d.]+)").findAll(it).map { m ->
                m.groupValues[1].toInt() to m.groupValues[2].toDouble()
            }.toList()
            if (values.size == respiratoryLabels.size) {
                section to respiratoryLabels.zip(values).associate { (k, v) -> k to DurationIndex(v.first, v.second) }
            } else null
        }
    }.toMap()

    val meanRespDur = extract("""Mean\s+Duration\(sec\)\s+((?:\d+\s+)+)""")?.groupValues?.get(1)?.trim()
        ?.split("""\s+""".toRegex())
        ?.mapIndexedNotNull { i, v -> respiratoryLabels.getOrNull(i)?.let { it to v.toIntOrNull() } }
        ?.toMap() ?: emptyMap()

    val maxRespDur = extract("""Max\s+Duration\(sec\)\s+((?:\d+|-\s+)+)""")?.groupValues?.get(1)?.trim()
        ?.split("""\s+""".toRegex())
        ?.mapIndexedNotNull { i, v -> respiratoryLabels.getOrNull(i)?.let { it to v.toIntOrNull() } }
        ?.toMap() ?: emptyMap()

    val o2 = extract("""Mean Oxygen Saturation.*?([\d.]+)\s+Lowest Oxygen Saturation.*?([\d.]+)""")?.groupValues
        ?: listOf("", "0.0", "0.0")

    val snore = extract("""Total Snoring Time\s*\(min\)\s*(\d+:\d+)\s+Total Snoring Time\s*\(%\)\s*([\d.]+)""")?.groupValues
        ?: listOf("", "0:00", "0.0")

    val heart = extract(
        """Average\s+Heart\s+Rate\s*[:\-]?\s*([\d.]+).*?Highest\s+Heart\s+Rate\s*[:\-]?\s*([\d.]+).*?Lowest\s+Heart\s+Rate\s*[:\-]?\s*([\d.]+)"""
    )?.groupValues ?: listOf("", "0.0", "0.0", "0.0")


    val breathingAnomalies = extractAll("""(EtCO2\(>50mmHg\)|Hypoventilation|Cheyne-Stokes Breathing|Periodic Breathing)\s+(Y|N)""")
        .associate { it.groupValues[1] to it.groupValues[2] }

    val cardiacEvents = extractAll("""(Bradycardia|Sinus Tachycardia|Atrial fibrillation|Arrhythmia|PVC)\s+(Y|N)""")
        .associate { it.groupValues[1] to it.groupValues[2] }

    val movementEvents = extractAll("""(LM|LM Arousal|PLMS|PLMS Arousal)\s+(\d+)\s+([\d.]+)""")
        .associate { it.groupValues[1] to DurationIndex(it.groupValues[2].toInt(), it.groupValues[3].toDouble()) }

    val diseasePrediction = mutableMapOf<String, Map<String, String>>()
    extract("""Expected to Occur withn\s+10 years\s+([\w.<>\"\s]+?)\s+Arrhythmia\s+(\d+%)\s+Expected to Occur withn\s+20 years\s+([\w.<>\"\s]+?)\s+Cerebrovascular Disease\s+(\d+%)""")?.let {
        diseasePrediction["10yr"] = mapOf("Dementia" to it.groupValues[1].trim(), "Arrhythmia Risk" to it.groupValues[2])
        diseasePrediction["20yr"] = mapOf("Dementia" to it.groupValues[3].trim(), "Cerebrovascular Risk" to it.groupValues[4])
    }
    extract("""Expected to Occur in life\s*time\s+(less than \d+%|\d+%)""")?.groupValues?.getOrNull(1)?.let {
        diseasePrediction["Lifetime"] = mapOf("Risk" to it)
    }

    return PSGReport(
        patientInfo = PatientInfo(
            name = patient.getOrElse(1) { "" },
            age = patient.getOrElse(2) { "0" }.toIntOrNull() ?: 0,
            gender = patient.getOrElse(3) { "" },
            date = patient.getOrElse(4) { "" }
        ),
        vitals = Vitals(
            height = vitals[1].toIntOrNull() ?: 0,
            weight = vitals[2].toIntOrNull() ?: 0,
            bmi = vitals[3].toDoubleOrNull() ?: 0.0,
            nc = vitals[4].toDoubleOrNull() ?: 0.0,
            wc = vitals[5].toDoubleOrNull() ?: 0.0,
            hipc = vitals[6].toDoubleOrNull() ?: 0.0,
            whr = vitals[7].toDoubleOrNull() ?: 0.0
        ),
        sleepTiming = sleepTiming,
        sleepEfficiency = sleepEfficiency,
        sleepStages = sleepStages,
        arousalEvents = arousalEvents,
        respiratoryEvents = respiratoryEvents,
        meanRespDur = meanRespDur.mapValues { it.value ?: 0 },
        maxRespDur = maxRespDur,
        oxygen = Pair(o2[1].toDoubleOrNull() ?: 0.0, o2[2].toDoubleOrNull() ?: 0.0),
        snoring = Pair(snore[1], snore[2].toDoubleOrNull() ?: 0.0),
        breathingAnomalies = breathingAnomalies,
        heartRate = Triple(
            heart.getOrElse(1) { "0.0" }.toDoubleOrNull() ?: 0.0,
            heart.getOrElse(2) { "0.0" }.toDoubleOrNull() ?: 0.0,
            heart.getOrElse(3) { "0.0" }.toDoubleOrNull() ?: 0.0
        ),
        cardiacEvents = cardiacEvents,
        movementEvents = movementEvents,
        diseasePrediction = diseasePrediction
    )
}

fun readTextFromFile(file: File): String {
    return try {
        file.readText(Charsets.UTF_8)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

fun processAndSaveParsedReport(context: Context, rawFileName: String): Boolean {
    val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    val rawFile = File(documentsDir, rawFileName)
    if (!rawFile.exists()) {
        return false
    }
    val rawText = readTextFromFile(rawFile)
    val psgReport = parsePSGText(rawText)
    val reportText = buildPSGSummary(psgReport)
    PROMPT = reportText
    // val reportText = psgReport.toString()
    return saveTextToDocuments(context, "parsed_psg.txt", reportText)
}