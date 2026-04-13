package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.features.reminders.components.ReminderSection
import java.util.*

private data class TabSpec(val title: String, val icon: ImageVector)

private data class MissionEditorInput(
    val mission: TacticalMission,
    val attachmentOptions: List<AttachmentOption>,
    val projectOptions: List<ProjectOption>,
    val reminderTime: Long?,
    val sheetMode: Boolean,
)

private data class MissionEditorCallbacks(
    val onSetReminder: (Long) -> Unit,
    val onClearReminder: () -> Unit,
    val onDismiss: () -> Unit,
    val onConfirm: (String, String, Long, MissionStatus, List<String>, List<String>) -> Unit,
    val onCreateRootContext: (suspend (String) -> String?)?,
    val onCreateDocument: (suspend (NewDocumentDraft) -> String?)?,
)

private data class MissionEditorDraft(
    val title: String,
    val description: String,
    val deadline: Long,
    val status: MissionStatus,
    val selectedTab: Int,
)

private data class MissionEditorContentState(
    val draft: MissionEditorDraft,
    val reminderTime: Long?,
    val projectLinks: List<String>,
    val attachmentLinks: List<String>,
    val sheetMode: Boolean,
)

private data class MissionEditorContentActions(
    val onDraftChange: (MissionEditorDraft) -> Unit,
    val onSave: () -> Unit,
    val onDismiss: () -> Unit,
    val onOpenDeadlinePicker: () -> Unit,
    val onClearDeadline: () -> Unit,
    val onOpenAttachments: () -> Unit,
    val onOpenContexts: () -> Unit,
    val onRemoveAttachment: (String) -> Unit,
    val onRemoveProject: (String) -> Unit,
    val onSetReminder: (Long) -> Unit,
    val onClearReminder: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionEditorRoute(
    input: MissionEditorInput,
    callbacks: MissionEditorCallbacks,
    modifier: Modifier = Modifier,
) {
    var draft by remember(input.mission) {
        mutableStateOf(
            MissionEditorDraft(
                title = input.mission.title,
                description = input.mission.description ?: "",
                deadline = input.mission.deadline,
                status = input.mission.status,
                selectedTab = 0,
            ),
        )
    }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    var activeLinkPickerTab by remember { mutableStateOf<LinkPickerTab?>(null) }
    val projectLinks =
        remember(input.mission.id) {
            mutableStateListOf<String>().apply {
                addAll(input.mission.linkedProjectIds.orEmpty())
            }
        }
    val attachmentLinks =
        remember(input.mission.id) {
            mutableStateListOf<String>().apply {
                addAll(input.mission.linkedAttachmentIds.orEmpty())
            }
        }
    val tabs =
        remember {
            listOf(
                TabSpec("General", Icons.Outlined.Description),
                TabSpec("Attachments", Icons.Outlined.AttachFile),
                TabSpec("Context Links", Icons.Outlined.AccountTree),
            )
        }

    val saveMission = {
        callbacks.onConfirm(
            draft.title,
            draft.description,
            draft.deadline,
            draft.status,
            projectLinks.toList(),
            attachmentLinks.toList(),
        )
    }

    MissionEditorContent(
        state =
            MissionEditorContentState(
                draft = draft,
                reminderTime = input.reminderTime,
                projectLinks = projectLinks,
                attachmentLinks = attachmentLinks,
                sheetMode = input.sheetMode,
            ),
        actions =
            MissionEditorContentActions(
                onDraftChange = { draft = it },
                onSave = saveMission,
                onDismiss = callbacks.onDismiss,
                onOpenDeadlinePicker = { showDeadlinePicker = true },
                onClearDeadline = {
                    draft = draft.copy(deadline = NO_DEADLINE)
                },
                onOpenAttachments = { activeLinkPickerTab = LinkPickerTab.ATTACHMENTS },
                onOpenContexts = { activeLinkPickerTab = LinkPickerTab.CONTEXTS },
                onRemoveAttachment = { attachmentLinks.remove(it) },
                onRemoveProject = { projectLinks.remove(it) },
                onSetReminder = callbacks.onSetReminder,
                onClearReminder = callbacks.onClearReminder,
            ),
        tabs = tabs,
        attachmentOptions = input.attachmentOptions,
        projectOptions = input.projectOptions,
        modifier = modifier,
    )

    MissionEditorOverlays(
        draft = draft,
        activeLinkPickerTab = activeLinkPickerTab,
        attachmentOptions = input.attachmentOptions,
        projectOptions = input.projectOptions,
        projectLinks = projectLinks,
        attachmentLinks = attachmentLinks,
        showDeadlinePicker = showDeadlinePicker,
        onDeadlineDismiss = { showDeadlinePicker = false },
        onDeadlineConfirm = {
            draft = draft.copy(deadline = it)
            showDeadlinePicker = false
        },
        onLinkPickerDismiss = { activeLinkPickerTab = null },
        onContextSelected = { id ->
            if (!projectLinks.contains(id)) {
                projectLinks.add(id)
            }
            activeLinkPickerTab = null
        },
        onAttachmentSelected = { id ->
            if (!attachmentLinks.contains(id)) {
                attachmentLinks.add(id)
            }
            activeLinkPickerTab = null
        },
        onCreateRootContext = callbacks.onCreateRootContext,
        onCreateDocument = callbacks.onCreateDocument,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionEditorContent(
    state: MissionEditorContentState,
    actions: MissionEditorContentActions,
    tabs: List<TabSpec>,
    attachmentOptions: List<AttachmentOption>,
    projectOptions: List<ProjectOption>,
    modifier: Modifier = Modifier,
) {
    val titleField = state.draft.title

    Surface(
        modifier = modifier.fillMaxSize(),
        color =
            if (state.sheetMode) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.background
            },
    ) {
        Scaffold(
            topBar = {
                MissionEditorTopBar(
                    sheetMode = state.sheetMode,
                    titleField = titleField,
                    selectedTab = state.draft.selectedTab,
                    tabs = tabs,
                    onTitleChange = { actions.onDraftChange(state.draft.copy(title = it)) },
                    onSave = actions.onSave,
                    onDismiss = actions.onDismiss,
                    onTabSelected = { actions.onDraftChange(state.draft.copy(selectedTab = it)) },
                )
            },
            containerColor =
                if (state.sheetMode) {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    MaterialTheme.colorScheme.background
                },
            contentWindowInsets =
                if (state.sheetMode) {
                    WindowInsets(0, 0, 0, 0)
                } else {
                    ScaffoldDefaults.contentWindowInsets
                },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .then(if (state.sheetMode) Modifier.navigationBarsPadding() else Modifier)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = if (state.sheetMode) 16.dp else 20.dp,
                            vertical = 16.dp,
                        ),
                verticalArrangement = Arrangement.spacedBy(if (state.sheetMode) 16.dp else 20.dp),
            ) {
                when (state.draft.selectedTab) {
                    0 ->
                        MissionGeneralTab(
                            draft = state.draft,
                            reminderTime = state.reminderTime,
                            onDraftChange = actions.onDraftChange,
                            onOpenDeadlinePicker = actions.onOpenDeadlinePicker,
                            onClearDeadline = actions.onClearDeadline,
                            onSetReminder = actions.onSetReminder,
                            onClearReminder = actions.onClearReminder,
                        )
                    1 ->
                        AttachmentLinksTab(
                            attachmentLinks = state.attachmentLinks,
                            attachmentOptions = attachmentOptions,
                            onRemoveAttachment = actions.onRemoveAttachment,
                            onOpenAttachments = actions.onOpenAttachments,
                        )
                    else ->
                        ContextLinksTab(
                            projectLinks = state.projectLinks,
                            projectOptions = projectOptions,
                            onRemoveProject = actions.onRemoveProject,
                            onOpenContexts = actions.onOpenContexts,
                        )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionEditorTopBar(
    sheetMode: Boolean,
    titleField: String,
    selectedTab: Int,
    tabs: List<TabSpec>,
    onTitleChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onTabSelected: (Int) -> Unit,
) {
    if (sheetMode) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = titleField,
                    onValueChange = onTitleChange,
                    placeholder = {
                        Text(
                            "Назва місії…",
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                    fontWeight = FontWeight.Normal,
                                ),
                        )
                    },
                    textStyle =
                        MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Normal,
                        ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = missionFieldColors(),
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    enabled = titleField.isNotBlank(),
                    onClick = onSave,
                    colors =
                        IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Зберегти",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        modifier = Modifier.height(40.dp),
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        colors =
                            SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        icon = {},
                    ) {
                        Text(
                            text = tab.title,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    } else {
        TopAppBar(
            title = {
                Text(
                    "Edit Mission",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .padding(8.dp)
                            .size(40.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            actions = {
                FilledTonalButton(
                    enabled = titleField.isNotBlank(),
                    onClick = onSave,
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    icon = null,
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(tab.icon, contentDescription = null)
                            if (selectedTab == index) {
                                Text(tab.title)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun MissionEditorScreen(
    mission: TacticalMission,
    attachmentOptions: List<AttachmentOption>,
    projectOptions: List<ProjectOption>,
    reminderTime: Long?,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, MissionStatus, List<String>, List<String>) -> Unit,
    onCreateRootContext: (suspend (String) -> String?)? = null,
    onCreateDocument: (suspend (NewDocumentDraft) -> String?)? = null,
    sheetMode: Boolean = false,
    modifier: Modifier = Modifier,
) = MissionEditorRoute(
    input =
        MissionEditorInput(
            mission = mission,
            attachmentOptions = attachmentOptions,
            projectOptions = projectOptions,
            reminderTime = reminderTime,
            sheetMode = sheetMode,
        ),
    callbacks =
        MissionEditorCallbacks(
            onSetReminder = onSetReminder,
            onClearReminder = onClearReminder,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            onCreateRootContext = onCreateRootContext,
            onCreateDocument = onCreateDocument,
        ),
    modifier = modifier,
)

@Composable
private fun MissionGeneralTab(
    draft: MissionEditorDraft,
    reminderTime: Long?,
    onDraftChange: (MissionEditorDraft) -> Unit,
    onOpenDeadlinePicker: () -> Unit,
    onClearDeadline: () -> Unit,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
) {
    MissionDetailsCard(
        title = draft.title,
        description = draft.description,
        onTitleChange = { onDraftChange(draft.copy(title = it)) },
        onDescriptionChange = { onDraftChange(draft.copy(description = it)) },
    )
    MissionDeadlineCard(
        deadline = draft.deadline,
        onOpenDeadlinePicker = onOpenDeadlinePicker,
        onClearDeadline = onClearDeadline,
    )
    MissionReminderCard(
        reminderTime = reminderTime,
        onSetReminder = onSetReminder,
        onClearReminder = onClearReminder,
    )
    MissionStatusCard(
        status = draft.status,
        onStatusChange = { onDraftChange(draft.copy(status = it)) },
    )
}

@Composable
private fun MissionDetailsCard(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface,
                                ),
                        ),
                    ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Mission Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Mission Title") },
                    placeholder = { Text("Enter mission title...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    placeholder = { Text("Add mission description...") },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 5,
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                )
            }
        }
    }
}

@Composable
private fun MissionDeadlineCard(
    deadline: Long,
    onOpenDeadlinePicker: () -> Unit,
    onClearDeadline: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                ),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            ),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDeadlinePicker)
                    .padding(20.dp),
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Deadline",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatDate(deadline),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                IconButton(onClick = onClearDeadline) {
                    Icon(
                        imageVector = Icons.Outlined.EventBusy,
                        contentDescription = "Очистити дедлайн",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionReminderCard(
    reminderTime: Long?,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                ),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        "Нагадування",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Окреме сповіщення для цієї місії",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ReminderSection(
                reminderTime = reminderTime,
                onSetReminder = { year, month, day, hour, minute ->
                    val calendar =
                        Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                    onSetReminder(calendar.timeInMillis)
                },
                onClearReminder = onClearReminder,
            )
        }
    }
}

@Composable
private fun MissionStatusCard(
    status: MissionStatus,
    onStatusChange: (MissionStatus) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Статус",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(MissionStatus.ACTIVE, MissionStatus.INACTIVE, MissionStatus.PAUSED).forEach { item ->
                    FilterChip(
                        selected = status == item,
                        onClick = { onStatusChange(item) },
                        label = { Text(missionStatusLabel(item)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentLinksTab(
    attachmentLinks: List<String>,
    attachmentOptions: List<AttachmentOption>,
    onRemoveAttachment: (String) -> Unit,
    onOpenAttachments: () -> Unit,
) {
    MissionLinksCard(
        title = "Attachments",
        count = attachmentLinks.size,
        icon = Icons.Outlined.AttachFile,
        accentColor = MaterialTheme.colorScheme.secondary,
        accentContainer = MaterialTheme.colorScheme.secondaryContainer,
        emptyLabel = "No attachments yet",
        actionLabel = "Add Attachment",
        onActionClick = onOpenAttachments,
    ) {
        attachmentLinks.forEach { id ->
            RemovableLinkChip(
                label = attachmentOptions.firstOrNull { it.id == id }?.name ?: id,
                onRemove = { onRemoveAttachment(id) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ContextLinksTab(
    projectLinks: List<String>,
    projectOptions: List<ProjectOption>,
    onRemoveProject: (String) -> Unit,
    onOpenContexts: () -> Unit,
) {
    MissionLinksCard(
        title = "Contexts",
        count = projectLinks.size,
        icon = Icons.Outlined.AccountTree,
        accentColor = MaterialTheme.colorScheme.primary,
        accentContainer = MaterialTheme.colorScheme.primaryContainer,
        emptyLabel = "No contexts linked",
        actionLabel = "Add Context",
        onActionClick = onOpenContexts,
    ) {
        projectLinks.forEach { id ->
            RemovableLinkChip(
                label = projectOptions.firstOrNull { it.id == id }?.name ?: id,
                onRemove = { onRemoveProject(id) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MissionLinksCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    accentContainer: Color,
    emptyLabel: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    chips: @Composable () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = accentColor.copy(alpha = 0.12f),
                ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(accentContainer.copy(alpha = 0.25f), MaterialTheme.colorScheme.surface),
                        ),
                    ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = CircleShape,
                        color = accentContainer,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = count == 0,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            emptyLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = count > 0,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        chips()
                    }
                }

                FilledTonalButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(actionLabel, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun RemovableLinkChip(
    label: String,
    onRemove: () -> Unit,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun MissionEditorOverlays(
    draft: MissionEditorDraft,
    activeLinkPickerTab: LinkPickerTab?,
    attachmentOptions: List<AttachmentOption>,
    projectOptions: List<ProjectOption>,
    projectLinks: List<String>,
    attachmentLinks: List<String>,
    showDeadlinePicker: Boolean,
    onDeadlineDismiss: () -> Unit,
    onDeadlineConfirm: (Long) -> Unit,
    onLinkPickerDismiss: () -> Unit,
    onContextSelected: (String) -> Unit,
    onAttachmentSelected: (String) -> Unit,
    onCreateRootContext: (suspend (String) -> String?)?,
    onCreateDocument: (suspend (NewDocumentDraft) -> String?)?,
) {
    if (showDeadlinePicker) {
        DeadlinePickerDialog(
            initialTime = if (draft.deadline == NO_DEADLINE) System.currentTimeMillis() else draft.deadline,
            onDismiss = onDeadlineDismiss,
            onConfirm = onDeadlineConfirm,
        )
    }

    activeLinkPickerTab?.let { initialTab ->
        LinkedTargetsPickerDialog(
            contextOptions = projectOptions,
            attachmentOptions = attachmentOptions,
            preselectedContextIds = projectLinks.toSet(),
            preselectedAttachmentIds = attachmentLinks.toSet(),
            initialTab = initialTab,
            onDismiss = onLinkPickerDismiss,
            onContextSelected = onContextSelected,
            onAttachmentSelected = onAttachmentSelected,
            onCreateRootContext = onCreateRootContext,
            onCreateDocument = onCreateDocument,
        )
    }
}

@Composable
private fun missionFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )

private fun missionStatusLabel(status: MissionStatus): String =
    when (status) {
        MissionStatus.ACTIVE -> "Активна"
        MissionStatus.COMPLETED -> "Виконана"
        MissionStatus.INACTIVE -> "Неактивна"
        MissionStatus.PAUSED -> "Призупинена"
    }

private fun formatDate(ts: Long): String {
    if (ts == NO_DEADLINE) return "Без дедлайну"
    val months =
        listOf(
            "січ",
            "лют",
            "бер",
            "квіт",
            "трав",
            "черв",
            "лип",
            "серп",
            "вер",
            "жовт",
            "лист",
            "груд",
        )
    val calendar = Calendar.getInstance().apply { timeInMillis = ts }
    val monthLabel = months[calendar.get(Calendar.MONTH)]
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val year = calendar.get(Calendar.YEAR)
    return "$day $monthLabel $year"
}
