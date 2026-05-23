package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.SyllabusDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MilestoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("milestone_id", 0)
        val subjectName = intent.getStringExtra("subject_name") ?: "Syllabus Update"
        val title = intent.getStringExtra("milestone_title") ?: "An upcoming milestone is scheduled."

        // 1. Show the user notification
        NotificationHelper.showMilestoneNotification(context, id, subjectName, title)

        // 2. Mark as notified in database using a background coroutine
        val pendingResult = goAsync()
        val database = SyllabusDatabase.getDatabase(context.applicationContext, CoroutineScope(Dispatchers.IO))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.syllabusDao().markMilestoneAsNotified(id)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
