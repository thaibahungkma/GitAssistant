package com.example.assistantkotlin.extend

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateFormat
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assistantkotlin.MainActivity
import com.example.assistantkotlin.R
import com.example.assistantkotlin.adapter.NoteAdapter
import com.example.assistantkotlin.assistant.HomeActivity
import com.example.assistantkotlin.model.ModelNote
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.tool_bar.*
import kotlinx.android.synthetic.main.tool_bar_note.*
import java.util.*
import kotlin.collections.ArrayList

class NoteActivity : AppCompatActivity() {
    private lateinit var dbref: DatabaseReference
    private lateinit var noteRecyclerView: RecyclerView
    private lateinit var noteArrayList: ArrayList<ModelNote>
    private lateinit var firebaseAuth: FirebaseAuth
    private var user: FirebaseUser? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        //init Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        user = firebaseAuth.currentUser
        checkUser()
        //recyclerView
        noteRecyclerView=findViewById(R.id.noteList)
        noteRecyclerView.layoutManager=LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        noteRecyclerView.setHasFixedSize(true)
        noteArrayList= arrayListOf<ModelNote>()

        getNoteData()
        backToolbarNoteBtn.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    private fun getNoteData() {
        val uid=user?.uid
        dbref=FirebaseDatabase.getInstance().getReference("Note").child("$uid")
        dbref.addValueEventListener(object :ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    for (noteSnapshot in snapshot.children){

                        val note =noteSnapshot.getValue(ModelNote::class.java)
                        noteArrayList.add(note!!)
                    }
                    noteRecyclerView.adapter=NoteAdapter(noteArrayList.reversed() as java.util.ArrayList<ModelNote>)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }
    private fun checkUser() {
        //get current user
        if (user == null) {
            // user not logged in
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, HomeActivity::class.java))
    }
}