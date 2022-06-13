package com.example.assistantkotlin.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.assistantkotlin.R
import com.example.assistantkotlin.model.ModelSuggest

class ListSuggestAdapter(private val context: Activity,private val arrayList: ArrayList<ModelSuggest>) :ArrayAdapter<ModelSuggest>(
    context, R.layout.list_item,arrayList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val view: View= inflater.inflate(R.layout.list_item,null)
        val imageView : ImageView =view.findViewById(R.id.IconSuggestIV)
        val textView: TextView =view.findViewById(R.id.suggestTv)


        imageView.setImageResource(arrayList[position].imgId)
        textView.text=arrayList[position].name

        return view
    }
}