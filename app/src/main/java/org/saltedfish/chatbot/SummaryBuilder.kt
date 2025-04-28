// package com.holix.android.bottomsheetdialog.compose.org.saltedfish.chatbot
package org.saltedfish.chatpsg

data class PatientInfo(val name: String, val age: Int, val gender: String, val date: String)
data class Vitals(val height: Int, val weight: Int, val bmi: Double, val nc: Double, val wc: Double, val hipc: Double, val whr: Double)
data class DurationIndex(val count: Int, val index: Double)

data class PSGReport(
    val patientInfo: PatientInfo,
    val vitals: Vitals,
    val sleepTiming: Map<String, Pair<String, Double?>>, // Label -> (Time, Percent?)
    val sleepEfficiency: Double,
    val sleepStages: Map<String, Pair<String, Double>>, // Stage -> (Time, Percent)
    val arousalEvents: Map<String, DurationIndex>,
    val respiratoryEvents: Map<String, Map<String, DurationIndex>>, // Section -> Label -> Count/Index
    val meanRespDur: Map<String, Int>,
    val maxRespDur: Map<String, Int?>,
    val oxygen: Pair<Double, Double>,
    val snoring: Pair<String, Double>,
    val breathingAnomalies: Map<String, String>,
    val heartRate: Triple<Double, Double, Double>, // Avg, High, Low
    val cardiacEvents: Map<String, String>,
    val movementEvents: Map<String, DurationIndex>,
    val diseasePrediction: Map<String, Map<String, String>>
)

fun buildPSGSummary(report: PSGReport): String {
    return buildString {
        appendLine("PSG Summary Report")
        appendLine("===========================")
        appendLine("Patient: ${report.patientInfo.name} (${report.patientInfo.gender}, ${report.patientInfo.age} yrs)")
        appendLine("Date: ${report.patientInfo.date}")
        appendLine()

        appendLine("Vitals:")
        appendLine("  - Height: ${report.vitals.height} cm")
        appendLine("  - Weight: ${report.vitals.weight} kg")
        appendLine("  - BMI: ${report.vitals.bmi}")
        appendLine("  - NC: ${report.vitals.nc} cm, WC: ${report.vitals.wc} cm, HipC: ${report.vitals.hipc} cm, WHR: ${report.vitals.whr}")
        appendLine()

        appendLine("Sleep Efficiency: ${report.sleepEfficiency}%")
        appendLine("Sleep Timing:")
        report.sleepTiming.forEach { (label, pair) ->
            appendLine("  - $label: ${pair.first}${pair.second?.let { " (${it}%)" } ?: ""}")
        }
        appendLine()

        appendLine("Sleep Stages:")
        report.sleepStages.forEach { (stage, pair) ->
            appendLine("  - $stage: ${pair.first} (${pair.second}%)")
        }
        appendLine()

        appendLine("Arousal Events:")
        report.arousalEvents.forEach { (type, value) ->
            appendLine("  - $type: ${value.count} / ${value.index}")
        }
        appendLine()

        appendLine("Respiratory Events:")
        report.respiratoryEvents.forEach { (section, map) ->
            appendLine("  [$section]")
            map.forEach { (type, value) ->
                appendLine("    - $type: ${value.count} / ${value.index}")
            }
        }
        appendLine()

        appendLine("Mean Resp Duration:")
        report.meanRespDur.forEach { (type, sec) ->
            appendLine("  - $type: ${sec} sec")
        }

        appendLine("Max Resp Duration:")
        report.maxRespDur.forEach { (type, sec) ->
            appendLine("  - $type: ${sec ?: "-"} sec")
        }
        appendLine()

        appendLine("Oxygen:")
        appendLine("  - Mean: ${report.oxygen.first}%")
        appendLine("  - Lowest: ${report.oxygen.second}%")
        appendLine()

        appendLine("Snoring:")
        appendLine("  - Duration: ${report.snoring.first}")
        appendLine("  - Percentage: ${report.snoring.second}%")
        appendLine()

        appendLine("Breathing Anomalies:")
        report.breathingAnomalies.forEach { (label, value) ->
            appendLine("  - $label: $value")
        }
        appendLine()

        appendLine("Cardiac Events:")
        report.cardiacEvents.forEach { (label, value) ->
            appendLine("  - $label: $value")
        }
        appendLine()

        appendLine("Movement Events:")
        report.movementEvents.forEach { (label, value) ->
            appendLine("  - $label: ${value.count} / ${value.index}")
        }
        appendLine()

        appendLine("Heart Rate:")
        appendLine("  - Average: ${report.heartRate.first}")
        appendLine("  - Maximum: ${report.heartRate.second}")
        appendLine("  - Minimum: ${report.heartRate.third}")
        appendLine()

        appendLine("Disease Prediction:")
        report.diseasePrediction.forEach { (period, values) ->
            appendLine("  [$period]")
            values.forEach { (label, value) ->
                appendLine("    - $label: $value")
            }
        }
    }
}
