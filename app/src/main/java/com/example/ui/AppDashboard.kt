package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.MilestoneReminder
import com.example.data.Subject
import com.example.data.SyllabusLesson
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDashboard(
    viewModel: SyllabusViewModel,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val subjects by viewModel.subjects.collectAsState()
    val milestones by viewModel.milestones.collectAsState()

    // Screen states
    val activeSubject by viewModel.activeSubject.collectAsState()
    val activeLessons by viewModel.activeLessons.collectAsState()
    val activeSubjectId by viewModel.selectedSubjectId.collectAsState()

    // Modals & Sheets
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    // Request Notification Permissions dynamically for API 33+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Lesson milestones will trigger alert sounds and popups!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissions denied; reminders will slide in silently.", Toast.LENGTH_SHORT).show()
        }
    }

    // Trigger Permission request on start
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("main_scaffold"),
        floatingActionButton = {
            if (activeSubjectId == null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddSubjectDialog = true },
                        icon = { Icon(Icons.Default.Book, "Add Custom Book") },
                        text = { Text("New Book") },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.testTag("add_book_fab_btn")
                    )

                    FloatingActionButton(
                        onClick = { showAddMilestoneDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_milestone_fab_btn")
                    ) {
                        Icon(Icons.Default.AddAlert, "Add Syllabus Milestone")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Main Dashboard view OR Details overlay via visual sliding transition
            AnimatedContent(
                targetState = activeSubjectId,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { width -> width / 2 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width / 2 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> width / 2 } + fadeOut())
                    }
                },
                label = "DashboardTransition"
            ) { subId ->
                if (subId == null) {
                    // Dashboards Panel
                    DashboardMainView(
                        subjects = subjects,
                        milestones = milestones,
                        onSubjectClick = { sId -> viewModel.selectSubject(sId) },
                        onDeleteMilestone = { milestone -> viewModel.deleteReminder(context, milestone) },
                        hasPermission = hasNotificationPermission,
                        requestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                } else {
                    // Detailed Syllabus Syllabus Checkoff View for selected subject
                    SubjectSyllabusView(
                        subject = activeSubject,
                        lessons = activeLessons,
                        onBack = { viewModel.selectSubject(null) },
                        onToggleLesson = { lId, sId, checked ->
                            viewModel.toggleLesson(lId, sId, checked)
                        },
                        onAddMilestoneShortcut = { sId, subName, title, triggerMins ->
                            viewModel.addMilestoneReminder(context, sId, subName, title, triggerMins)
                        }
                    )
                }
            }

            // Milestone Scheduler Dialog overlay
            if (showAddMilestoneDialog) {
                AddMilestonePopup(
                    subjects = subjects,
                    onDismiss = { showAddMilestoneDialog = false },
                    onConfirm = { sId, sName, title, minutes ->
                        viewModel.addMilestoneReminder(context, sId, sName, title, minutes)
                        showAddMilestoneDialog = false
                        Toast.makeText(context, "Reminder generated for '$title'!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Subject adder dialog overlay
            if (showAddSubjectDialog) {
                AddSubjectPopup(
                    onDismiss = { showAddSubjectDialog = false },
                    onConfirm = { name, bookName, category, targetSize, teacher ->
                        viewModel.addSubject(name, bookName, category, targetSize, teacher)
                        showAddSubjectDialog = false
                        Toast.makeText(context, "Class/Book '$bookName' registered!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// FORMAT HELPER
fun formatTimestamp(millis: Long): String {
    val date = Date(millis)
    val sdf = SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault())
    return sdf.format(date)
}

@Composable
fun DashboardMainView(
    subjects: List<Subject>,
    milestones: List<MilestoneReminder>,
    onSubjectClick: (Int) -> Unit,
    onDeleteMilestone: (MilestoneReminder) -> Unit,
    hasPermission: Boolean,
    requestPermission: () -> Unit
) {
    // Recalculate global status fraction
    val totalLsn = subjects.sumOf { it.totalLessons }
    val compLsn = subjects.sumOf { it.completedLessons }
    val overallPerc = if (totalLsn > 0) (compLsn * 100 / totalLsn) else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High fidelity Islamic Header Banner
        item {
            InteractiveHeaderBanner(
                studentName = "Muhammad Hamza Chattha",
                overallPercentage = overallPerc,
                completedCount = compLsn,
                totalCount = totalLsn,
                hasPermission = hasPermission,
                onRequestPerm = requestPermission
            )
        }

        // Section Title: My Subjects
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jamia Books & Syllabus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "${subjects.size} Enrolled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // List of Subjects
        if (subjects.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Subjects Configured",
                    desc = "Use the buttons in the action panel to register interactive syllabus books."
                )
            }
        } else {
            items(subjects, key = { it.id }) { subject ->
                SubjectProgressCard(
                    subject = subject,
                    onClick = { onSubjectClick(subject.id) }
                )
            }
        }

        // Upcoming Milestone Reminders block
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upcoming Milestones & Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (milestones.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No Scheduled Milestones",
                    desc = "Click the alarm FAB icon below to configure high-fidelity alerts in 1 Minute, tomorrow or next week for lessons or oral examinations."
                )
            }
        } else {
            items(milestones, key = { it.id }) { reminder ->
                MilestoneAlertCard(
                    reminder = reminder,
                    onDelete = { onDeleteMilestone(reminder) }
                )
            }
        }
    }
}

// 1. ISLAMIC RE-DESIGN HEADER BANNER WITH COHESIVE PROGRESS RING
@Composable
fun InteractiveHeaderBanner(
    studentName: String,
    overallPercentage: Int,
    completedCount: Int,
    totalCount: Int,
    hasPermission: Boolean,
    onRequestPerm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                ),
                RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jamia Madina-tul-Ilm",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Dars-e-Nizami Student Board",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Decorative Islamic Crescent moon icon or status
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dual Grid: Left stats text, Right modern progress wheel
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = studentName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Overall Syllabus Progression: $overallPercentage%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$completedCount completed out of $totalCount total topics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    // Notice if permissions missing
                    if (!hasPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                .clickable { onRequestPerm() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Alarms restricted. Tap to fix.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // High fidelity circular projection
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { overallPercentage / 100f },
                        modifier = Modifier
                            .size(76.dp)
                            .testTag("overall_progress_ring"),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 8.dp,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$overallPercentage%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Finished",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// 2. DETAILED SUBJECT PROGRESSION CARD (WITH SUBTITLE, TEACHER & CUSTOM INDICATORS)
@Composable
fun SubjectProgressCard(
    subject: Subject,
    onClick: () -> Unit
) {
    val progressFraction = if (subject.totalLessons > 0) (subject.completedLessons.toFloat() / subject.totalLessons) else 0f
    val progressPercentage = (progressFraction * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .testTag("subject_card_${subject.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Icon + Name + Category label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Book: ${subject.bookName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Category tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = subject.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle: Teacher & Completed Sabaq stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = "Teacher",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subject.teacherName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${subject.completedLessons}/${subject.totalLessons} Sabaq finished",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual M3 Progress indicator with customized gold details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("progress_indicator_${subject.id}"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$progressPercentage%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// 3. MILESTONE REMINDER CARD WITH REMOVAL TRIGGER
@Composable
fun MilestoneAlertCard(
    reminder: MilestoneReminder,
    onDelete: () -> Unit
) {
    val isPast = reminder.milestoneTimeUtc < System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isPast) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                RoundedCornerShape(12.dp)
            )
            .testTag("milestone_card_${reminder.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) 
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bell alert shape colored based on trigger status
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPast) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPast) Icons.Default.CheckCircle else Icons.Default.Alarm,
                        contentDescription = "Notification State",
                        tint = if (isPast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                               else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = reminder.subjectName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Alarm: " + formatTimestamp(reminder.milestoneTimeUtc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_milestone_btn_${reminder.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete alarm reminder",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 4. DETAILED SYLLABUS LIST & ACTIONS SHEET OVERLAY
@Composable
fun SubjectSyllabusView(
    subject: Subject?,
    lessons: List<SyllabusLesson>,
    onBack: () -> Unit,
    onToggleLesson: (Int, Int, Boolean) -> Unit,
    onAddMilestoneShortcut: (Int, String, String, Int) -> Unit
) {
    if (subject == null) return

    val progressFraction = if (subject.totalLessons > 0) (subject.completedLessons.toFloat() / subject.totalLessons) else 0f
    val progressPercentage = (progressFraction * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("syllabus_subject_view")
    ) {
        // Top navigation bar with customized emerald detailing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back back to main dashboard",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Instructor: ${subject.teacherName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        // Stats card under headers
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Active Course Textbook",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subject.bookName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Syllabus Completed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${subject.completedLessons} of ${subject.totalLessons} Sabaq ($progressPercentage%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }
        }

        // Shortcut Milestone Scheduler specifically for this subject
        var customMilestoneTitle by remember { mutableStateOf("") }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Quick Schedule Next Sabaq Milestone Reminder:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = customMilestoneTitle,
                        onValueChange = { customMilestoneTitle = it },
                        placeholder = { Text("e.g. Fiqh Examination / Sarf Sabaq 10 Oral Prep") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_milestone_input"),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customMilestoneTitle.isNotBlank()) {
                                // Add milestone shortcut trigger in 2 Minutes for instant testing!
                                onAddMilestoneShortcut(subject.id, subject.name, customMilestoneTitle, 2)
                                customMilestoneTitle = ""
                            }
                        },
                        modifier = Modifier.testTag("quick_milestone_add_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Alert in 2m")
                    }
                }
            }
        }

        // Syllabus Itemized list
        Text(
            text = "Book Chapters / Lessons Checkoff List",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("lessons_checkbox_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (lessons.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Syllabus Chapters Loaded",
                        desc = "Add lessons or contact database support."
                    )
                }
            } else {
                items(lessons, key = { it.id }) { lesson ->
                    LessonCheckboxRow(
                        lesson = lesson,
                        onCheckedChange = { isChecked ->
                            onToggleLesson(lesson.id, subject.id, isChecked)
                        }
                    )
                }
            }
        }
    }
}

// 5. INTERACTIVE SYLLABUS CHECKBOX ROW
@Composable
fun LessonCheckboxRow(
    lesson: SyllabusLesson,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (lesson.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                RoundedCornerShape(10.dp)
            )
            .testTag("lesson_row_${lesson.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (lesson.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored check dot or lesson order box
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (lesson.isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (lesson.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Finished",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = lesson.lessonNumber.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lesson.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                               else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (lesson.isCompleted && lesson.completedAtUtc != null) {
                        Text(
                            text = "Achieved: " + formatTimestamp(lesson.completedAtUtc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Checkbox(
                checked = lesson.isCompleted,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag("lesson_check_${lesson.id}"),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// EMPTY CARD STANDBY UX
@Composable
fun EmptyStateCard(
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AssignmentLate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

// 6. POPUP SCREEN FOR ADDING MILESTONES
@Composable
fun AddMilestonePopup(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableStateOf(0) }
    var minutesOffset by remember { mutableStateOf(2) } // trigger in 2 mins default

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                .testTag("add_milestone_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "New Syllabus Milestone Reminder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Configure an offline-first system alarm to trigger notifications for review times or test milestones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select Book / Subject
                Text(
                    text = "Select Quran/Grammar Book:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (subjects.isEmpty()) {
                    Text(
                        text = "⚠️ Please register a textbook book first!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_subject_spinner"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(text = subjects[selectedIndex].name + " (Book: " + subjects[selectedIndex].bookName + ")")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            subjects.forEachIndexed { i, sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name) },
                                    onClick = {
                                        selectedIndex = i
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                Text(
                    text = "Milestone Title / Topic Exam:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Sarf Bab al-Majaazi Oral Exam / Hadith Memorization") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("milestone_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Time selector for custom ease of testing! (1 Min, 2 Min, 5 Min, 1 Hour, Tomorrow)
                Text(
                    text = "Trigger System Notification Reminder:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 5, 60, 1440).forEach { mins ->
                        val label = when (mins) {
                            1 -> "1 Min"
                            2 -> "2 Min"
                            5 -> "5 Min"
                            60 -> "1 Hour"
                            else -> "Tomorrow"
                        }
                        FilterChip(
                            selected = minutesOffset == mins,
                            onClick = { minutesOffset = mins },
                            label = { Text(label) },
                            modifier = Modifier.testTag("time_chip_$mins")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel/Confirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && subjects.isNotEmpty()) {
                                onConfirm(
                                    subjects[selectedIndex].id,
                                    subjects[selectedIndex].name,
                                    title,
                                    minutesOffset
                                )
                            }
                        },
                        enabled = title.isNotBlank() && subjects.isNotEmpty(),
                        modifier = Modifier.testTag("confirm_add_milestone_btn")
                    ) {
                        Text("Schedule Alarm")
                    }
                }
            }
        }
    }
}

// 7. POPUP SCREEN FOR ADDING BOOKS
@Composable
fun AddSubjectPopup(
    onDismiss: () -> Unit,
    onConfirm: (name: String, bookName: String, category: String, targetSize: Int, teacher: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bookName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Arabic Grammar") }
    var teacher by remember { mutableStateOf("") }
    var totalLessonsText by remember { mutableStateOf("10") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(20.dp))
                .testTag("add_subject_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Enrol New Book Outline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Register a book in the Jamia curriculum and auto-generate itemized interactive syllabus checklists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Subject Name
                Text(
                    text = "Syllabus Name:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Advanced Arabic Sarf") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("book_subject_input_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Book Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Primary Text Book Used:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = bookName,
                            onValueChange = { bookName = it },
                            placeholder = { Text("e.g. Tasheel al-Sarf") },
                            modifier = Modifier.testTag("book_name_input_field_item"),
                            singleLine = true
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Category/Genre:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            placeholder = { Text("e.g. Grammar / Fiqh") },
                            modifier = Modifier.testTag("book_category_item"),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Instructor & Lessons size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = "Syllabus Instructor / Mufti:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = teacher,
                            onValueChange = { teacher = it },
                            placeholder = { Text("e.g. Moulana Ahmad Sahab") },
                            modifier = Modifier.testTag("book_instructor_item"),
                            singleLine = true
                        )
                    }

                    Column(modifier = Modifier.weight(0.7f)) {
                        Text(
                            text = "Total Sabaqs:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = totalLessonsText,
                            onValueChange = { totalLessonsText = it },
                            placeholder = { Text("10") },
                            modifier = Modifier.testTag("book_lessons_count_item"),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel/Confirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val totalSize = totalLessonsText.toIntOrNull() ?: 10
                            if (name.isNotBlank() && bookName.isNotBlank()) {
                                onConfirm(name, bookName, category, totalSize, teacher)
                            }
                        },
                        enabled = name.isNotBlank() && bookName.isNotBlank(),
                        modifier = Modifier.testTag("confirm_add_book_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Add to Syllabus")
                    }
                }
            }
        }
    }
}
