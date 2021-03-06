package com.example.assistantkotlin.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.assistantkotlin.MainActivity
import com.example.assistantkotlin.R
import com.example.assistantkotlin.fragment.RecognitionSettingFragment
import com.example.assistantkotlin.fragment.WidgetFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.tool_bar.*
import java.util.*

class SettingActivity : AppCompatActivity() {

    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)

        //back btn setOnclick
        backToolbarBtn.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        //get save setting
        //get key value from share preferences recognition
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val savedRecognition=sharedPreferences?.getString("recognition",null)
        if (savedRecognition=="English"){
            currentRecognitionTv.text="English"
        } else if (savedRecognition=="Vietnamese"){
            currentRecognitionTv.text="Tiếng Việt"
        }

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()
        //Logout btn
        LnLogout.setOnClickListener {
            LnLogout.startAnimation(zoomIn)
            firebaseAuth.signOut()
            checkUser()
        }

        //Recognition language setting
        LnRecognitionLanguage.setOnClickListener {
            LnRecognitionLanguage.startAnimation(zoomIn)
            var dialog = RecognitionSettingFragment()
            dialog.show(supportFragmentManager,"Recognition Language Setting")
        }
        LnWidget.setOnClickListener {
            LnWidget.startAnimation(zoomIn)
            var widgetDialog= WidgetFragment()
            widgetDialog.show(supportFragmentManager,"Widget Setting")
        }

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
            val email = firebaseUser.email
            emailSettingTv.text = email
            val acct = GoogleSignIn.getLastSignedInAccount(this)
            if (acct != null) {
                val personName = acct.displayName
                nameSettingTv.text=personName
                val personGivenName = acct.givenName
                val personFamilyName = acct.familyName
                val personEmail = acct.email
                val personId = acct.id
                val personPhoto: Uri? = acct.photoUrl
                Picasso.get().load(personPhoto).placeholder(R.drawable.ic_baseline_face_24)
                    .into(avatarIvSetting)
            }
        }
    }




    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, HomeActivity::class.java))
//        overridePendingTransition(R.anim.slide_right,R.anim.slide_left)
    }
}