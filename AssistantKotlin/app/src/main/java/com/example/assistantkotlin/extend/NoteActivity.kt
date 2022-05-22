package com.example.assistantkotlin.extend

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateFormat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assistantkotlin.R
import com.example.assistantkotlin.adapter.NoteAdapter
import com.example.assistantkotlin.assistant.HomeActivity
import com.example.assistantkotlin.model.ModelNote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.tool_bar.*
import kotlinx.android.synthetic.main.tool_bar_note.*
import java.util.*
import kotlin.collections.ArrayList

class NoteActivity : AppCompatActivity() {
    private lateinit var dbref: DatabaseReference
    private lateinit var noteRecyclerView: RecyclerView
    private lateinit var noteArrayList: ArrayList<ModelNote>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        noteRecyclerView=findViewById(R.id.noteList)
        noteRecyclerView.layoutManager=LinearLayoutManager(this)
        noteRecyclerView.setHasFixedSize(true)

        noteArrayList= arrayListOf<ModelNote>()

        getNoteData()
        backToolbarNoteBtn.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    private fun getNoteData() {
        dbref=FirebaseDatabase.getInstance().getReference("Note").child("note")
        dbref.addValueEventListener(object :ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    for (noteSnapshot in snapshot.children){

                        val note =noteSnapshot.getValue(ModelNote::class.java)
                        noteArrayList.add(note!!)
                    }
                    noteRecyclerView.adapter=NoteAdapter(noteArrayList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }
}