package com.example.assistantkotlin.extend

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.assistantkotlin.AES.AES
import com.example.assistantkotlin.MainActivity
import com.example.assistantkotlin.R
import com.example.assistantkotlin.adapter.NoteAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import kotlinx.android.synthetic.main.activity_edit_note.*
import kotlinx.android.synthetic.main.tool_bar_editnote.*
import java.text.SimpleDateFormat
import java.util.*

class EditNoteActivity : AppCompatActivity() {
    private lateinit var dbref: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private var user: FirebaseUser? = null
    var aes: AES = AES()

    var noteTimeStamp: String? = null
    var noteTitle: String? = null
    var noteDescription: String? = null
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)
//        init animation
        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)

        //init Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        user = firebaseAuth.currentUser

        //setOnClick
        saveNoteBtn.setOnClickListener {
            saveNoteBtn.startAnimation(zoomIn)
            upDateNote()

        }
        deleteNoteBtn.setOnClickListener {
            deleteNoteBtn.startAnimation(zoomIn)
            showDialogDelete()
        }
        backToolbarEditNoteBtn.setOnClickListener {
            startActivity(Intent(this,NoteActivity::class.java))
        }

        //get String from Note Adapter setOnClick itemView
        val intent = intent
        noteTimeStamp = intent.getStringExtra("noteTimeStamp")
        noteTitle = intent.getStringExtra("noteTitle")
        noteDescription = intent.getStringExtra("noteDescription")

        //set text for Note Edit
        editNoteTime.text="Được tạo lúc: "+ noteTimeStamp?.let { getDateTime(it) }
        editNoteTitleEt.setText(noteTitle)
        editNoteDescriptionEt.setText(noteDescription)

    }

    private fun showDialogDelete(){
        val dialogBuilder = AlertDialog.Builder(this)
        // set message of alert dialog
        dialogBuilder.setMessage("Bạn có chắc muốn xóa ghi chú này không ?")
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action
            .setPositiveButton("Xóa", DialogInterface.OnClickListener {
                    dialog, id -> finish()
                deleteNote()
            })
            // negative button text and action
            .setNegativeButton("Thoát", DialogInterface.OnClickListener {
                    dialog, id -> dialog.cancel()
            })

        // create dialog box
        val alert = dialogBuilder.create()
        // set title for alert dialog box
        alert.setTitle("Xóa ghi chú")
        // show alert dialog
        alert.show()
    }

    private fun deleteNote() {
        //xu ly ok xoa
        val uid=user?.uid
        dbref= FirebaseDatabase.getInstance().getReference("Note")
        dbref.child("$uid").child("$noteTimeStamp").removeValue().addOnSuccessListener {
            Toast.makeText(this, "Đã ghi chú xóa thành công", Toast.LENGTH_SHORT).show()
            editNoteTitleEt.text.clear()
            editNoteDescriptionEt.text.clear()
            Handler().postDelayed({
                startActivity(Intent(this,NoteActivity::class.java))
                }, 500)
        }.addOnFailureListener {
            Toast.makeText(this, "Xóa ghi chú thất bại", Toast.LENGTH_SHORT).show()
        }
    }

    //convert timestamp to day
    private fun getDateTime(s: String): String? {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa")
            val netDate = Date(s.toLong())
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun upDateNote(){
        val NoteTitleUpdate=editNoteTitleEt.text.toString().trim()
        val NoteDescriptionUpdate=editNoteDescriptionEt.text.toString().trim()
        val uid=user?.uid
        dbref= FirebaseDatabase.getInstance().getReference("Note")
        if (!(TextUtils.isEmpty(NoteTitleUpdate) and TextUtils.isEmpty(NoteDescriptionUpdate))){
            val NoteDescriptionUpdateEncry= aes.encrypt(NoteDescriptionUpdate,noteTimeStamp.plus(2299))
            var hashMap = mapOf<String,String>(
                "noteTitle" to NoteTitleUpdate,
                "noteDescription" to NoteDescriptionUpdateEncry.toString()
            )

            dbref.child("$uid").child("$noteTimeStamp").updateChildren(hashMap).addOnSuccessListener {
                Toast.makeText(this, "Đã cập nhập thành công", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this,NoteActivity::class.java))
            }.addOnFailureListener {
                Toast.makeText(this, "Cập nhật ghi chú thất bại", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            Toast.makeText(this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show()
        }


    }

    private fun checkUser() {
        //get current user
        if (user == null) {
            // user not logged in
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}