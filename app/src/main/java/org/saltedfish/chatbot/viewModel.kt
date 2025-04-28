package org.saltedfish.chatpsg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
data class Photo(
    var id :Int=0,
    val uri: Uri,
    val request: ImageRequest?
)
var PROMPT = """<|im_start|>system
You are an expert in composing function.<|im_end|>
<|im_start|>user

Here is a list of functions:

%DOC%

Now my query is: %QUERY%
<|im_end|>
<|im_start|>assistant
"""
val MODEL_NAMES = arrayOf("Qwen 2.5","","Bert","PhoneLM", "Qwen 1.5")
class ChatViewModel : ViewModel() {
//    private var _inputText: MutableLiveData<String> = MutableLiveData<String>()
//    val inputText: LiveData<String> = _inputText
    private var _messageList: MutableLiveData<List<Message>> = MutableLiveData<List<Message>>(
    listOf()
)
    private var _photoList: MutableLiveData<List<Photo>> = MutableLiveData<List<Photo>>(
        listOf()
    )
    val photoList = _photoList
    private var _previewUri: MutableLiveData<Uri?> = MutableLiveData<Uri?>(null)
    val previewUri = _previewUri
    var _scrollstate:ScrollState? = null
    private var _lastId = 0;
    val messageList= _messageList
    var _isExternalStorageManager = MutableLiveData<Boolean>(false)
    var _isBusy = MutableLiveData<Boolean>(true)
    val isBusy = _isBusy
    val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading = _isLoading
    private var _modelType = MutableLiveData<Int>(0)
    private var _modelId = MutableLiveData<Int>(0)
    val modelId = _modelId
    val modelType = _modelType
    private var profiling_time = MutableLiveData<DoubleArray>()
    val profilingTime = profiling_time

    private var _backendType = -1
    fun setModelType(type:Int){
        _modelType.value = type
    }
    fun setBackendType(type:Int){
        _backendType=type
    }
    fun setModelId(id:Int){
        _modelId.value = id
    }
    fun setPreviewUri(uri: Uri?){
        _previewUri.value = uri
    }
    init {

    JNIBridge.setCallback { id,value, isStream,profile ->
            Log.i("chatViewModel","id:$id,value:$value,isStream:$isStream profile:${profile.joinToString(",")}")
            updateMessage(id,value.trim().replace("|NEWLINE|","\n").replace("▁"," "),isStream)
            if (!isStream){
                _isBusy.postValue(false)
               if(profile.isNotEmpty()) profiling_time.postValue(profile)
            }
        }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        _isExternalStorageManager.value = Environment.isExternalStorageManager()
        } else {
            TODO("VERSION.SDK_INT < R")
        }


    }
    fun addMessage(message: Message,remote:Boolean=false) {
        if (message.isUser){
                message.id = _lastId++
            }
        val list = (_messageList.value?: listOf()).plus(message)

        if (remote){
            _messageList.postValue(list)
        }
        else{
            _messageList.value = list

        }
    }

    fun sendMessage(context:Context,message: Message){
        if (message.isUser){
            addMessage(message)
            val bot_message = Message("...",false,0)
            bot_message.id = _lastId++
            addMessage(bot_message)
            _isBusy.value = true
            if (arrayOf(0,2,3).contains(modelType.value)){
//            if (modelType.value == 0){
                CoroutineScope(Dispatchers.IO).launch {
//                val run_text = "A dialog, where User interacts with AI. AI is helpful, kind, obedient, honest, and knows its own limits.\nUser: ${message.text}"
                   val profiling_time = JNIBridge.run(bot_message.id,message.text,100)
                    Log.i("chatViewModel","profiling_time:$profiling_time")
                }
            }else if (modelType.value ==1){
                val image_content = if (message.type==MessageType.IMAGE){
                   val uri =  message.content as Uri?
                    val inputStream = uri?.let { context.contentResolver.openInputStream(it) }
                    inputStream?.readBytes()?:byteArrayOf()
                } else {
                    byteArrayOf()
                }

                viewModelScope.launch(Dispatchers.IO)  {
                    JNIBridge.runImage(bot_message.id, image = image_content,message.text,100)
                }

        }}}

    fun initStatus(context: Context,modelType:Int=_modelType.value?:0){
        if (_isExternalStorageManager.value != true) return;
        Log.e("chatViewModel", "initStatus$modelType")
        val model_info = "$modelId:$modelType"
        val model_id = modelId.value
        //modelId: 0->PhoneLM,1->Qwen
        val modelPath = when(modelType){
            3->{
                when(model_id){
                    0->"model/phonelm-1.5b-instruct-q4_0_4_4.mllm"
                    1->"model/qwen-2.5-1.5b-instruct-q4_0_4_4.mllm"
                    2->"model/qwen-1.5-1.8b-chat-q4_0_4_4.mllm"

                    else->"model/phonelm-1.5b-instruct-q4_0_4_4.mllm"
                }
            }
            1->"model/fuyu-8b-q4_k.mllm"
            4->{
                when(model_id){
                    0->"model/phonelm-1.5b-call-q8_0.mllm"
                    1->"model/qwen-2.5-1.5b-call-q4_0_4_4.mllm"
                    2->"model/qwen-1.5-1.8b-call-q4_0_4_4.mllm"
                    else->"qwen-2.5-1.5b-call-q4_0_4_4.mllm"
//                    1->"model/qwen-2.5-1.5b-call-fp32.mllm"
//                    else->"qwen-2.5-1.5b-call-fp32.mllm"
                }
            }
            else -> "model/phonelm-1.5b-instruct-q4_0_4_4.mllm"
        }
        val qnnmodelPath = when(modelType){
            3->{
                when(model_id){
                    0->"model/phonelm-1.5b-instruct-int8.mllm"
                    1->"model/qwen-2.5-1.5b-chat-int8.mllm"
                    2->"model/qwen-1.5-1.8b-chat-int8.mllm"
                    else->"model/phonelm-1.5b-instruct-int8.mllm"
                }
            }
            1->""
            4->{
                when(model_id){
                    0->"model/phonelm-1.5b-call-int8.mllm"
                    1->"model/qwen-2.5-1.5b-call-int8.mllm"
                    2->"model/qwen-1.5-1.8b-call-int8.mllm"
                    else->"qwen-2.5-1.5b-call-int8.mllm"
                }
            }
            else -> "model/phonelm-1.5b-instruct-int8.mllm"
        }

        val vacabPath = when(modelType){
            1->"model/fuyu_vocab.mllm"
            3->{
                when(model_id){
                    0->"model/phonelm_vocab.mllm"
                    1->"model/qwen2.5_vocab.mllm"
                    2->"model/qwen_vocab.mllm"
                    else->""
                }
            }
            4->{
                when(model_id){
                    0->"model/phonelm_vocab.mllm"
                    1->"model/qwen2.5_vocab.mllm"
                    2->"model/qwen_vocab.mllm"
                    else->""
                }
            }
            else -> ""
        }
        val mergePath = when (model_id){
            1->"model/qwen2.5_merges.txt"
            0->"model/phonelm_merges.txt"
            2->"model/qwen_merges.txt"
            else->""
        }
        var downloadsPath = "/sdcard/Download/"
        if (!downloadsPath.endsWith("/")){
            downloadsPath = downloadsPath.plus("/")
        }
        //list files of downloadsPath
        val files = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.listFiles()

        Log.i("chatViewModel", "files:${files?.size}")
        val load_model = when (modelType) {
            1 -> 1
            3, 4 -> {
//                if (model_id == 0) {
//                    3
//                } else {
//                    0
//                }
                when(model_id){
                    0->3
                    1->0
                    2->4
                    else->0
                }


            }else -> 0
        }
        viewModelScope.launch(Dispatchers.IO)  {
            Log.i("chatViewModel", "load_model:$load_model on $_backendType")
            val result = JNIBridge.Init( load_model, downloadsPath,modelPath, qnnmodelPath, vacabPath,mergePath,_backendType)
            if (result){
                // addMessage(Message("Model ${MODEL_NAMES[load_model]} Loaded!",false,0),true)
                _isLoading.postValue(false)
                _isBusy.postValue(false)
            }else{
                addMessage(Message("Fail To Load Models! Please Check if models exists at /sdcard/Download/model and restart app.",false,0),true)
            }
        }


    }
    fun updateMessage(id:Int,content:String,isStreaming:Boolean=true){
        val index = _messageList.value?.indexOfFirst { it.id == id }?:-1
        if (index == -1) {
            Log.i("chatViewModel","updateMessage: index == -1")
            return
        }
        val message = _messageList.value?.get(index)?.copy()

        if (message!=null){
            message.text = content
            message.isStreaming= isStreaming
            val list = (_messageList.value?: mutableListOf()).toMutableList()
            // change the item of immutable list
            list[index] = message
            _messageList.postValue(list.toList())
        }
    }
}
