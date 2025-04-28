package com.holix.android.bottomsheetdialog.compose.org.saltedfish.chatpsg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.IOException

class PdfToBitmapConverter(private val context: Context) {

    fun convertPdfToBitmaps(pdfUri: Uri): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null

        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
            if (fileDescriptor != null) {
                pdfRenderer = PdfRenderer(fileDescriptor)
                for (i in 0 until pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(i)
                    // 페이지 크기에 맞춰 Bitmap 생성 (필요에 따라 크기를 조정 가능)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                    page.close()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
        return bitmaps
    }
}
