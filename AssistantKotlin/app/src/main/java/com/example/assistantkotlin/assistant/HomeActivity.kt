package com.example.assistantkotlin.assistant

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.ContactsContract
import android.provider.MediaStore
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
import com.example.assistantkotlin.MainActivity
import com.example.assistantkotlin.R
import com.example.assistantkotlin.data.AssistantDatabase
import com.example.assistantkotlin.databinding.ActivityHomeBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_home.*
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

    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth

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

        //Mov to Setting Activity
        avatarIv.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))

        }

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
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
        if (savedRecognition=="English"){
            savedRecognitionEnglish=true
        } else if (savedRecognition=="Vietnamese"){
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
//        if (savedRecognition=="English"){
//            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
//        }
//        else if (savedRecognition=="Vietnamese"){
//            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VI")
//        }
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
                            keeper.lowercase().contains("thank you") -> speak("No problem, it's my job")
                            keeper.lowercase().contains("welcome") -> speak("for what?")
                            keeper.lowercase().contains("help me") -> speak("Alright, can i help you ?")
                            keeper.lowercase().contains("i love you") -> speak("i love you too")
                            keeper.lowercase().contains("your name") -> speak("My name is Sun Assistant")
                            keeper.lowercase().contains("created you") -> speak("Thai ba hung created me while working on my graduation project")
                            keeper.lowercase().contains("clear") -> assistantViewModel.onClear()
                            keeper.lowercase().contains("date") -> getDate()
                            keeper.lowercase().contains("what time") -> getTime()
                            keeper.lowercase().contains("phone call") -> makeAPhoneCall()
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
                            keeper.lowercase().contains("open google translate")-> openGooleTranslate()
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
                            keeper.lowercase().contains("copy to clipboard") -> clipBoardCopy()
                            keeper.lowercase().contains("read last clipboard") -> clipBoardSpeak()
                            keeper.lowercase().contains("capture a photo") || keeper.contains("Take a photo") -> capturePhoto()
                            keeper.lowercase().contains("play ringtone") -> playRingtone()
                            keeper.lowercase().contains("stop ringtone") || keeper.contains("Top ringtone") -> stopRingtone()
                            keeper.lowercase().contains("alarm") -> setAlarm()
                            keeper.lowercase().contains("weather") -> weather()
                            keeper.lowercase().contains("joke") -> joke()
                            keeper.lowercase().contains("question") -> question()
                            keeper.lowercase().contains("hello") || keeper.contains("hi") || keeper.contains("hey")
                            -> speak("Hello, how I can help you?")
                            else -> speak("Sorry, please try again")}
                    }
                    else if (savedRecognition=="Vietnamese"){
                        var ketqua=convert(keeper)!!.lowercase()
                        when {
                            ketqua.contains("cam on") -> speak("Không có chi, hí hí")
                            ketqua.contains("choi game") -> speak("Lo làm lo học đi bạn, chơi ít thôi")
                            ketqua.contains("giup toi") -> speak("tất nhiên rồi, tôi có thể giúp gì cho bạn ?")
                            ketqua.contains("anh yeu em") ||ketqua.contains("toi yeu ban") || ketqua.contains("em yeu anh")-> speak("tôi cũng yêu bạn")
                            ketqua.contains("ten ban") -> speak("Tên tôi là trợ lý Sun")
                            ketqua.contains("tao ra ban") ||ketqua.contains("tao ban")||ketqua.contains("lap trinh ban")-> speak("Anh Hùng đẹp trai lập trình nên tôi lúc làm đồ án")
                            ketqua.contains("xoa chat") -> assistantViewModel.onClear()
                            ketqua.contains("hom nay ngay bao nhieu") ||ketqua.contains("hom nay la ngay bao nhieu")||ketqua.contains("hom nay ngay may")||ketqua.contains("ngay thang")-> getDate()
                            ketqua.contains("may gio")||ketqua.contains("xem gio") -> getTime()
                            ketqua.contains("goi dien") -> makeAPhoneCall()
                            ketqua.contains("gui tin nhan") -> sendSMS()
                            ketqua.contains("doc tin nhan")||ketqua.contains("doc sms") -> readSMS()
                            ketqua.contains("mo gmail") -> openGmail()
                            ketqua.contains("mo facebook") -> openFaceBook()
                            ketqua.contains("mo message") -> openMessages()
                            ketqua.contains("mo instagram") -> openInstagram()
                            ketqua.contains("mo youtube") -> openYoutube()
                            ketqua.contains("mo zalo") -> openZalo()
                            ketqua.contains("mo trinh duyet") ||ketqua.contains("mo chrome")-> openChrome()
                            ketqua.contains("mo messenger")-> openMessenger()
                            ketqua.contains("mo google dich")-> openGooleTranslate()
                            ketqua.contains("mo netflix")||ketqua.contains("xem netflix")-> openNetflix()
                            ketqua.contains("mo zing mp3")->openZingMp3()
                            ketqua.contains("nghe nhac")-> openZingMp3ne()
                            ketqua.contains("mo google map")||ketqua.contains("mo ban do")-> openGoogleMap()
                            ketqua.contains("share a file") -> shareAFIle()
                            ketqua.contains("share a text message") -> shareATextMessage()
                            ketqua.contains("goi danh ba") -> callContact()
                            ketqua.contains("bat bluetooth") -> turnOnBluetooth()
                            ketqua.contains("tat bluetooth") -> turnOffBluetooth()
                            ketqua.contains("thiet bi bluetooth") -> getAllPaireDevice()
                            ketqua.contains("bat flash") ||ketqua.contains("bat den pin")-> turnOnFlash()
                            ketqua.contains("tat flash") ||ketqua.contains("tat den pin")-> turnOffFlash()
                            ketqua.contains("copy vao bo nho") ||ketqua.contains("sao chep")-> clipBoardCopy()
                            ketqua.contains("doc bo nho tam") -> clipBoardSpeak()
                            ketqua.contains("chup anh") || ketqua.contains("may anh") -> capturePhoto()
                            ketqua.contains("play ringtone")||ketqua.contains("bat nhac chuong") -> playRingtone()
                            ketqua.contains("stop ringtone") || ketqua.contains("dung nhac") -> stopRingtone()
                            keeper.lowercase().contains("alarm") -> setAlarm()
                            keeper.lowercase().contains("weather") -> weather()
                            keeper.lowercase().contains("joke") -> joke()
                            keeper.lowercase().contains("question") -> question()
                            ketqua.contains("xin chao") || ketqua.contains("hi") || ketqua.contains("hey")
                            -> speak("Chào bạn, bạn có cần tôi giúp gì?")
                            else -> speak("Xin lỗi, vui lòng thử lại")}
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
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        assistantViewModel.sendMessageToDatabase(keeper,text)
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
    private fun makeAPhoneCall() {
        val keeperSplit = keeper.replace("".toRegex(), "").split("o").toTypedArray()
        val number = keeperSplit[2]
        //no space
        if (number.trim { it <= ' ' }.length > 0) {
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
                speak("Calling $number")
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse(dial)))
            }
        } else {
            Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendSMS() {
        Log.d("keeper", "Done0")
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SENDSMS)
            Log.d("keeper", "Done1")
        } else {
            Log.d("keeper", "Done2")
            val keeperReplaced = keeper.replace("".toRegex(), "")
            val number = keeperReplaced.split("o").toTypedArray()[1].split("t").toTypedArray()[0]
            val message = keeper.split("that").toTypedArray()[1]
            Log.d("chk", number + message)
            val mySmsManager = SmsManager.getDefault()
            mySmsManager.sendTextMessage(
                number.trim { it <= ' ' }, null, message.trim() { it <= ' ' }, null, null
            )
            speak("Message sent that $message")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), READSMS)
        } else {
            val cursor = contentResolver.query(Uri.parse("content://sms"), null, null, null)
            cursor!!.moveToFirst()
            speak("Your last message was" + cursor.getString(12))
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
    private fun openGooleTranslate() {
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
            val name = keeper.split("call").toTypedArray()[1].trim { it <= ' ' }
            Log.d("chk", name)
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
                        startActivity(Intent(Intent.ACTION_CALL, Uri.parse(dial)))
                    }
                } else {
                    Toast.makeText(this, "Enter Phone Number", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                speak("Something when wrong")
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
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
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
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
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
            speak("Data stored in last clipboard is" + pasteData)
        } else {
            speak("Clipboard is Empty")
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

    private fun joke() {

    }

    private fun question() {

    }

    private fun setAlarm() {
        TODO("Not yet implemented")
    }

    private fun weather() {
        TODO("Not yet implemented")
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
                makeAPhoneCall()
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
                } else
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
                speak("Bluetooth is on")
            } else {
                speak("Could not able to turn on Bluetooth")
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

    private fun checkUser() {
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
                val personName = acct.displayName
                val personGivenName = acct.givenName
                val personFamilyName = acct.familyName
                val personEmail = acct.email
                val personId = acct.id
                val personPhoto: Uri? = acct.photoUrl
                Picasso.get().load(personPhoto).placeholder(R.drawable.ic_baseline_face_24)
                    .into(avatarIv)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

}