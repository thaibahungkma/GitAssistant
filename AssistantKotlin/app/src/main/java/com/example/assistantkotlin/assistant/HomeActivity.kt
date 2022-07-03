package com.example.assistantkotlin.assistant

import android.Manifest
import android.annotation.SuppressLint
import android.app.SearchManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings.Global.putString
import android.provider.Telephony
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.assistantkotlin.AES.AES
import com.example.assistantkotlin.MainActivity
import com.example.assistantkotlin.R
import com.example.assistantkotlin.data.AssistantDatabase
import com.example.assistantkotlin.databinding.ActivityHomeBinding
import com.example.assistantkotlin.extend.NoteActivity
import com.example.assistantkotlin.extend.RemindActivity
import com.example.assistantkotlin.extend.SuggestActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_home.*
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var assistantViewModel: AssistantViewModel

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var keeper: String

    private lateinit var personName:String
    var aes: AES = AES()

    private var REQUESTCALL = 1
    private var SENDSMS = 2
    private var READSMS = 3
    private var SHAREAFILE = 4
    private var SHAREATEXTFILE = 5
    private var READCONTACTS = 6
    private var CAPTUREPHOTO = 7

    private var REQUEST_CODE_SELECT_DOC: Int = 100
    private var REQUEST_ENABLE_BT = 1000
    val RecordAudioRequestCode: Int = 1

    private var bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var cameraManager: CameraManager
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var cameraID: String
    private lateinit var ringtone: Ringtone

    private val logtts = "TTS"
    private val logsr = "SR"
    private val logkeeper = "keeper"

    private var imageIndex: Int = 0
    private lateinit var imgUri: Uri
    private var savedRecognitionEnglish:Boolean = true
    private var isNote:Boolean=false
    private lateinit var numberSMS: String
    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private var user: FirebaseUser? = null

    @Suppress("DEPRECATION")
    private val imageDirection =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString() + "/assistant/"


    @SuppressLint("ClickableViewAccessibility")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        supportActionBar?.hide()
        val application = requireNotNull(this).application
        val dataSource = AssistantDatabase.getInstance(application).assistantDao
        val viewModelFactory = AssistantViewModelFactory(dataSource, application)
        assistantViewModel =
            ViewModelProvider(
                this, viewModelFactory
            ).get(AssistantViewModel::class.java)
        val adapter = AssistantAdapter()
        recyclerview.adapter = adapter

        assistantViewModel.messages.observe(this,{ it?.let { adapter.data = it } })
        binding.setLifecycleOwner(this)

        //animations
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)
        val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down)
        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)

        //Mov to Setting Activity
        avatarIv.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }
        //Tools on Click
        openNoteIv.setOnClickListener {
            startActivity(Intent(this, NoteActivity::class.java))
            openNoteIv.startAnimation(zoomIn)
        }
        OpenAnniversaryIV.setOnClickListener {
            startActivity(Intent(this, RemindActivity::class.java))
            OpenAnniversaryIV.startAnimation(zoomIn)
        }
        openPlayListIv.setOnClickListener {
            openPlayListIv.startAnimation(zoomIn)
            assistantViewModel.onClear()
            Toast.makeText(this, "Đã xóa lịch sử trò chuyện", Toast.LENGTH_SHORT).show()
        }
        openSuggestIv.setOnClickListener {
            openSuggestIv.startAnimation(zoomIn)
            startActivity(Intent(this, SuggestActivity::class.java))
        }

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        user = firebaseAuth.currentUser
        checkUser()


        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraID = cameraManager.cameraIdList[0]
            //0 back camera
            //1 font camera
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        ringtone = RingtoneManager.getRingtone(
            applicationContext,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )

        //audio record permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            checkPermission()
        }



        //get key value from share preferences recognition
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val savedRecognition=sharedPreferences?.getString("recognition",null)
        if (savedRecognition=="English") {
            savedRecognitionEnglish=true
        }
        else if (savedRecognition=="Vietnamese"){
            savedRecognitionEnglish=false
        }





        textToSpeech = TextToSpeech(this) { status ->

            if (status == TextToSpeech.SUCCESS) {
                val result: Int
                if (savedRecognition=="English"){
                    result = textToSpeech.setLanguage(Locale.ENGLISH)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(logtts, "Language not supported")
                    } else {
                        Log.e(logtts, "Language supported")
                    }
                }
                else if (savedRecognition=="Vietnamese"){
                    result= textToSpeech.setLanguage(Locale.forLanguageTag("vi-VI"))
                    textToSpeech.setPitch(0.9f)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(logtts, "Language not supported")
                    } else {
                        Log.e(logtts, "Language supported")
                    }
                }
            } else {
                Log.e(logtts, "Initialization failed")
            }
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                Log.d(logtts, "Ready")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SR", "started")
            }

            override fun onRmsChanged(p0: Float) {

            }

            override fun onBufferReceived(p0: ByteArray?) {

            }

            override fun onEndOfSpeech() {
                Log.d("SR", "ended")
            }

            override fun onError(p0: Int) {
                Log.d("SR", "Error")
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResults(bundle: Bundle?) {
                val data = bundle!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (data != null) {
                    keeper = data[0]
                    Log.d(logkeeper, keeper)
                    //English recognition
                    if (savedRecognition=="English"){
                        when {
                            keeper.lowercase().contains("recommend")|| keeper.lowercase().contains("tutorial")-> speak("Hi, my name is Sun assistant, i can help you to do some task, please give your command by voice")
                            keeper.lowercase().contains("thank you") -> speak("No problem, it's my job")
                            keeper.lowercase().contains("welcome") -> speak("for what?")
                            keeper.lowercase().contains("help me") -> speak("Alright, can i help you ?")
                            keeper.lowercase().contains("i love you") -> speak("i love you too")
                            keeper.lowercase().contains("your name") -> speak("My name is Sun Assistant")
                            keeper.lowercase().contains("created you") -> speak("Thai ba hung created me while working on my graduation project")
                            keeper.lowercase().contains("clear") -> assistantViewModel.onClear()
                            keeper.lowercase().contains("date") -> getDate()
                            keeper.lowercase().contains("what time") -> getTime()
                            keeper.lowercase().contains("phone to") -> makeNumberCall()
                            keeper.lowercase().contains("send sms") -> sendSMS()
                            keeper.lowercase().contains("read sms")||keeper.lowercase().contains("read message") -> readSMS()
                            keeper.lowercase().contains("open gmail") -> openGmail()
                            keeper.lowercase().contains("open facebook") -> openFaceBook()
                            keeper.lowercase().contains("open message") -> openMessages()
                            keeper.lowercase().contains("open instagram") -> openInstagram()
                            keeper.lowercase().contains("open youtube") -> openYoutube()
                            keeper.lowercase().contains("open zalo") -> openZalo()
                            keeper.lowercase().contains("open browser") ||keeper.lowercase().contains("open chrome")-> openChrome()
                            keeper.lowercase().contains("open messenger")-> openMessenger()
                            keeper.lowercase().contains("open google translate")-> openGoogleTranslate()
                            keeper.lowercase().contains("open netflix")-> openNetflix()
                            keeper.lowercase().contains("open zing mp3")||keeper.lowercase().contains("play music") ->openZingMp3()
                            keeper.lowercase().contains("open google map")||keeper.lowercase().contains("open map")-> openGoogleMap()
                            keeper.lowercase().contains("share a file") -> shareAFIle()
                            keeper.lowercase().contains("share a text message") -> shareATextMessage()
                            keeper.lowercase().contains("call contact") -> callContact()
                            keeper.lowercase().contains("turn on bluetooth") -> turnOnBluetooth()
                            keeper.lowercase().contains("turn off bluetooth") -> turnOffBluetooth()
                            keeper.lowercase().contains("bluetooth device") -> getAllPaireDevice()
                            keeper.lowercase().contains("turn on flash") -> turnOnFlash()
                            keeper.lowercase().contains("turn off flash") -> turnOffFlash()
                            keeper.lowercase().contains("remind") ||keeper.lowercase().contains("notification")-> makeRemind()
                            keeper.lowercase().contains("copy to clipboard") -> clipBoardCopy()
                            keeper.lowercase().contains("read last clipboard") -> clipBoardSpeak()
                            keeper.lowercase().contains("capture a photo") || keeper.contains("Take a photo") -> capturePhoto()
                            keeper.lowercase().contains("play ringtone") -> playRingtone()
                            keeper.lowercase().contains("stop ringtone") || keeper.contains("Top ringtone") -> stopRingtone()
                            keeper.lowercase().contains("search google") -> googleSearch()
                            keeper.lowercase().contains("search youtube") -> youtubeSearch()
                            keeper.lowercase().contains("weather") -> GetCurrentWeatherDataEnglish()
                            keeper.lowercase().contains("hello") || keeper.contains("hi") || keeper.contains("hey")
                            -> speak("Hello, how I can help you?")
                            else -> if (isNote==false){
                                googleSearchWithout()
                            }
                                }
                    }
                    else {
                        var ketqua=convert(keeper)!!.lowercase()
                        when {
                            ketqua.contains("gioi thieu") -> speakNow("gioi thieu")
                            ketqua.contains("cam on") -> speakNow("cam on")
                            ketqua.contains("hat bai")||ketqua.contains("hat mot bai")||ketqua.contains("sing a song")
                            -> speakNow("hat")
                            ketqua.contains("alo") -> speak("Mình nghe")
                            ketqua.contains("toi la ai")||ketqua.contains("ten toi la gi")
                                    ||ketqua.contains("do ban biet ten cua toi")-> speak("Nếu mình không nhầm, thì tên bạn là $personName")
                            ketqua.contains("choi game") -> speakNow("choi game")
                            ketqua.contains("ban co doi bung") -> speakNow("doi bung")
                            ketqua.contains("anh yeu em") ||ketqua.contains("toi yeu ban") || ketqua.contains("em yeu anh")-> speakNow("yeu")
                            ketqua.contains("ten ban") -> speak("Tên mình là trợ lý Sun")
                            ketqua.contains("tao ra ban") ||ketqua.contains("tao ban")||ketqua.contains("lap trinh ban")-> speak("Anh Hùng đẹp trai lập trình nên tôi lúc làm đồ án đấy")
                            ketqua.contains("xoa chat") -> assistantViewModel.onClear()
                            ketqua.contains("hom nay ngay bao nhieu") ||ketqua.contains("hom nay la ngay bao nhieu")||ketqua.contains("hom nay ngay may")||ketqua.contains("ngay thang")-> getDate()
                            ketqua.contains("may gio")||ketqua.contains("xem gio") -> getTime()
                            ketqua.contains("goi so")||ketqua.contains("goi dien so") -> makeNumberCall()
                            ketqua.contains("gui tin nhan cho")->convertKeeper(1)
                            ketqua.contains("nhan tin cho") -> sendSMS2()
                            ketqua.contains("doc tin nhan vua nhan")||ketqua.contains("tin nhan gan nhat")||ketqua.contains("doc tin nhan nhan")||ketqua.contains("doc hop thu den gan nhat") -> readSMS()
                            ketqua.contains("mo gmail") -> openGmail()
                            ketqua.contains("mo facebook") -> openFaceBook()
                            ketqua.contains("mo message") -> openMessages()
                            ketqua.contains("mo instagram") -> openInstagram()
                            ketqua.contains("mo youtube") -> openYoutube()
                            ketqua.contains("mo zalo") -> openZalo()
                            ketqua.contains("mo trinh duyet") ||ketqua.contains("mo chrome")-> openChrome()
                            ketqua.contains("mo messenger")-> openMessenger()
                            ketqua.contains("mo google dich")-> openGoogleTranslate()
                            ketqua.contains("mo netflix")||ketqua.contains("xem netflix")-> openNetflix()
                            ketqua.contains("mo zing mp3")->openZingMp3()
                            ketqua.contains("nghe nhac")-> openZingMp3ne()
                            ketqua.contains("mo google map")||ketqua.contains("mo ban do")-> openGoogleMap()
                            ketqua.contains("share a file") -> shareAFIle()
                            ketqua.contains("share a text message") -> shareATextMessage()
                            ketqua.contains("goi cho") -> callContact()
                            ketqua.contains("bat bluetooth") -> turnOnBluetooth()
                            ketqua.contains("tat bluetooth") -> turnOffBluetooth()
                            ketqua.contains("thiet bi bluetooth") -> getAllPaireDevice()
                            ketqua.contains("bat flash") ||ketqua.contains("bat den flash") ||ketqua.contains("bat den pin")-> turnOnFlash()
                            ketqua.contains("tat flash") ||ketqua.contains("tat den flash") ||ketqua.contains("tat den pin")-> turnOffFlash()
                            ketqua.contains("copy vao bo nho") ||ketqua.contains("sao chep")-> clipBoardCopy()
                            ketqua.contains("doc bo nho tam") -> clipBoardSpeak()
                            ketqua.contains("chup anh") || ketqua.contains("may anh") -> capturePhoto()
                            ketqua.contains("play ringtone")||ketqua.contains("bat nhac chuong") -> playRingtone()
                            ketqua.contains("stop ringtone") || ketqua.contains("dung nhac") -> stopRingtone()
                            ketqua.contains("dat bao thuc") -> setAlarm()
                            ketqua.contains("thoi tiet")-> GetCurrentWeatherData()
                            ketqua.contains("tim google") -> googleSearch()
                            ketqua.contains("tim youtube") -> youtubeSearch()
                            ketqua.contains("tao ghi chu")->createNote()
                            ketqua.contains("day la dau")||ketqua.contains("toi dang o dau")||ketqua.contains("vi tri cua toi")->getJsoupWhere().execute()
                            ketqua.contains("la gi")||ketqua.contains("la ai")->getJsoupWhat().execute()
                            ketqua.contains("bang bao nhieu")||ketqua.contains("bang may")->caculator()
                            ketqua.contains("tao nhac nho")||ketqua.contains("tao lich hen")->makeRemind()
                            ketqua.contains("mo ghi chu")||ketqua.contains("mo danh sach ghi chu")->openNote()
                            ketqua.contains("xem goi y")||ketqua.contains("goi y")||ketqua.contains("mo goi y")->openSuggest()
                            ketqua.contains("bat nhac nhe")||ketqua.contains("nhac ru ngu")
                                    ||ketqua.contains("thu gian")||ketqua.contains("nhac chill")->openChillMusic()
                            ketqua.contains("nhac son tung") || ketqua.contains("nhac sep")->openSonTungMusic()
                            ketqua.contains("chao") || ketqua.contains("hey")||ketqua.contains("hello")
                            -> speakNow("hello")
                            else ->
                                if (isNote==false){
                                    googleSearchWithout()
                                }}
                    }


                }
            }


            override fun onPartialResults(p0: Bundle?) {

            }

            override fun onEvent(p0: Int, p1: Bundle?) {
                TODO("Not yet implemented")
            }

        })
        assistantActionBtn.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    assistantActionBtn.startAnimation(scaleUp)
                    speechRecognizer.stopListening()

                }
                MotionEvent.ACTION_DOWN -> {
                    assistantActionBtn.startAnimation(scaleDown)
                    textToSpeech.stop()
                    speechRecognizer.startListening(recognizerIntent)
                }
            }
            false
        }
        checkIfSpeechRecognizerAvailable()
    }

    private fun convertKeeper(i: Int) {
        if(i==1){
            speak("Mời bạn thử lại câu lệnh, ví dụ: Nhắn tin cho Hùng")
        }

    }


    fun convert(str: String): String? {
        var str = str
        str = str.replace("à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ".toRegex(), "a")
        str = str.replace("è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ".toRegex(), "e")
        str = str.replace("ì|í|ị|ỉ|ĩ".toRegex(), "i")
        str = str.replace("ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ".toRegex(), "o")
        str = str.replace("ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ".toRegex(), "u")
        str = str.replace("ỳ|ý|ỵ|ỷ|ỹ".toRegex(), "y")
        str = str.replace("đ".toRegex(), "d")
        str = str.replace("À|Á|Ạ|Ả|Ã|Â|Ầ|Ấ|Ậ|Ẩ|Ẫ|Ă|Ằ|Ắ|Ặ|Ẳ|Ẵ".toRegex(), "A")
        str = str.replace("È|É|Ẹ|Ẻ|Ẽ|Ê|Ề|Ế|Ệ|Ể|Ễ".toRegex(), "E")
        str = str.replace("Ì|Í|Ị|Ỉ|Ĩ".toRegex(), "I")
        str = str.replace("Ò|Ó|Ọ|Ỏ|Õ|Ô|Ồ|Ố|Ộ|Ổ|Ỗ|Ơ|Ờ|Ớ|Ợ|Ở|Ỡ".toRegex(), "O")
        str = str.replace("Ù|Ú|Ụ|Ủ|Ũ|Ư|Ừ|Ứ|Ự|Ử|Ữ".toRegex(), "U")
        str = str.replace("Ỳ|Ý|Ỵ|Ỷ|Ỹ".toRegex(), "Y")
        str = str.replace("Đ".toRegex(), "D")
        return str
    }


    private fun checkIfSpeechRecognizerAvailable() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.d(logsr, "yes")
        } else {
            Log.d(logsr, "false")
        }
    }

    //speak text
    fun speak(text: String) {
        try {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
            assistantViewModel.sendMessageToDatabase(text,keeper)
        }
        catch (speak:Exception){
            Log.d("Errorspeak","skip")
        }

    }
    fun speakNow(text: String) {
        try {
            var textVoice:String
            when (text){
                "hello"-> textVoice="Chào $personName, bạn có cần mình giúp gì không ? "
                "gioi thieu"->textVoice="Mình là trợ lý Sun. Mình có thể giúp bạn thực hiện một số công việc, $personName hãy ra lệnh cho mình bằng giọng nói nhé"
                "cam on"->textVoice="Không có gì, nhiệm vụ của mình là giúp đỡ $personName mà \uD83D\uDE0A"
                "choi game"->textVoice="Mình cũng thích chơi game lắm. Chờ mình thông minh hơn rồi mình sẽ chơi với bạn nhé"
                "doi bung"->textVoice="Mình không ăn cơm nên không đói, nhưng bạn nhớ sạc pin điện thoại để mình luôn khỏe mạnh nhé"
                "yeu"->textVoice="$personName đang tỏ tình với mình đấy à, mình đồng ý \uD83E\uDD70"
                "hat"->textVoice="""Cơ hội cho mình thể hiện đây rồi, mời $personName lắng nghe nhé
                    | Một con vịt xòe ra hai cái cánh
                    | Nó kêu rằng quác quác quác, quạc quạc quạc
                """.trimMargin()
                else -> textVoice="Xin lỗi, hiện tại mình chưa được học câu lệnh này"
            }
            textToSpeech.speak(textVoice, TextToSpeech.QUEUE_FLUSH, null, "")
            assistantViewModel.sendMessageToDatabase(textVoice,keeper)
        }
        catch (speak:Exception){
            Log.d("ErrorSpeakNow","skip")
        }

    }

    //get Date to speech
    fun getDate() {
        val calendar = Calendar.getInstance()
        val formattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.time)
        val splitDate = formattedDate.split(",").toTypedArray()
        val date = splitDate[1].trim() { it <= ' ' }

        if (savedRecognitionEnglish==true){
            speak("The date is $date")
        }
        else{
            speak("Hôm nay là $date")
        }
    }

    //get Time current to Speech
    fun getTime() {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("HH:mm:ss")
        val time: String = format.format(calendar.getTime())
        if (savedRecognitionEnglish==true){
            speak("The time is $time")
        }
        else{
            speak("Bây giờ là $time")
        }

    }

    //Phone call number
    private fun makeNumberCall() {
        try {
            val keeperSplit = convert(keeper)!!.replace(" ".toRegex(), "").split("o").toTypedArray()
            val number = keeperSplit[2]
            if (number.trim() { it <= ' ' }.length > 0) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CALL_PHONE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CALL_PHONE),
                        REQUESTCALL
                    )
                } else {
                    val dial = "tel:$number"
                    if (savedRecognitionEnglish==true){
                        assistantViewModel.sendMessageToDatabase("Calling $number",keeper)
                    }
                    else{
                        assistantViewModel.sendMessageToDatabase("Đang gọi cho $number",keeper)
                    }

                    startActivity(Intent(Intent.ACTION_CALL, Uri.parse(dial)))
                }
            } else {
                speak("Vui lòng đọc số điện thoại")
            }
        }
        catch (call:Exception){

        }

    }
    private fun openNote() {
        startActivity(Intent(this, NoteActivity::class.java))
    }

    private fun openSuggest() {
        speak("Đây là gợi ý những câu lệnh bạn có thể yêu cầu cho mình")
        startActivity(Intent(this, SuggestActivity::class.java))
    }
    private fun makeRemind() {
        speak("OK, mời bạn tạo nhắc nhở")
        startActivity(Intent(this, RemindActivity::class.java))
    }

    private fun sendSMS() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SEND_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SENDSMS)
            } else {
                if (keeper==""){
                    speak("Mình không nhận được nội dung tin nhắn, bạn vui lòng thử lại")
                    Toast.makeText(this, "Không nhận được nội dung tin nhắn", Toast.LENGTH_SHORT).show()
                } else{
                    val mySmsManager = SmsManager.getDefault()
                    mySmsManager.sendTextMessage(
                        numberSMS.trim { it <= ' ' }, null, keeper.trim() { it <= ' ' }, null, null
                    )
                    speak("Đã gửi tin nhắn thành công")
                }

                isNote=false
            }
        }
        catch (sendSMS:Exception){
            speak("Xin lỗi, đã có lỗi xảy ra")
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), READSMS)
        } else {
            val cursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null)
            cursor!!.moveToFirst()
            val name = cursor!!.getColumnIndex("address")
            val body= cursor!!.getColumnIndex("body")
            if (savedRecognitionEnglish==true){
                speak("Message from ${cursor.getString(name)} is ${cursor.getString(body)}")
            }
            else{
                speak("Tin nhắn nhận từ ${cursor.getString(name)} nội dung là ${cursor.getString(body)} ")
            }

        }
    }

    private fun openMessages() {
        val intent =
            packageManager.getLaunchIntentForPackage(Telephony.Sms.getDefaultSmsPackage(this))
        intent?.let { startActivity(it) }
    }

    private fun openFaceBook() {
        if (savedRecognitionEnglish==true){
            speak("Opening Facebook")
        }
        else{
            speak("Đang mở Facebook")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.facebook.katana")
        intent?.let { startActivity(it) }
    }
    private fun openChrome() {
        if (savedRecognitionEnglish==true){
            speak("Opening Chrome")
        }
        else{
            speak("Đang mở trình duyệt")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.android.chrome")
        intent?.let { startActivity(it) }
    }
    private fun openGoogleMap() {
        if (savedRecognitionEnglish==true){
            speak("Opening Google Map")
        }
        else{
            speak("Đang mở bản đồ")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
        intent?.let { startActivity(it) }
    }
    private fun openGoogleTranslate() {
        if (savedRecognitionEnglish==true){
            speak("Opening Google Translate")
        }
        else{
            speak("Đang mở Google Dịch")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.translate")
        intent?.let { startActivity(it) }
    }
    private fun openZingMp3() {
        if (savedRecognitionEnglish==true){
            speak("Opening Zing Mp3")
        }
        else{
            speak("Đang mở Zing mp3")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.zing.mp3")
        intent?.let { startActivity(it) }
    }
    private fun openZingMp3ne() {
        if (savedRecognitionEnglish==true){
            speak("you can listen to music on Zing Mp3")
        }
        else{
            speak("Lên Zing mp3 nghe nhạc cực chất nhé bạn")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.zing.mp3")
        intent?.let { startActivity(it) }
    }
    private fun openNetflix() {
        if (savedRecognitionEnglish==true){
            speak("Opening Netflix")
        }
        else{
            speak("Đang mở Netflix")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.netflix.mediaclient")
        intent?.let { startActivity(it) }
    }
    private fun openMessenger() {
        if (savedRecognitionEnglish==true){
            speak("Opening Messenger")
        }
        else{
            speak("Đang mở Messenger")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.facebook.orca")
        intent?.let { startActivity(it) }
    }



    private fun openGmail() {
        if (savedRecognitionEnglish==true){
            speak("Opening Gmail")
        }
        else{
            speak("Đang mở Gmail")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.google.android.gm")
        intent?.let { startActivity(it) }
    }
    private fun caculator(){
        getJsoup().execute()

    }
    inner class getJsoup: AsyncTask<Void,String,String>(){

        override fun onPreExecute() {
            super.onPreExecute()
        }
        override fun doInBackground(vararg p0: Void?): String {
            var keeperReplace=keeper.replace("nhân".toRegex(),"x").replace("+","%2B").replace("chia".toRegex(),"/")
            var url ="https://www.google.com/search?q=$keeperReplace"
            val document : Document
            val element: Elements
            val ketqua:String
            try {
                 document= Jsoup.connect(url).get()
                 element= document.getElementsByClass("qv3Wpe")
                 ketqua=element.text().toString()
                return ketqua
            }
            catch (jsoup:Exception){
                return null.toString()
                Log.d(TAG, "doInBackground: null")
            }

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            speak("Kết quả là $result")
            Log.d(TAG, "onPostExecute: $result ")
        }

    }

    inner class getJsoupWhat: AsyncTask<Void,String,String>(){

        override fun onPreExecute() {
            super.onPreExecute()
        }
        override fun doInBackground(vararg p0: Void?): String {
            var url ="https://www.google.com/search?q=$keeper"
            val document : Document
            val element: Elements
            val ketqua:String
            try {
                document= Jsoup.connect(url).get()
                element= document.getElementsByClass("kno-rdesc").removeClass("Uo8X3b OhScic zsYMMe").removeClass("ruhjFe NJLBac fl")
                ketqua=element.text().replace("Wikipedia".toRegex(),"")
                return ketqua
            }
            catch (jsoup:Exception){
                return null.toString()
                Log.d(TAG, "doInBackground: null")
            }

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result==""){
                Log.d(TAG, "onPostExecute: $result ")
                googleSearchWithout()
            } else{
                speak("$result")
                Log.d(TAG, "onPostExecute: $result ")
            }


        }

    }
    inner class getJsoupWhere: AsyncTask<Void,String,String>(){

        override fun onPreExecute() {
            super.onPreExecute()
        }
        override fun doInBackground(vararg p0: Void?): String {
            var url ="https://www.google.com/search?q=$keeper"
            val document : Document
            val element: Elements
            val ketqua:String
            try {
                document= Jsoup.connect(url).get()
                element= document.getElementsByClass("desktop-title-content")
                ketqua=element.text().toString()
                return ketqua
            }
            catch (jsoup:Exception){
                return null.toString()
                Log.d(TAG, "doInBackground: null")
            }

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result==""){
                Log.d(TAG, "onPostExecute: $result ")
                googleSearchWithout()
            } else{
                speak("Bạn đang ở $result")
                Log.d(TAG, "onPostExecute: $result ")
            }


        }

    }

    private fun openYoutube() {
        if (savedRecognitionEnglish==true){
            speak("Opening Youtube")
        }
        else{
            speak("Đang mở Youtube")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
        intent?.let { startActivity(it) }
    }
    private fun youtubeSearch() {
        if (savedRecognitionEnglish==false){
            var getKey=keeper.trim().split("e").toTypedArray()
            val keyWord=getKey[1]
            val searchIntent = Intent(Intent.ACTION_SEARCH)
            searchIntent.setPackage("com.google.android.youtube")
            searchIntent.putExtra(SearchManager.QUERY, keyWord)
            startActivity(searchIntent)
            speak("Đây là kết quả tìm kiếm $keyWord trên Youtube")
        } else{
            var getKey=keeper.trim().split("e").toTypedArray()
            val keyWord=getKey[2]
            val searchIntent = Intent(Intent.ACTION_SEARCH)
            searchIntent.setPackage("com.google.android.youtube")
            searchIntent.putExtra(SearchManager.QUERY, keyWord)
            startActivity(searchIntent)
            speak("There are $keyWord on Youtube")
        }

    }
    private fun openChillMusic(){
        var id:String?=null
        var rd=Random()
        var x=rd.nextInt(5);
        when{
            x==0->id="BUbSpHCVK0Q"
            x==1->id="ROy57arUE1s"
            x==2->id="ROy57arUE1s"
            x==3->id="ROy57arUE1s"
            x==4->id="FLHfYUdvfqk"
        }
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$id"))
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("http://www.youtube.com/watch?v=$id")
        )
        speak("Được thôi, mời bạn nghe bài hát sau")
        try {
            startActivity(appIntent)
        } catch (ex: ActivityNotFoundException) {
            startActivity(webIntent)
        }
    }
    private fun openSonTungMusic(){
        var id:String?=null
        var rd=Random()
        var x=rd.nextInt(5);
        when{
            x==0->id="30KI5SuECuc"
            x==1->id="psZ1g9fMfeo"
            x==2->id="FN7ALfpGxiI"
            x==3->id="6t-MjBazs3o"
            x==4->id="knW7-x7Y7RE"
        }
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$id"))
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("http://www.youtube.com/watch?v=$id")
        )
        speak("Oh yeah, tôi cũng là một fan của Sếp đấy ")
        try {
            startActivity(appIntent)
        } catch (ex: ActivityNotFoundException) {
            startActivity(webIntent)
        }
    }

    private fun openInstagram() {
        if (savedRecognitionEnglish==true){
            speak("Opening Instagram")
        }
        else{
            speak("Đang mở Instagram")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.instagram.android")
        intent?.let { startActivity(it) }
    }

    private fun openZalo() {
        if (savedRecognitionEnglish==true){
            speak("Opening Zalo")
        }
        else{
            speak("Đang mở Zalo")
        }
        val intent = packageManager.getLaunchIntentForPackage("com.zing.zalo")
        intent?.let { startActivity(it) }
    }

    private fun shareAFIle() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                SHAREAFILE
            )
        } else {
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            val myFileIntent = Intent(Intent.ACTION_GET_CONTENT)
            myFileIntent.type = "application/pdf"
            startActivityForResult(myFileIntent, REQUEST_CODE_SELECT_DOC)
        }
    }

    private fun shareATextMessage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                SHAREATEXTFILE
            )
        } else {
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            val message = keeper.split("that").toTypedArray()[1]
            val intentShare = Intent(Intent.ACTION_SEND)
            intentShare.type = "text/plain"
            intentShare.putExtra(Intent.EXTRA_TEXT, message)
            startActivity(Intent.createChooser(intentShare, "Sharing Text"))
        }
    }

    private fun callContact() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
                READCONTACTS
            )
        } else {
            val numberList = keeper.split(" ")
            val size=numberList.size
            var nameUn=""
            for (i in 2 until size){
                nameUn=nameUn+" "+numberList[i].capitalize()
            }
            var name=nameUn.trim()
            Log.d("chk",name)
            try {
                val cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone
                            .TYPE
                    ), "DISPLAY_NAME='$name'", null, null
                )
                cursor!!.moveToFirst()
                val number = cursor.getString(0)
                if (number.trim { it <= ' ' }.length > 0) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.CALL_PHONE),
                            REQUESTCALL
                        )
                    } else {
                        val dial = "tel:$number"
                        assistantViewModel.sendMessageToDatabase("Đang gọi cho $name",keeper)
                        startActivity(Intent(Intent.ACTION_CALL, Uri.parse(dial)))
                    }
                } else {
                    Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_LONG).show()
                }

            } catch (contact: Exception) {
                contact.printStackTrace()
                speak("Mình không tìm được tên bạn yêu cầu")
            }
        }

    }
    fun sendSMS2() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
                READCONTACTS
            )
        } else {
            val numberList = keeper.split(" ")
            val size = numberList.size
            var nameUn = ""
            for (i in 3 until size) {
                nameUn = nameUn + " " + numberList[i].capitalize()
            }
            val name = nameUn.trim()
            try {
                val cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone
                            .TYPE
                    ), "DISPLAY_NAME='$name'", null, null
                )
                cursor!!.moveToFirst()
                numberSMS = cursor.getString(0)
                speak("Ok, vậy nội dung gửi cho $name là gì ?")
                Handler().postDelayed({
                    keeper = ""
                    Log.d("keeperRong", keeper)
                    isNote=true
                    speechRecognizer.startListening(recognizerIntent)
                    Handler().postDelayed({
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            sendSMS()
                        }
                    }, 6000)
                }, 3600)
            }
            catch (sendSMS2:Exception){
                speak("Xin lỗi, mình không tìm thấy tên trong danh bạ")
            }
        }


    }

    private fun turnOnBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            if (savedRecognitionEnglish==true){
                speak("Turning On Bluetooth")
            }
            else{
                speak("Đang bật Bluetooth")
            }

            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            startActivityForResult(intent, REQUEST_ENABLE_BT)
        } else {
            if (savedRecognitionEnglish==true){
                speak("Bluetooth is already On")
            }
            else{
                speak("Bluetooth đã được bật")
            }

        }
    }

    private fun turnOffBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable()
            if (savedRecognitionEnglish==true){
                speak("Turning Off Bluetooth")
            }
            else{
                speak("Đang tắt Bluetooth")
            }
        } else {
            if (savedRecognitionEnglish==true){
                speak("Bluetooth is already Off")
            }
            else{
                speak("Bluetooth đã tắt rồi")
            }

        }
    }

    private fun getAllPaireDevice() {
        if (bluetoothAdapter.isEnabled()) {
            speak("Paired Devices are")
            var text = ""
            var count = 1
            val devices: Set<BluetoothDevice> = bluetoothAdapter.getBondedDevices()
            for (device in devices) {
                text += "\nDevice: $count ${device.name}, $device"
                count += 1
            }
            speak(text)
        } else {
            speak("Turn on bluetooth to get paired devices")

        }
    }

    private fun turnOnFlash() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraID, true)
                if (savedRecognitionEnglish==true){
                    speak("Flash turned on")
                }
                else{
                    speak("Đã bật đèn pin")
                }

            }
        } catch (flash: java.lang.Exception) {
            flash.printStackTrace()
            speak("Error Occurred")
        }
    }

    private fun turnOffFlash() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraID, false)
                if (savedRecognitionEnglish==true){
                    speak("Flash turned off")
                }
                else{
                    speak("Đã tắt đèn pin")
                }
            }
        } catch (offFlash: java.lang.Exception) {
            offFlash.printStackTrace()
            speak("Error Occurred")
        }
    }

    fun clipBoardCopy() {
        val data = keeper.split("that").toTypedArray()[1].trim { it <= ' ' }
        if (!data.isEmpty()) {
            val clipData = ClipData.newPlainText("text", data)
            speak("Data copied to clipboard that is $data")
        }
    }

    fun clipBoardSpeak() {
        val item = clipboardManager.primaryClip!!.getItemAt(0)
        val pasteData = item.text.toString()
        if (pasteData != "") {
            if (savedRecognitionEnglish==true){
                speak("Data stored in last clipboard is " + pasteData)
            } else{
                speak("Dữ liệu được ghi nhớ là " + pasteData)
            }

        } else {
            if (savedRecognitionEnglish==true){
                speak("Data is empty")
            } else{
                speak("Bộ nhớ hiện đang rỗng ")
            }
        }
    }

    private fun capturePhoto() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                CAPTUREPHOTO
            )
        } else {
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            imageIndex++
            val file: String = imageDirection + imageIndex + ".jpg"
            val newFile = File(file)

            try {
                newFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val outputFileUri = Uri.fromFile(newFile)
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
            startActivity(cameraIntent)
            if (savedRecognitionEnglish==true){
                speak("Photo will be saved to $file")
            }
            else{
                speak("Ảnh đã được lưu ở $file")
            }


        }
    }

    private fun playRingtone() {
        if (savedRecognitionEnglish==true){
            speak("Playing Ringtone")
        }
        else{
            speak("Đang bật nhạc chuông")
        }
        ringtone.play()
        Handler().postDelayed({
            ringtone.stop()
        }, 6000)
    }

    private fun stopRingtone() {
        speak("Ringtone Stopped")
        ringtone.stop()

    }

    private fun googleSearch() {
        try {
            if (savedRecognitionEnglish==true){
                var getKey= keeper.trim().split("e").toTypedArray()
                val keyWord=getKey[2]
                val intent=Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra(SearchManager.QUERY,keyWord)
                startActivity(intent)
                speak("Search results for $keyWord")
            }
            else{
                var getKey= keeper.trim().split("e").toTypedArray()
                val keyWord=getKey[1]
                val intent=Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra(SearchManager.QUERY,keyWord)
                startActivity(intent)
                speak("Sau đây là kết quả tìm kiếm cho $keyWord")
            }
        }
        catch (google:Exception){
            if (savedRecognitionEnglish==true){
                speak("Try agian, ex: Search google Spider Man")
            }else{
                speak("Thử lại, ví dụ: Tìm google Học viện Kỹ thuật Mật Mã")
            }
        }



    }
    private fun googleSearchWithout() {
        try {
            if (savedRecognitionEnglish==true){
                val intent=Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra(SearchManager.QUERY,keeper)
                startActivity(intent)
                speak("I found something you might need")
            }
            else{
                val intent=Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra(SearchManager.QUERY,keeper)
                startActivity(intent)
                speak("Sau đây là một số kết quả mình tìm được")
            }
        }
        catch (google:Exception){
            if (savedRecognitionEnglish==true){
                speak("Try agian, ex: Search google Spider Man")
            }else{
                speak("Thử lại, ví dụ: Tìm google Học viện Kỹ thuật Mật Mã")
            }
        }



    }
    private fun createNote() {
        isNote=true
        speak("Được chứ, vậy nội dung là gì")
        Handler().postDelayed({
            keeper=""
            Log.d("keeperRong",keeper)
            speechRecognizer.startListening(recognizerIntent)
            Handler().postDelayed({
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    upDbNote()
                }
            }, 6000)
        }, 2500)



    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun upDbNote(){
        //up data to rtdb
        val uid=user?.uid
        val timestamp = "" + System.currentTimeMillis()
        dbRef =FirebaseDatabase.getInstance().getReference("Note")
        if (keeper!=""){
            // sử dụng HashMap
            val noteTitle=""
            val noteDescription=keeper.trim()
            val noteTime=timestamp
            val hashMap = HashMap<Any, String>()
            hashMap.put("noteTitle",noteTitle)
            hashMap.put("noteDescription",aes.encrypt(noteDescription,noteTime.plus(2299)).toString())
            hashMap.put("noteTime",noteTime)
            dbRef.child("$uid").child("$timestamp").setValue(hashMap).addOnSuccessListener {
                speak("Đã thêm ghi chú thành công")
                isNote=false
            }.addOnFailureListener {
                Toast.makeText(this, "Thêm ghi chú thất bại", Toast.LENGTH_SHORT).show()
                isNote=false
            }

        }
        else{
            Toast.makeText(this, "Không nhận được nội dung ghi chú", Toast.LENGTH_SHORT).show()
        }
    }


    //set Alarm with Vietnamese
    //command dat bao thuc luc HH gio MM phut
    private fun setAlarm() {
        // handling Hour
        val keeperSplit = convert(keeper)!!.replace(" ".toRegex(),"").split("c").toTypedArray()
        val time:String
        if (convert(keeper)!!.contains("luc")){
             time =keeperSplit[2]
        } else{
             time =keeperSplit[1]
        }
        val getHour=time.split(":").toTypedArray()
        val hour=getHour[0]//get hour String
        val HOUR=hour.toInt()
        // handling Minute
        var minute:String
        if (keeper.contains(":")){
            minute=getHour[1]
        }
        else {
            minute="00"
        }
        val MINUTE=minute.toInt()
        //set alarm, intent to Alarm Clock
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
        intent.putExtra(AlarmClock.EXTRA_HOUR, HOUR)
        intent.putExtra(AlarmClock.EXTRA_MINUTES, MINUTE)
        if (HOUR <= 24 && MINUTE <= 60) {
            startActivity(intent)
        } else{
            speak("Thời gian bạn yêu cầu không chính xác, vui lòng thử lại")
        }
        speak("OK, mình đã đặt báo thức giúp bạn lúc $HOUR giờ $MINUTE phút")
    }
    //get weather with Vietnamese recognition
    private fun GetCurrentWeatherData() {
        //split keeper

        val keeperSlpit = convert(keeper)!!.replace(" ".toRegex(),"").split("t").toTypedArray()
        val city =keeperSlpit[3]
        val citySpeak=keeper.split("t").toTypedArray()
        val citySpeakFull=citySpeak[3]
        val requestQueue = Volley.newRequestQueue(this)
        val url =
            "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=f832efb17766467eb502d5d1145d7424&units=metric"
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val day = jsonObject.getString("dt")
//                    val name = jsonObject.getString("name")
                    val l = java.lang.Long.valueOf(day)
                    val date = Date(l * 1000L)
                    val simpleDateFormat = SimpleDateFormat("EEEE dd-MM-yyyy")
                    val Day = simpleDateFormat.format(date)
                    val jsonArrayWeather = jsonObject.getJSONArray("weather")
                    val jsonObjectWeather = jsonArrayWeather.getJSONObject(0)
                    val status = jsonObjectWeather.getString("main")
                    var STATUS:String =""
                    if (savedRecognitionEnglish==false){
                        when{
                            status=="Clouds"->STATUS="Trời có mây"
                            status=="Clear"->STATUS="Trời nắng"
                            status=="Rain"->STATUS="Trời mưa"
                        }
                    }

                    //main
                    val jsonObjectMain = jsonObject.getJSONObject("main")
                    val temp = jsonObjectMain.getString("temp")
                    val humidity = jsonObjectMain.getString("humidity")
                    //covert temp
                    val a = java.lang.Double.valueOf(temp)
                    val Temp = a.toInt().toString()
                    //speak
                    speak("""
                        Dự báo thời tiết khu vực $citySpeakFull 
                        $Day
                        $STATUS
                        Nhiệt độ khoảng $Temp độ C
                        Độ ẩm $humidity %
                    """.trimIndent())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        ) { speak("Vui lòng thử lại câu lệnh, ví dụ : Thời tiết Hà Nội")}
        requestQueue.add(stringRequest)
    }

    //get weather with English recognition
    private fun GetCurrentWeatherDataEnglish() {
        //split keeper

        val keeperSlpit = convert(keeper)!!.replace(" ".toRegex(),"").split("w").toTypedArray()
        val city =keeperSlpit[0]
        val requestQueue = Volley.newRequestQueue(this)
        val url =
            "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=f832efb17766467eb502d5d1145d7424&units=metric"
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val day = jsonObject.getString("dt")
                    val name = jsonObject.getString("name")
                    val l = java.lang.Long.valueOf(day)
                    val date = Date(l * 1000L)
                    val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy")
                    val Day = simpleDateFormat.format(date)
                    val jsonArrayWeather = jsonObject.getJSONArray("weather")
                    val jsonObjectWeather = jsonArrayWeather.getJSONObject(0)
                    val status = jsonObjectWeather.getString("main")
                    var STATUS:String =""
                    if (savedRecognitionEnglish==true){
                        when{
                            status=="Clouds"->STATUS="Sky is cloudy"
                            status=="Clear"->STATUS="Sky is clear"
                            status=="Rain"->STATUS="It's raining"
                        }
                    }

                    //main
                    val jsonObjectMain = jsonObject.getJSONObject("main")
                    val temp = jsonObjectMain.getString("temp")
                    val humidity = jsonObjectMain.getString("humidity")
                    //covert temp
                    val a = java.lang.Double.valueOf(temp)
                    val Temp = a.toInt().toString()
                    //speak
                    speak("""
                        Current weather in $name ,
                        $Day,
                        $STATUS,
                        The temperature is $Temp degrees Celsius
                        humidity is $humidity percent
                        
                    """.trimIndent())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        ) { speak("Please try again, for example : Ha Noi weather")}
        requestQueue.add(stringRequest)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUESTCALL) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makeNumberCall()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
            }
        } else
            if (requestCode == SENDSMS) {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSMS()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
                }
            } else
                if (requestCode == READSMS) {
                    if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        readSMS()
                    } else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
                    }
                }
                    else
                    if (requestCode == SHAREAFILE) {
                        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                            shareAFIle()
                        } else {
                            Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
                        }
                    } else
                        if (requestCode == SHAREATEXTFILE) {
                            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                shareATextMessage()
                            } else {
                                Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
                            }
                        } else
                            if (requestCode == READCONTACTS) {
                                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                    callContact()
                                    sendSMS2()
                                } else {
                                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG)
                                        .show()
                                }
                            } else
                                if (requestCode == CAPTUREPHOTO) {
                                    if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                        capturePhoto()
                                    } else {
                                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG)
                                            .show()
                                    }
                                } else
                                    if (requestCode == RecordAudioRequestCode && grantResults.size > 0) {
                                        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                            Toast.makeText(
                                                this,
                                                "Permission Granted",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                    }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RecordAudioRequestCode
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_DOC && resultCode == RESULT_OK) {
            val filePath = data!!.data!!.path
            Log.d("chk", "path: $filePath")
            val file = File(filePath)
            val intentShare = Intent(Intent.ACTION_SEND)
            intentShare.type = "application/pdf"
            intentShare.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://$file"))
            startActivity(Intent.createChooser(intentShare, "Share the file..."))
        }
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("TAGBluetooth", "onActivityResult: Bluetooth On")
            } else {
                if (savedRecognitionEnglish==true){
                    speak("Could not able to turn on Bluetooth")
                }
                else{
                    speak("Mình chưa được cấp quyền bật Bluetooth")
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
        speechRecognizer.cancel()
        speechRecognizer.destroy()
        Log.i(logsr, "destroy")
        Log.i(logtts, "destroy")
    }

    fun checkUser() {
        //get current user
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            // user not logged in
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            //user logged in
            //get user info
            val acct = GoogleSignIn.getLastSignedInAccount(this)
            if (acct != null) {
                personName = acct.givenName.toString()
                val personGivenName = acct.givenName
                val personFamilyName = acct.familyName
                val personEmail = acct.email
                val personId = acct.id
                val personPhoto: Uri? = acct.photoUrl
                Picasso.get().load(personPhoto).placeholder(R.drawable.ic_baseline_face_24)
                    .into(avatarIv)
                helloNameTv.text=personGivenName

                //get hour to hello
                val rightNow = Calendar.getInstance()
                val currentHourIn24Format: Int =rightNow.get(Calendar.HOUR_OF_DAY)
                when (currentHourIn24Format){
                    in 0..3->helloTv.text="Sáng mai gặp lại bạn nhé,"
                    in 4..10->helloTv.text="Chúc bạn ngày mới tốt lành,"
                    in 11..13->helloTv.text="Bữa trưa vui vẻ nhé,"
                    in 14..17->helloTv.text="Buổi chiều năng động nào,"
                    in 18..19->helloTv.text="Đến giờ ăn tối rồi,"
                    in 20..21->helloTv.text="Buổi tối thật chill nhé,"
                    in 22..23->helloTv.text="Chúc ngủ ngon,"
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        assistantViewModel.onClear()
    }


}