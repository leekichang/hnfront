package com.holix.android.bottomsheetdialog.compose.org.saltedfish.chatpsg
import org.saltedfish.chatbot.R

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.chrisbanes.photoview.PhotoView
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.ExperimentalPagerApi

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PdfPreviewScreen(pdfUri: Uri) {
    val context = LocalContext.current
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    // PDF 파일을 Bitmap 리스트로 변환 (PdfToBitmapConverter는 별도 구현)
    LaunchedEffect(pdfUri) {
        val converter = PdfToBitmapConverter(context)
        bitmaps = converter.convertPdfToBitmaps(pdfUri)
    }

    if (bitmaps.isNotEmpty()) {
        // HorizontalPager로 한 페이지씩 보여줍니다.
        HorizontalPager(
            count = bitmaps.size,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AndroidView(
                factory = { ctx ->
                    PhotoView(ctx).apply {
                        setImageBitmap(bitmaps[page])
                        // 기본적으로 PhotoView는 줌/패닝 기능을 지원합니다.
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
