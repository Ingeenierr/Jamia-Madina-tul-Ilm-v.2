package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Subject::class, SyllabusLesson::class, MilestoneReminder::class],
    version = 1,
    exportSchema = false
)
abstract class SyllabusDatabase : RoomDatabase() {

    abstract fun syllabusDao(): SyllabusDao

    companion object {
        @Volatile
        private var INSTANCE: SyllabusDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): SyllabusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SyllabusDatabase::class.java,
                    "syllabus_database"
                )
                    .addCallback(SyllabusDatabaseCallback(context, scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SyllabusDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.syllabusDao())
                }
            }
        }

        suspend fun populateDatabase(dao: SyllabusDao) {
            // Prepopulate default high-fidelity Madrassa subjects
            val subjects = listOf(
                Subject(1, "Sarf (Arabic Morphology)", "Ilm-us-Sarf", "Arabic Grammar", 10, 3, "Moulana Ilyas Ahmad"),
                Subject(2, "Nahw (Arabic Syntax)", "Ilm-an-Nahw", "Arabic Grammar", 10, 2, "Mufti Ahmad Sahab"),
                Subject(3, "Fiqh (Jurisprudence)", "Nur al-Idah", "Jurisprudence", 8, 1, "Mufti Rizwan Khan"),
                Subject(4, "Hadith Studies", "Mishkat al-Masabih", "Prophetic Traditions", 6, 2, "Moulana Yusuf Qadri"),
                Subject(5, "Quranic Exegesis", "Tafseer al-Jalalayn", "Quranic Exegesis", 6, 1, "Shaykh Abdul Hadi"),
                Subject(6, "Hifdh (Quran Memorization)", "Juz 'Amma", "Quranic Memorization", 10, 4, "Qari Bilal Husain")
            )
            dao.insertSubjects(subjects)

            // Prepopulate lessons
            val lessons = mutableListOf<SyllabusLesson>()

            // 1. Sarf Lessons
            lessons.add(SyllabusLesson(subjectId = 1, title = "Introduction to Sarf (Word Types)", lessonNumber = 1, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 1, title = "The Three-Letter Verb (Thulathi)", lessonNumber = 2, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 1, title = "Past Tense Conjugation (Al-Maadi)", lessonNumber = 3, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 1, title = "Present Tense Conjugation (Al-Mudari)", lessonNumber = 4, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 1, title = "Active & Passive Participles (Fa'il / Maf'ul)", lessonNumber = 5, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 1, title = "Command Verbs (Al-Amr)", lessonNumber = 6, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 1, title = "Prohibitive Verbs (An-Nahy)", lessonNumber = 7, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 1, title = "Enhanced Verbs (Thulathi Mazeed)", lessonNumber = 8, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 1, title = "Irregular Verbs (Mu'tall)", lessonNumber = 9, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 1, title = "Revision & Morphological Scales (Meezan)", lessonNumber = 10, isCompleted = false))

            // 2. Nahw Lessons
            lessons.add(SyllabusLesson(subjectId = 2, title = "Introduction to Syntax & Sentence Structure", lessonNumber = 1, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 2, title = "The Nominal Sentence (Al-Jumla al-Ismiyyah)", lessonNumber = 2, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 2, title = "The Verbal Sentence (Al-Jumla al-Fi'liyyah)", lessonNumber = 3, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 2, title = "Declension Types (I'rab)", lessonNumber = 4, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 2, title = "The Subject & Object (Fa'il & Maf'ul)", lessonNumber = 5, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 2, title = "Genitive Constructions (Idafah)", lessonNumber = 6, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 2, title = "Prepositions (Huroof al-Jarr)", lessonNumber = 7, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 2, title = "Weak and Defective Nouns", lessonNumber = 8, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 2, title = "Conditional and Relative Sentences", lessonNumber = 9, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 2, title = "Final Advanced Syntax & Parsing (I'rab Analysis)", lessonNumber = 10, isCompleted = false))

            // 3. Fiqh Lessons
            lessons.add(SyllabusLesson(subjectId = 3, title = "Introduction to Fiqh & Fatwas", lessonNumber = 1, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 3, title = "Cleanliness & Ablution (Taharah & Wudu)", lessonNumber = 2, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 3, title = "Bathing (Ghusl) and Water Regulations", lessonNumber = 3, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 3, title = "Aesthetic conditions & Timings of Salah", lessonNumber = 4, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 3, title = "Congregational Prayer & Imamate Rules", lessonNumber = 5, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 3, title = "Fasting (Sawm) rules & Invalidators", lessonNumber = 6, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 3, title = "Charity & Almsgiving (Zakat)", lessonNumber = 7, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 3, title = "Laws of Hajj (Pilgrimage)", lessonNumber = 8, isCompleted = false))

            // 4. Hadith Lessons
            lessons.add(SyllabusLesson(subjectId = 4, title = "Hadith 1: Sincerity of Intentions (Niyyah)", lessonNumber = 1, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 4, title = "Hadith 2: Islam, Iman, and Ihsan (Jibreel)", lessonNumber = 2, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 4, title = "Hadith 3: The Pillars of Islam", lessonNumber = 3, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 4, title = "Hadith 4: Character, Ethics and Kindness", lessonNumber = 4, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 4, title = "Hadith 5: Halal and Haram boundaries", lessonNumber = 5, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 4, title = "Hadith 6: Islamic Brotherhood and Social Etiquette", lessonNumber = 6, isCompleted = false))

            // 5. Tafseer Lessons
            lessons.add(SyllabusLesson(subjectId = 5, title = "Surah Al-Fatihah (The Opening) Tafseer", lessonNumber = 1, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 5, title = "Surah Al-Kahf (Lessons from the Cave)", lessonNumber = 2, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 5, title = "Surah Yaseen (Heart of the Quran)", lessonNumber = 3, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 5, title = "Surah Ar-Rahman (Mercies of Allah)", lessonNumber = 4, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 5, title = "Surah Al-Mulk (Dominion & Protection)", lessonNumber = 5, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 5, title = "Last Ten Surahs Word-by-Word Analysis", lessonNumber = 6, isCompleted = false))

            // 6. Hifdh Lessons
            lessons.add(SyllabusLesson(subjectId = 6, title = "Surah An-Nas & Al-Falaq", lessonNumber = 1, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 6, title = "Surah Al-Ikhlas & Al-Masad", lessonNumber = 2, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 6, title = "Surah An-Nasr & Al-Kafirun", lessonNumber = 3, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 6, title = "Surah Al-Kawthar & Al-Ma'un", lessonNumber = 4, isCompleted = true, completedAtUtc = System.currentTimeMillis()))
            lessons.add(SyllabusLesson(subjectId = 6, title = "Surah Quraysh & Al-Fil", lessonNumber = 5, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 6, title = "Surah Al-Humazah & Al-'Asr", lessonNumber = 6, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 6, title = "Surah At-Takathur & Al-Qari'ah", lessonNumber = 7, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 6, title = "Surah Al-'Adiyat & Az-Zalzalah", lessonNumber = 8, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 6, title = "Surah Al-Bayyinah & Al-Qadr", lessonNumber = 9, isCompleted = false))
            lessons.add(SyllabusLesson(subjectId = 6, title = "Surah Al-'Alaq & At-Tin", lessonNumber = 10, isCompleted = false))

            dao.insertLessons(lessons)

            // Prepopulate some interactive future milestone reminders
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            val testMilestones = listOf(
                MilestoneReminder(1, 1, "Sarf (Arabic Morphology)", "Verb Conjugation Board Oral Exam", now + dayMs + 2 * 3600 * 1000L),
                MilestoneReminder(2, 3, "Fiqh (Jurisprudence)", "Wudu Practical Demonstration", now + 2 * dayMs + 4 * 3600 * 1000L),
                MilestoneReminder(3, 2, "Nahw (Arabic Syntax)", "I'rab Analysis of Al-Fatihah Submissions", now + 3 * dayMs)
            )
            for (m in testMilestones) {
                dao.insertMilestone(m)
            }
        }
    }
}
