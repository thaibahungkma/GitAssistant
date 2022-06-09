package com.example.assistantkotlin.extend

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.assistantkotlin.R
import com.example.assistantkotlin.assistant.HomeActivity
import kotlinx.android.synthetic.main.activity_remind.*
import kotlinx.android.synthetic.main.tool_bar_remind.*
import java.util.*

class RemindActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remind)
        createNotificationChannel()
        remindSubmitButton.setOnClickListener { scheduleNotification() }
        backToolbarRemindBtn.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun scheduleNotification() {
        val intent = Intent(applicationContext,Notification::class.java)
        var title= remindTitle.text.toString()
        val message = remindDescription.text.toString()
        if (title==""){
            title="Trợ lý Sun nhắc bạn"
        }
        intent.putExtra(titleExtra,title)
        intent.putExtra(messageExtra, message)
        if (message==""){
            Toast.makeText(this, "Bạn phải nhập lời nhắn", Toast.LENGTH_SHORT).show()
        } else{

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager=getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time= getTime()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
        showAlert(time, title,message)}

    }

    private fun showAlert(time: Long, title: String, message: String) {
        val date = Date(time)
        val dateFormat=android.text.format.DateFormat.getLongDateFormat(applicationContext)
        val timeFormat=android.text.format.DateFormat.getTimeFormat(applicationContext)

        AlertDialog.Builder(this)
            .setTitle("Đã lập lịch")
            .setMessage(
                "Tiêu đề: "+ title +
                        "\nLời nhắn: " +message+
                        "\nVào lúc: " +dateFormat.format(date)+ " " + timeFormat.format(date)

            )
            .setPositiveButton("OK"){_,_-> startActivity(Intent(this, HomeActivity::class.java))}
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getTime(): Long {
        val minute = remindTimePick.minute
        val hour = remindTimePick.hour
        val day=remindDatePick.dayOfMonth
        val month=remindDatePick.month
        val year=remindDatePick.year

        val calendar =Calendar.getInstance()
        calendar.set(year,month,day,hour,minute)
        return calendar.timeInMillis

    }


    @SuppressLint("WrongConstant")
    private fun createNotificationChannel() {
        val sound =
            Uri.parse("android.resource://"+packageName+"/"+ R.raw.sound)
        val audioAttributes= AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
        val name= "Nhắc nhở"
        val desc = "Nội dung nhắc nhở"
        val importance= NotificationManager.IMPORTANCE_MAX
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(channelID,name,importance)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        channel.description = desc
        channel.setSound(sound,audioAttributes)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}