package org.saltedfish.chatpsg

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.commandiron.compose_loading.Wave
import org.saltedfish.chatbot.ui.theme.ChatBotTheme
import androidx.compose.ui.text.style.TextAlign
import com.holix.android.bottomsheetdialog.compose.org.saltedfish.chatpsg.PdfToBitmapConverter
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import android.widget.Toast
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch



fun saveTextToDocuments(context: Context, fileName: String, content: String): Boolean {
    return try {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()  // Documents 디렉토리가 없으면 생성
        }

        val file = File(documentsDir, fileName)
        val writer = FileWriter(file)
        writer.write(content)
        writer.flush()
        writer.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun extractTextFromPdfUri(context: Context, pdfUri: Uri): String {
    var document: PDDocument? = null
    var inputStream: InputStream? = null
    return try {
        // ContentResolver를 통해 URI로부터 InputStream을 연다.
        inputStream = context.contentResolver.openInputStream(pdfUri)
        // InputStream이 null이 아니라면 PDF 문서를 로드
        if (inputStream != null) {
            document = PDDocument.load(inputStream)
            // PDFTextStripper를 사용해 텍스트를 추출
            val pdfStripper = PDFTextStripper()
            pdfStripper.getText(document)
        } else {
            ""
        }
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    } finally {
        // 자원 해제: 문서와 InputStream을 닫는다.
        try {
            document?.close()
            inputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // PDFBox 초기화
        PDFBoxResourceLoader.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                JNIBridge.stop()
                this.startActivity(intent)
            }
        }
        setContent {
            ChatBotTheme {
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()
                // PDF 선택을 위한 launcher 선언 (Compose 방식)
                val pdfLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let { pdfUri ->
                        coroutineScope.launch {
                            // PDF에서 텍스트 추출
                            val extractedText = extractTextFromPdfUri(this@MainActivity, pdfUri)
                            // 파일명은 타임스탬프를 포함하여 생성
                            val fileName = "extracted_text.txt"
                            val saved = saveTextToDocuments(this@MainActivity, fileName, extractedText)
                            if (saved) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Text file saved as $fileName",
                                    Toast.LENGTH_LONG
                                ).show()
                                // 파일 저장 후 파싱 작업을 진행하고 결과에 따라 바로 채팅 화면으로 이동
                                val parseSaved = processAndSaveParsedReport(this@MainActivity, fileName)
//                                val fn = "PROMPT.txt"
//                                saveTextToDocuments(this@MainActivity, fn, PROMPT)
                                if (parseSaved) {
                                    navController.navigate("chat/0?type=3&device=0")
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Failed to parse and save PSG report",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Failed to save text file",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        }
                    }
                }
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        Home(
                            navController = navController,
                            onPdfSelect = { pdfLauncher.launch(arrayOf("application/pdf")) }
                        )
                    }
                    composable(
                        "chat/{id}?type={type}&device={device}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.IntType },
                            navArgument("type") { type = NavType.IntType; defaultValue = 0 },
                            navArgument("device") { type = NavType.IntType; defaultValue = 0 }
                        )
                    ) {
                        Chat(
                            navController,
                            it.arguments?.getInt("type") ?: 3,
                            it.arguments?.getInt("id") ?: 0,
                            it.arguments?.getInt("device") ?: 0
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(navController: androidx.navigation.NavController, onPdfSelect: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    var selectedBackend by remember { mutableStateOf(0) }
    val modelNames = listOf("PhoneLM", "Qwen 2.5", "Qwen 1.5")
    val deviceNames = listOf("CPU", "NPU")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        contentWindowInsets = WindowInsets(16, 20, 16, 10),
        topBar = { Greeting("Android") },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(bottom = 50.dp),
                icon = { Icon(Icons.Rounded.Settings, "Star Us!") },
                text = { Text(text = "Model Settings") },
                onClick = { showBottomSheet = true },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // MainEntryCards 호출 시 onPdfSelect 람다 전달
            MainEntryCards(
                navController = navController,
                selectedIndex = selectedIndex,
                selectedBackend = selectedBackend,
                onStartClick = onPdfSelect
            )
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Choose a Instructed LLM for PSG Chat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        modelNames.forEachIndexed { index, s ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modelNames.size
                                ),
                                selected = selectedIndex == index,
                                onClick = { selectedIndex = index }
                            ) {
                                Text(text = s)
                            }
                        }
                    }
                    Text(
                        "Choose a Backend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        deviceNames.forEachIndexed { index, s ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modelNames.size
                                ),
                                selected = selectedBackend == index,
                                onClick = { selectedBackend = index }
                            ) {
                                Text(text = s)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPreviewScreen(pdfUri: Uri) {
    val context = LocalContext.current
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(pdfUri) {
        // PdfToBitmapConverter는 PDF를 Bitmap 리스트로 변환하는 유틸리티 클래스입니다.
        val converter = PdfToBitmapConverter(context)
        bitmaps = converter.convertPdfToBitmaps(pdfUri)
    }
    if (bitmaps.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = bitmaps) { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PDF page",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Chat(
    navController: NavController,
    chatType: Int = 0,
    modelId: Int = 0,
    deviceId: Int = 0,
    vm: ChatViewModel = viewModel()
) {
    LaunchedEffect(key1 = chatType, key2 = modelId) {
        vm.setModelType(chatType)
        vm.setModelId(modelId)
        vm.setBackendType(deviceId)
    }

    val previewUri by vm.previewUri.observeAsState()
    val messages by vm.messageList.observeAsState(mutableListOf())
    val context = LocalContext.current
    val isBusy by vm.isBusy.observeAsState(false)
    val isLoading by vm.isLoading.observeAsState(true)
    val scrollState = rememberScrollState()
    val modelType by vm.modelType.observeAsState(0)
    val isExternalStorageManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }
    if (!isExternalStorageManager) {
        vm._isExternalStorageManager.value = false
        // request permission with ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION

    } else {
        vm._isExternalStorageManager.value = true
        LaunchedEffect(key1 = true) {
            vm.initStatus(context, chatType)
            vm._scrollstate = scrollState
        }
    }
    LaunchedEffect(key1 = isBusy) {
        scrollState.animateScrollTo(scrollState.maxValue)

    }
    Scaffold(modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(
                    text = "Chat", fontWeight = FontWeight.Bold, fontSize = 24.sp
                )
            }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
            })
        },
        bottomBar = {
            BottomAppBar {
                ChatInput(!isBusy && !isLoading, withImage = modelType == 1, onImageSelected = {
                    vm.setPreviewUri(it)
                }
                ) {
                    //TODO
                    //Get timestamp
//                    vm.sendInstruct(context, it)
                    vm.sendMessage(context, it)
                }
            }
        }) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(it)
                .consumeWindowInsets(it)
                .systemBarsPadding()
                .verticalScroll(scrollState)

        ) {
            //write a banner widget
            if (vm.profilingTime.value != null && vm.profilingTime.value?.size!! > 0) Row(
                modifier = Modifier
                    .fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer)
                   , horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Prefill: ${String.format("%.2f",vm.profilingTime.value!![1])}Tok/s, Decode: ${String.format("%.2f",vm.profilingTime.value!![2])}Tok/s",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
            ) {
                if (!isLoading) ChatBubble(
                    message = Message(
                        "Hi! I am a PSG ChatBot. How can I help you today?",
                        false,
                        0
                    )
                )
                messages.forEach {
                    ChatBubble(message = it)
                }
            }


        }
        if (previewUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .consumeWindowInsets(it)
                    .systemBarsPadding()
                    .background(Color.Transparent)
            ) {
                PreviewBubble(previewUri!!)

            }
        }
            if (isLoading) Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .consumeWindowInsets(it)
                    .systemBarsPadding()
                    .background(Color.Transparent)
            ) {
               Column (
                   Modifier.align(Alignment.Center),
                   horizontalAlignment = Alignment.CenterHorizontally,
                   verticalArrangement = Arrangement.Center
                   ){
                   Wave(
                       size = 100.dp,
                       color = MaterialTheme.colorScheme.onPrimaryContainer,
                   )
                   Spacer(modifier = Modifier.height(80.dp))
                   Text(text = "Loading Model...",color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                   )

               }
        }
    }

}

fun getBubbleShape(
    density: Density,
    cornerRadius: Dp,
    arrowWidth: Dp,
    arrowHeight: Dp,
    arrowOffset: Dp
): GenericShape {

    val cornerRadiusPx: Float
    val arrowWidthPx: Float
    val arrowHeightPx: Float
    val arrowOffsetPx: Float

    with(density) {
        cornerRadiusPx = cornerRadius.toPx()
        arrowWidthPx = arrowWidth.toPx()
        arrowHeightPx = arrowHeight.toPx()
        arrowOffsetPx = arrowOffset.toPx()
    }

    return GenericShape { size: Size, layoutDirection: LayoutDirection ->

        this.addRoundRect(
            RoundRect(
                rect = Rect(
                    offset = Offset(0f, 0f),
                    size = Size(size.width, size.height - arrowHeightPx)
                ),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
        )
        moveTo(arrowOffsetPx, size.height - arrowHeightPx)
        lineTo(arrowOffsetPx + arrowWidthPx / 2, size.height)
        lineTo(arrowOffsetPx + arrowWidthPx, size.height - arrowHeightPx)

    }
}

@Composable
fun BoxScope.PreviewBubble(preview: Uri) {
    val density = LocalDensity.current
    val arrowHeight = 8.dp

    val bubbleShape = remember {
        getBubbleShape(
            density = density,
            cornerRadius = 10.dp,
            arrowWidth = 20.dp,
            arrowHeight = arrowHeight,
            arrowOffset = 30.dp
        )
    }
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .shadow(10.dp, bubbleShape)
            .padding(start = 5.dp)
            .fillMaxWidth(0.2f)
            .background(MaterialTheme.colorScheme.primaryContainer)

    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current).data(data = preview)
                    .size(coil.size.Size.ORIGINAL).build()
            ),
            contentDescription = "Image Description",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = arrowHeight)
                .clip(RoundedCornerShape(10.dp))
        )

    }
}



@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatInput(
    enable: Boolean,
    withImage: Boolean,
    onImageSelected: (Uri?) -> Unit = {},

    onMessageSend: (Message) -> Unit = {}
) {
    var text by remember { mutableStateOf("") }
    var imageUri = remember { mutableStateOf<Uri?>(null) }

//softkeyborad
    val keyboardController = LocalSoftwareKeyboardController.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        it?.let {
            imageUri.value = it
            onImageSelected(it)
        }

    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedTextField(
//            enabled = enable,
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .padding(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(0.5f),
                focusedContainerColor = Color.White.copy(0.5f),

                )

        )
        IconButton(onClick = {
            keyboardController?.hide()
            val punctuation = listOf('.', '?', '!', ',', ';', ':', '。', '？', '！', '，', '；', '：')
            if (text.isNotEmpty() && !punctuation.contains(text.last()) && text.last() != '\n') text += "."
            onMessageSend(
                Message(
                    text,
                    true,
                    0,
                    type = if (imageUri.value == null) MessageType.TEXT else MessageType.IMAGE,
                    content = imageUri.value
                )
            );text = "";imageUri.value = null;onImageSelected(null)
        }, enabled = enable) {
            Icon(
                painter = painterResource(id = R.drawable.up),
                contentDescription = "Send",
                Modifier.size(36.dp)
            )
        }

    }

}

@Composable
fun ColumnScope.ChatBubble(
    message: Message,
) {
    if (message.text.isNotEmpty()) ChatBubbleBox(isUser = message.isUser) {
        SelectionContainer {
            Text(text = message.text, fontSize = 18.sp)
        }
    }
}

@Composable
fun ColumnScope.ChatBubbleBox(isUser: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .padding(5.dp)
            .align(if (isUser) Alignment.End else Alignment.Start)
            .clip(
                RoundedCornerShape(
                    topStart = 48f,
                    topEnd = 48f,
                    bottomStart = if (isUser) 48f else 0f,
                    bottomEnd = if (isUser) 0f else 48f
                )
            )
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
            .widthIn(max = 300.dp)
    ) {
        content()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 56.dp, start = 20.dp)) {
        Text(
            text = "PSG Chat",
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 30.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        // Subtitle
        Text(
            text = "HoneyNaps Project",
            style = MaterialTheme.typography.titleLarge,
            lineHeight = 24.sp

        )
    }

}

@Composable
fun MainEntryCards(
    navController: NavController,
    selectedIndex: Int = 0,
    selectedBackend: Int = 0,
    onStartClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .padding(top = 200.dp)
    ) {
        Row {
            EntryCard(
                icon = R.drawable.text,
                backgroundColor = Color(0xEDADE6AA),
                title = "Start",
                subtitle = "\" Ask about your PSG Report! \"",
                onClick = onStartClick  // 여기서 전달받은 람다 실행
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.EntryCard(
    icon: Int,
    backgroundColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .weight(0.5f)
            .aspectRatio(2.2f),
        shape = RoundedCornerShape(20),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            RoundIcon(id = icon, backgoundColor = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(36.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.next),
                    contentDescription = "Icon Description",
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(36.dp),
                )
            }

        }
    }
}

@Composable
fun RoundIcon(id: Int, backgoundColor: Color) {
    Box(
        modifier = Modifier
            .padding(end = 16.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(backgoundColor)
    ) {
        Image(
            painter = painterResource(id),
            contentDescription = "Icon Description",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChatBotTheme {
        Greeting("Android")
    }
}