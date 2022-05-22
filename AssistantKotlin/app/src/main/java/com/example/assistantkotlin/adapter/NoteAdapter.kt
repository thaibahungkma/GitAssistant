package com.example.assistantkotlin.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.assistantkotlin.R
import com.example.assistantkotlin.model.ModelNote
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
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
//        holder.noteTime.text=currentItem.noteTime.
        var time=currentItem.noteTime
        if (time != null) {
            var TimeDay=getDateTime(time)
            holder.noteTime.text=TimeDay
        }

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
            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss")
            val netDate = Date(s.toLong())
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }
}