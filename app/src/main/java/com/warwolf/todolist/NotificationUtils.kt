package com.warwolf.todolist

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

object NotificationUtils {

    fun createNotificationChannel(context: Context) {
        val name = "ToDo App Channel"
        val desc = "Kanał służący do wysyłania powiadomień o zbliżających się zadaniach i tych, których termin minął"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager = context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleNotification(context: Context, task: Task, type: String){
        val intent = Intent(context, NotificationReceiver::class.java)
        val title: String
        val message: String
        val notificationID: Int
        if(type == "Overdue") {
            title = "Termin minął"
            message = "Czas na wykonanie zadania '${task.taskName}' właśnie minął"
            notificationID = task.id * (-1)
        }else{
            title = "Przypomnienie"
            message = "Zbliża się termin na wykonanie zadania '${task.taskName}'"
            notificationID = task.id
        }
        intent.putExtra(titleExtra, title)
        intent.putExtra(messageExtra, message)
        intent.putExtra("notificationID", notificationID)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val deadline = Calendar.getInstance().apply {
            time = task.taskDeadlineDate
            set(Calendar.HOUR_OF_DAY, task.taskDeadlineTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, task.taskDeadlineTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }
        val time = deadline.clone() as Calendar
        if (type == "Upcoming") {
            time.add(Calendar.DAY_OF_YEAR, -1)

        }
        val notifyTime = time.timeInMillis
        Log.i("MyActivity", notifyTime.toString())
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            notifyTime,
            pendingIntent
        )

    }

    fun deleteNotifications(context: Context, notificationID: Int){
        val intent = Intent(context, NotificationReceiver::class.java)

        var pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationID*(-1),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)



    }

}