package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,         // e.g., "Sarf (Morphology)", "Nahw (Syntax)", "Hadith", "Fiqh", "Tafseer"
    val bookName: String,     // e.g., "Ilm-us-Sarf", "Hidayat un-Nahw", "Nur al-Idah", "Mishkat al-Masabih"
    val category: String,     // e.g., "Grammar", "Hadith Studies", "Jurisprudence", "Quranic Exegesis"
    val totalLessons: Int,    // total number of lessons/chapters in the syllabus
    val completedLessons: Int, // number of completed lessons
    val teacherName: String   // e.g., "Mufti Ahmad Sahab", "Moulana Yusuf"
)

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["subjectId"])]
)
data class SyllabusLesson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val title: String,         // e.g., "Sabaq 1: Definition of Sarf", "Sabaq 2: Parts of Speech"
    val lessonNumber: Int,     // for ordered display
    val isCompleted: Boolean = false,
    val completedAtUtc: Long? = null
)

@Entity(tableName = "milestone_reminders")
data class MilestoneReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val subjectName: String,
    val title: String,         // e.g., "Exam on Bab Al-Wudu", "Revise Sabaq 12"
    val milestoneTimeUtc: Long, // timestamp for notification
    val isEnableNotification: Boolean = true,
    val isNotified: Boolean = false
)
