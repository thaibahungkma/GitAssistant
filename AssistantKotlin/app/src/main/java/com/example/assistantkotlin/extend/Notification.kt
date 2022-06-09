package com.example.assistantkotlin.extend

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient.getPackageName
import androidx.core.app.NotificationCompat
import com.example.assistantkotlin.R

const val notificationID = 1
const val channelID = "channel1"
const val titleExtra ="titleExtra"
const val messageExtra="messageExtra"

class Notification : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bitmap=BitmapFactory.decodeResource(context.resources,R.drawable.nhacnho)
        val sound =
            Uri.parse("android.resource://"+context.packageName+"/"+ R.raw.sound)
        val notification : Notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.iconssun)
            .setContentTitle(intent.getStringExtra(titleExtra))
            .setContentText(intent.getStringExtra(messageExtra))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setLargeIcon(bitmap)
            .setSound(sound)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationID,notification)
    }
}