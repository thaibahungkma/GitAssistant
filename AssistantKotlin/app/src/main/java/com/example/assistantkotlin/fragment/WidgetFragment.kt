package com.example.assistantkotlin.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import com.example.assistantkotlin.R
import com.example.assistantkotlin.assistant.HomeActivity
import com.example.assistantkotlin.assistant.SettingActivity
import kotlinx.android.synthetic.main.fragment_recognition_setting_dialog.*
import kotlinx.android.synthetic.main.fragment_recognition_setting_dialog.view.*
import kotlinx.android.synthetic.main.fragment_widget.*
import kotlinx.android.synthetic.main.fragment_widget.view.*

class WidgetFragment:DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var rootView: View =inflater.inflate(R.layout.fragment_widget, container,false)

        //cancel dialog setOnclick
        rootView.cancelWidget.setOnClickListener {
            dismiss()
        }
        //apply dialog setOnclick
        rootView.applyWidget.setOnClickListener {
            val selectId= radioGroupWidget.checkedRadioButtonId
            val radio =rootView.findViewById<RadioButton>(selectId)
            var recognitionResult = radio.text.toString()
            dismiss()
        }

        return rootView

    }



}