package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.MilestoneReminder

object NotificationScheduler {

    fun scheduleMilestoneAlarm(context: Context, milestone: MilestoneReminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // Don't schedule past times
        if (milestone.milestoneTimeUtc < System.currentTimeMillis() || !milestone.isEnableNotification) {
            return
        }

        val intent = Intent(context, MilestoneReceiver::class.java).apply {
            putExtra("milestone_id", milestone.id)
            putExtra("subject_name", milestone.subjectName)
            putExtra("milestone_title", milestone.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            milestone.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Exact alarm with idle allowance, with safe fallback for security permissions in Android 13+
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        milestone.milestoneTimeUtc,
                        pendingIntent
                    )
                } catch (se: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        milestone.milestoneTimeUtc,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    milestone.milestoneTimeUtc,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelMilestoneAlarm(context: Context, milestoneId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        val intent = Intent(context, MilestoneReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            milestoneId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
