package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.notification.NotificationScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SyllabusViewModel(private val repository: SyllabusRepository) : ViewModel() {

    // Expose lists of subjects reactively
    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expose all milestone reminders
    val milestones: StateFlow<List<MilestoneReminder>> = repository.allMilestones
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Details state
    private val _selectedSubjectId = MutableStateFlow<Int?>(null)
    val selectedSubjectId: StateFlow<Int?> = _selectedSubjectId.asStateFlow()

    val activeSubject: StateFlow<Subject?> = _selectedSubjectId
        .flatMapLatest { id ->
            if (id != null) repository.getSubjectById(id) else flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activeLessons: StateFlow<List<SyllabusLesson>> = _selectedSubjectId
        .flatMapLatest { id ->
            if (id != null) repository.getLessonsForSubject(id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectSubject(subjectId: Int?) {
        _selectedSubjectId.value = subjectId
    }

    // Toggle Lesson Checked/Unchecked
    fun toggleLesson(lessonId: Int, subjectId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleLessonCompletion(lessonId, subjectId, isCompleted)
        }
    }

    // Add scheduled milestone reminder
    fun addMilestoneReminder(
        context: Context,
        subjectId: Int,
        subjectName: String,
        title: String,
        triggerInMinutes: Int
    ) {
        viewModelScope.launch {
            val triggerTimeUtc = System.currentTimeMillis() + (triggerInMinutes * 60 * 1000L)
            val milestone = MilestoneReminder(
                subjectId = subjectId,
                subjectName = subjectName,
                title = title,
                milestoneTimeUtc = triggerTimeUtc,
                isEnableNotification = true,
                isNotified = false
            )
            // Insert in DB
            val generatedId = repository.insertMilestone(milestone)
            
            // Re-fetch milestone with generated ID to schedule the alarm
            val scheduledMilestone = milestone.copy(id = generatedId.toInt())
            
            // Set Alarm using scheduler
            NotificationScheduler.scheduleMilestoneAlarm(context.applicationContext, scheduledMilestone)
        }
    }

    // Delete reminder
    fun deleteReminder(context: Context, milestone: MilestoneReminder) {
        viewModelScope.launch {
            // Cancel system alarm
            NotificationScheduler.cancelMilestoneAlarm(context.applicationContext, milestone.id)
            // Delete from DB
            repository.deleteMilestone(milestone)
        }
    }

    // Add Subject helper (e.g. custom books)
    fun addSubject(name: String, bookName: String, category: String, totalLessons: Int, teacherName: String) {
        viewModelScope.launch {
            val subjectId = repository.insertSubject(
                Subject(
                    name = name,
                    bookName = bookName,
                    category = category,
                    totalLessons = totalLessons,
                    completedLessons = 0,
                    teacherName = teacherName
                )
            ).toInt()

            // Pre-insert default syllabus lesson check items
            val lessons = (1..totalLessons).map { num ->
                SyllabusLesson(
                    subjectId = subjectId,
                    title = "Sabaq $num: Topic Introduction",
                    lessonNumber = num,
                    isCompleted = false
                )
            }
            // Insert lessons in DB
            for (lesson in lessons) {
                repository.insertLesson(lesson)
            }
        }
    }
}

// Factory to inject repository into ViewModel
class SyllabusViewModelFactory(private val repository: SyllabusRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SyllabusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SyllabusViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
