package com.example.assistantkotlin.adapter

<<<<<<< HEAD
import android.content.Context
import android.content.Intent
=======
import android.os.Build
>>>>>>> f7782d4ec82442c8ef9a87d7059229983822508b
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.assistantkotlin.R
import com.example.assistantkotlin.extend.EditNoteActivity
import com.example.assistantkotlin.extend.NoteActivity
import com.example.assistantkotlin.model.ModelNote
import java.text.SimpleDateFormat
<<<<<<< HEAD
=======
import java.time.Instant
import java.time.ZoneId
>>>>>>> f7782d4ec82442c8ef9a87d7059229983822508b
import java.util.*

class NoteAdapter(private val noteList : ArrayList<ModelNote>) :RecyclerView.Adapter<NoteAdapter.MyViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView=LayoutInflater.from(parent.context).inflate(R.layout.item_note,parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem =noteList[position]
        holder.noteTitle.text=currentItem.noteTitle
        holder.noteDescription.text=currentItem.NoteDescription
<<<<<<< HEAD
        var time=currentItem.noteTime
        var noteTitle=currentItem.noteTitle
        var noteDescription=currentItem.NoteDescription
=======
//        holder.noteTime.text=currentItem.noteTime.
        var time=currentItem.noteTime
>>>>>>> f7782d4ec82442c8ef9a87d7059229983822508b
        if (time != null) {
            var TimeDay=getDateTime(time)
            holder.noteTime.text=TimeDay
        }
<<<<<<< HEAD
        holder.itemView.setOnClickListener {
            val intent = Intent(it.context,EditNoteActivity::class.java)
            //        init animation
            val turnRight = AnimationUtils.loadAnimation(it.context, R.anim.anim_turn_right)
            intent.putExtra("noteTimeStamp",time)
            intent.putExtra("noteTitle",noteTitle)
            intent.putExtra("noteDescription",noteDescription)
            holder.itemView.startAnimation(turnRight)
            it.context.startActivity(intent)
        }
=======
>>>>>>> f7782d4ec82442c8ef9a87d7059229983822508b

    }

    override fun getItemCount(): Int {
        return noteList.size
    }

    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val noteTitle: TextView=itemView.findViewById(R.id.noteTitleTv)
        val noteDescription: TextView=itemView.findViewById(R.id.noteDescriptionTv)
        val noteTime: TextView=itemView.findViewById(R.id.noteTimeTv)
    }
    private fun getDateTime(s: String): String? {
        try {
<<<<<<< HEAD
            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa")
=======
            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss")
>>>>>>> f7782d4ec82442c8ef9a87d7059229983822508b
            val netDate = Date(s.toLong())
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }
}