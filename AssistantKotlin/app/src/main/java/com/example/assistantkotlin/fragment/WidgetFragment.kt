package com.example.assistantkotlin.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import com.example.assistantkotlin.R
import com.example.assistantkotlin.assistant.HomeActivity
import com.example.assistantkotlin.assistant.SettingActivity
import com.example.assistantkotlin.extend.FloatingViewService
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
            var canDraw= true
            var intent: Intent?=null
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                intent =Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                canDraw=Settings.canDrawOverlays(activity)
                if (!canDraw && intent !=null){
                    startActivity(intent)
                } else{
                    val selectId= radioGroupWidget.checkedRadioButtonId
                    val radio =rootView.findViewById<RadioButton>(selectId)
                    var widgetResult = radio.text.toString()
                    if (widgetResult=="Bật"){
                        val service=Intent(activity, FloatingViewService::class.java)
                        activity?.startService(service)
                    } else{
                        val service=Intent(activity, FloatingViewService::class.java)
                        activity?.stopService(service)
                    }
                    dismiss()
                }
            }

        }

        return rootView

    }



}