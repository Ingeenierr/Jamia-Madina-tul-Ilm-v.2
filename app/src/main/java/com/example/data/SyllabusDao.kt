package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusDao {

    // --- Subject Queries ---
    @Query("SELECT * FROM subjects ORDER BY id ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :subjectId LIMIT 1")
    fun getSubjectById(subjectId: Int): Flow<Subject?>

    @Query("SELECT * FROM subjects WHERE id = :subjectId LIMIT 1")
    suspend fun getSubjectByIdSuspend(subjectId: Int): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    // --- Lesson Queries ---
    @Query("SELECT * FROM lessons WHERE subjectId = :subjectId ORDER BY lessonNumber ASC")
    fun getLessonsForSubject(subjectId: Int): Flow<List<SyllabusLesson>>

    @Query("SELECT * FROM lessons WHERE subjectId = :subjectId ORDER BY lessonNumber ASC")
    suspend fun getLessonsForSubjectSuspend(subjectId: Int): List<SyllabusLesson>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: SyllabusLesson): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<SyllabusLesson>)

    @Update
    suspend fun updateLesson(lesson: SyllabusLesson)

    @Query("UPDATE lessons SET isCompleted = :isCompleted, completedAtUtc = :completedAt WHERE id = :lessonId")
    suspend fun updateLessonStatus(lessonId: Int, isCompleted: Boolean, completedAt: Long?)

    @Query("DELETE FROM lessons WHERE subjectId = :subjectId")
    suspend fun deleteLessonsForSubject(subjectId: Int)

    // --- Milestone Reminder Queries ---
    @Query("SELECT * FROM milestone_reminders ORDER BY milestoneTimeUtc ASC")
    fun getAllMilestoneReminders(): Flow<List<MilestoneReminder>>

    @Query("SELECT * FROM milestone_reminders WHERE milestoneTimeUtc > :currentTime AND isNotified = 0 ORDER BY milestoneTimeUtc ASC")
    fun getUpcomingMilestoneReminders(currentTime: Long): Flow<List<MilestoneReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: MilestoneReminder): Long

    @Update
    suspend fun updateMilestone(milestone: MilestoneReminder)

    @Query("UPDATE milestone_reminders SET isNotified = 1 WHERE id = :milestoneId")
    suspend fun markMilestoneAsNotified(milestoneId: Int)

    @Delete
    suspend fun deleteMilestone(milestone: MilestoneReminder)
}
