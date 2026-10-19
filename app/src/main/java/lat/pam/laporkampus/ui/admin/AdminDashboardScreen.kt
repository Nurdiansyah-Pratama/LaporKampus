package lat.pam.laporkampus.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lat.pam.laporkampus.R
import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.model.ReportPriority
import lat.pam.laporkampus.data.model.ReportStatus
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.ui.tracking.SmallInfoChip
import lat.pam.laporkampus.ui.tracking.StatusBadge
import lat.pam.laporkampus.util.DateAndPrivacyUtils
import lat.pam.laporkampus.util.StatusTransitionRules
import lat.pam.laporkampus.viewmodel.AdminUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    currentUser: UserProfile,
    uiState: AdminUiState,
    onSelectStatusFilter: (ReportStatus?) -> Unit,
    onOpenDetail: (reportId: String) -> Unit,
    onUpdateReport: (
        report: Report,
        newStatus: ReportStatus,
        newPriority: ReportPriority,
        assignedTo: String,
        catatan: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var reportToManage by remember { mutableStateOf<Report?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.admin_dashboard_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(id = R.string.admin_dashboard_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (uiState.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Hitungan sederhana (jumlah per status) di layar Admin
        Text(
            text = stringResource(id = R.string.admin_stats_section_title),
            style = MaterialTheme.typography.titleLarge
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = uiState.selectedStatusFilter == null,
                onClick = { onSelectStatusFilter(null) },
                label = {
                    Text(
                        text = stringResource(
                            id = R.string.admin_filter_all,
                            uiState.allReports.size
                        )
                    )
                }
            )
            ReportStatus.entries.forEach { status ->
                val count = uiState.statusCounts[status] ?: 0
                FilterChip(
                    selected = uiState.selectedStatusFilter == status,
                    onClick = { onSelectStatusFilter(status) },
                    label = {
                        Text(text = "${status.label} ($count)")
                    }
                )
            }
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.filteredReports.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.admin_empty_reports),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.filteredReports,
                        key = { it.id }
                    ) { report ->
                        AdminReportItemCard(
                            report = report,
                            viewer = currentUser,
                            onManageClick = { reportToManage = report },
                            onViewDetailClick = { onOpenDetail(report.id) }
                        )
                    }
                }
            }
        }
    }

    val selectedReport = reportToManage
    if (selectedReport != null) {
        AdminManageReportDialog(
            report = selectedReport,
            technicians = uiState.technicians,
            isUpdating = uiState.isUpdating,
            onDismiss = { reportToManage = null },
            onSave = { newStatus, newPriority, assignedTo, catatan ->
                onUpdateReport(selectedReport, newStatus, newPriority, assignedTo, catatan)
                reportToManage = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminReportItemCard(
    report: Report,
    viewer: UserProfile,
    onManageClick: () -> Unit,
    onViewDetailClick: () -> Unit
) {
    val visibleReporter = DateAndPrivacyUtils.getVisibleReporterLabel(
        report = report,
        viewerRole = viewer.roleEnum,
        viewerUserId = viewer.id
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.kategori,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = report.statusEnum)
            }

            Text(
                text = report.deskripsi,
                style = MaterialTheme.typography.bodyMedium
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallInfoChip(
                    text = stringResource(
                        id = R.string.label_priority_prefix,
                        report.priorityEnum.label
                    ),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (report.isAnonymous) {
                    SmallInfoChip(
                        text = stringResource(id = R.string.badge_anonymous),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.label_location_prefix, report.namaLokasi),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(id = R.string.detail_reporter_id_prefix, visibleReporter),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (report.assignedTo.isNotBlank()) {
                    stringResource(id = R.string.detail_assigned_to_prefix, report.assignedTo)
                } else {
                    stringResource(id = R.string.detail_not_assigned)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetailClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.admin_btn_view_detail))
                }
                Button(
                    onClick = onManageClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.admin_btn_manage_report))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AdminManageReportDialog(
    report: Report,
    technicians: List<UserProfile>,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        newStatus: ReportStatus,
        newPriority: ReportPriority,
        assignedTo: String,
        catatan: String
    ) -> Unit
) {
    val currentStatus = report.statusEnum
    val validNextStatuses = remember(currentStatus) {
        StatusTransitionRules.nextValidStatuses(currentStatus).toList()
    }
    var selectedStatus by remember(currentStatus) {
        mutableStateOf(validNextStatuses.firstOrNull() ?: currentStatus)
    }
    var selectedPriority by remember(report.prioritas) {
        mutableStateOf(report.priorityEnum)
    }
    var assignedToText by remember(report.assignedTo) {
        mutableStateOf(report.assignedTo)
    }
    var catatan by remember { mutableStateOf("") }
    var noteError by remember { mutableStateOf<String?>(null) }

    var priorityDropdownExpanded by remember { mutableStateOf(false) }
    var techDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.admin_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.admin_label_current_status, currentStatus.label),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                // Pilihan transisi status yang valid saja (dari StatusTransitionRules)
                Text(
                    text = stringResource(id = R.string.admin_label_next_status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (validNextStatuses.isEmpty()) {
                    Text(
                        text = stringResource(
                            id = R.string.admin_no_valid_transition,
                            currentStatus.label
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Opsi tetap pada status saat ini (mis. hanya ubah prioritas/teknisi) + status transisi valid
                        val options = listOf(currentStatus) + validNextStatuses
                        options.distinct().forEach { statusOption ->
                            FilterChip(
                                selected = selectedStatus == statusOption,
                                onClick = { selectedStatus = statusOption },
                                label = { Text(statusOption.label) }
                            )
                        }
                    }
                }

                // Pilihan Prioritas Laporan
                ExposedDropdownMenuBox(
                    expanded = priorityDropdownExpanded,
                    onExpandedChange = { priorityDropdownExpanded = !priorityDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPriority.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.admin_label_priority)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = priorityDropdownExpanded,
                        onDismissRequest = { priorityDropdownExpanded = false }
                    ) {
                        ReportPriority.entries.forEach { prio ->
                            DropdownMenuItem(
                                text = { Text(prio.label) },
                                onClick = {
                                    selectedPriority = prio
                                    priorityDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Penugasan Teknisi (Bisa pilih dari daftar akun TEKNISI di Firestore atau ketik manual)
                ExposedDropdownMenuBox(
                    expanded = techDropdownExpanded && technicians.isNotEmpty(),
                    onExpandedChange = {
                        if (technicians.isNotEmpty()) {
                            techDropdownExpanded = !techDropdownExpanded
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = assignedToText,
                        onValueChange = { assignedToText = it },
                        label = { Text(stringResource(id = R.string.admin_label_assign_tech)) },
                        placeholder = { Text(stringResource(id = R.string.admin_hint_assign_tech)) },
                        trailingIcon = {
                            if (technicians.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = techDropdownExpanded)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )
                    if (technicians.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = techDropdownExpanded,
                            onDismissRequest = { techDropdownExpanded = false }
                        ) {
                            technicians.forEach { tech ->
                                val formatted = "${tech.nama} (${tech.id})"
                                DropdownMenuItem(
                                    text = { Text(formatted) },
                                    onClick = {
                                        assignedToText = formatted
                                        techDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Catatan Perubahan Status
                OutlinedTextField(
                    value = catatan,
                    onValueChange = {
                        catatan = it
                        noteError = null
                    },
                    label = { Text(stringResource(id = R.string.admin_label_history_note)) },
                    placeholder = { Text(stringResource(id = R.string.admin_hint_history_note)) },
                    minLines = 3,
                    isError = noteError != null,
                    supportingText = {
                        noteError?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (catatan.trim().isEmpty()) {
                        noteError = "Catatan perubahan status wajib diisi."
                    } else {
                        onSave(selectedStatus, selectedPriority, assignedToText, catatan)
                    }
                },
                enabled = !isUpdating
            ) {
                Text(text = stringResource(id = R.string.admin_btn_save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.admin_btn_cancel))
            }
        }
    )
}
