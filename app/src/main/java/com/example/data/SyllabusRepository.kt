package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SyllabusRepository(private val syllabusDao: SyllabusDao) {

    val allSubjects: Flow<List<Subject>> = syllabusDao.getAllSubjects()

    fun getSubjectById(subjectId: Int): Flow<Subject?> = syllabusDao.getSubjectById(subjectId)

    fun getLessonsForSubject(subjectId: Int): Flow<List<SyllabusLesson>> = syllabusDao.getLessonsForSubject(subjectId)

    val allMilestones: Flow<List<MilestoneReminder>> = syllabusDao.getAllMilestoneReminders()

    suspend fun insertSubject(subject: Subject): Long {
        return syllabusDao.insertSubject(subject)
    }

    suspend fun updateSubject(subject: Subject) {
        syllabusDao.updateSubject(subject)
    }

    suspend fun deleteSubject(subject: Subject) {
        // Also delete associated lessons first to maintain referential integrity
        syllabusDao.deleteLessonsForSubject(subject.id)
        syllabusDao.deleteSubject(subject)
    }

    suspend fun toggleLessonCompletion(lessonId: Int, subjectId: Int, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        // 1. Update lesson completion status
        syllabusDao.updateLessonStatus(lessonId, isCompleted, completedAt)

        // 2. Query all lessons for this subject to recalculate progress count
        val updatedLessons = syllabusDao.getLessonsForSubjectSuspend(subjectId)
        val completedCount = updatedLessons.count { it.isCompleted }

        // 3. Update the subject entry in the database
        val subject = syllabusDao.getSubjectByIdSuspend(subjectId)
        if (subject != null) {
            val updatedSubject = subject.copy(
                completedLessons = completedCount,
                totalLessons = if (updatedLessons.isNotEmpty()) updatedLessons.size else subject.totalLessons
            )
            syllabusDao.updateSubject(updatedSubject)
        }
    }

    suspend fun insertLesson(lesson: SyllabusLesson): Long {
        val id = syllabusDao.insertLesson(lesson)
        
        // Recalculate subject total/completed lesson counts
        recalculateSubjectCounts(lesson.subjectId)
        return id
    }

    private suspend fun recalculateSubjectCounts(subjectId: Int) {
        val updatedLessons = syllabusDao.getLessonsForSubjectSuspend(subjectId)
        val completedCount = updatedLessons.count { it.isCompleted }
        
        val subject = syllabusDao.getSubjectByIdSuspend(subjectId)
        if (subject != null) {
            val updatedSubject = subject.copy(
                completedLessons = completedCount,
                totalLessons = updatedLessons.size
            )
            syllabusDao.updateSubject(updatedSubject)
        }
    }

    suspend fun insertMilestone(milestone: MilestoneReminder): Long {
        return syllabusDao.insertMilestone(milestone)
    }

    suspend fun updateMilestone(milestone: MilestoneReminder) {
        syllabusDao.updateMilestone(milestone)
    }

    suspend fun deleteMilestone(milestone: MilestoneReminder) {
        syllabusDao.deleteMilestone(milestone)
    }

    suspend fun markMilestoneAsNotified(milestoneId: Int) {
        syllabusDao.markMilestoneAsNotified(milestoneId)
    }
}
