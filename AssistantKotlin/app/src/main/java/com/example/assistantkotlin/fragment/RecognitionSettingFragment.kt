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
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.fragment_recognition_setting_dialog.*
import kotlinx.android.synthetic.main.fragment_recognition_setting_dialog.view.*

class RecognitionSettingFragment:DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var rootView: View =inflater.inflate(R.layout.fragment_recognition_setting_dialog, container,false)

        //cancel dialog setOnclick
        rootView.cancelRecognitionBtn.setOnClickListener {
            dismiss()
        }
        //apply dialog setOnclick
        rootView.applyRecognitionBtn.setOnClickListener {
            val selectId= radioGroupRecognitionSetting.checkedRadioButtonId
            val radio =rootView.findViewById<RadioButton>(selectId)
            var recognitionResult = radio.text.toString()
            val sharedPreferences = activity?.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
            val editor= sharedPreferences?.edit()
            editor?.apply {
                putString("recognition",recognitionResult)
            }?.apply()
            dismiss()
        }

        return rootView

    }



}