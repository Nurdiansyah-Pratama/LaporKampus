package lat.pam.laporkampus.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RateReview
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        // Banner Khusus Portal Petugas (Tampilan Berbeda dari Layar Pelapor)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = stringResource(id = R.string.admin_portal_badge),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = stringResource(id = R.string.admin_dashboard_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = stringResource(id = R.string.admin_dashboard_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 3 Indikator Progres Utama di Header Portal Petugas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StaffStatPill(
                        label = ReportStatus.DIVERIFIKASI.label,
                        count = uiState.statusCounts[ReportStatus.DIVERIFIKASI] ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                    StaffStatPill(
                        label = ReportStatus.DIKERJAKAN.label,
                        count = uiState.statusCounts[ReportStatus.DIKERJAKAN] ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                    StaffStatPill(
                        label = ReportStatus.SELESAI.label,
                        count = uiState.statusCounts[ReportStatus.SELESAI] ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

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

        // Filter Status Laporan
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                            isUpdating = uiState.isUpdating,
                            onQuickProgressChange = { targetStatus ->
                                val autoNote = when (targetStatus) {
                                    ReportStatus.DIVERIFIKASI ->
                                        "Laporan sedang ditinjau oleh petugas (${currentUser.nama})."
                                    ReportStatus.DIKERJAKAN ->
                                        "Laporan sedang diproses / ditindaklanjuti oleh petugas."
                                    ReportStatus.SELESAI ->
                                        "Penanganan laporan telah selesai dilaksanakan."
                                    else ->
                                        "Status laporan diperbarui oleh petugas."
                                }
                                onUpdateReport(
                                    report,
                                    targetStatus,
                                    report.priorityEnum,
                                    report.assignedTo.ifBlank { currentUser.nama },
                                    autoNote
                                )
                            },
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

@Composable
private fun StaffStatPill(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminReportItemCard(
    report: Report,
    viewer: UserProfile,
    isUpdating: Boolean,
    onQuickProgressChange: (ReportStatus) -> Unit,
    onManageClick: () -> Unit,
    onViewDetailClick: () -> Unit
) {
    val visibleReporter = DateAndPrivacyUtils.getVisibleReporterLabel(
        report = report,
        viewerRole = viewer.roleEnum,
        viewerUserId = viewer.id
    )
    val currentStatus = report.statusEnum
    val progressValue = when (currentStatus) {
        ReportStatus.DITERIMA -> 0.15f
        ReportStatus.DIVERIFIKASI -> 0.45f
        ReportStatus.DIKERJAKAN -> 0.75f
        ReportStatus.SELESAI -> 1.0f
        ReportStatus.DITOLAK -> 1.0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                StatusBadge(status = currentStatus)
            }

            // Bar Progres Visual Penanganan Laporan
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

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

            HorizontalDivider()

            // Panel Ubah Progres 1-Klik (Sedang Ditinjau -> Sedang Diproses -> Sudah Selesai)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.admin_quick_progress_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = currentStatus == ReportStatus.DIVERIFIKASI,
                        onClick = {
                            if (currentStatus != ReportStatus.DIVERIFIKASI) {
                                onQuickProgressChange(ReportStatus.DIVERIFIKASI)
                            }
                        },
                        enabled = !isUpdating,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.RateReview,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        label = { Text(stringResource(id = R.string.admin_step_reviewing)) }
                    )

                    FilterChip(
                        selected = currentStatus == ReportStatus.DIKERJAKAN,
                        onClick = {
                            if (currentStatus != ReportStatus.DIKERJAKAN) {
                                onQuickProgressChange(ReportStatus.DIKERJAKAN)
                            }
                        },
                        enabled = !isUpdating,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        label = { Text(stringResource(id = R.string.admin_step_processing)) }
                    )

                    FilterChip(
                        selected = currentStatus == ReportStatus.SELESAI,
                        onClick = {
                            if (currentStatus != ReportStatus.SELESAI) {
                                onQuickProgressChange(ReportStatus.SELESAI)
                            }
                        },
                        enabled = !isUpdating,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        label = { Text(stringResource(id = R.string.admin_step_completed)) }
                    )
                }
            }

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
    val selectableStatuses = remember {
        listOf(
            ReportStatus.DIVERIFIKASI, // Sedang Ditinjau
            ReportStatus.DIKERJAKAN,   // Sedang Diproses
            ReportStatus.SELESAI,      // Sudah Selesai
            ReportStatus.DITOLAK       // Ditolak
        )
    }
    var selectedStatus by remember(currentStatus) {
        mutableStateOf(
            when (currentStatus) {
                ReportStatus.DITERIMA -> ReportStatus.DIVERIFIKASI
                ReportStatus.DIVERIFIKASI -> ReportStatus.DIKERJAKAN
                ReportStatus.DIKERJAKAN -> ReportStatus.SELESAI
                else -> currentStatus
            }
        )
    }
    var selectedPriority by remember(report.prioritas) {
        mutableStateOf(report.priorityEnum)
    }
    var assignedToText by remember(report.assignedTo) {
        mutableStateOf(report.assignedTo)
    }
    var catatan by remember { mutableStateOf("") }

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

                Text(
                    text = stringResource(id = R.string.admin_label_next_status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    selectableStatuses.forEach { statusOption ->
                        FilterChip(
                            selected = selectedStatus == statusOption,
                            onClick = { selectedStatus = statusOption },
                            label = { Text(statusOption.label) }
                        )
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

                // Penugasan Teknisi / Petugas
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

                // Catatan Progres Status (Jika dikosongkan akan terisi otomatis sesuai progres)
                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text(stringResource(id = R.string.admin_label_history_note)) },
                    placeholder = { Text(stringResource(id = R.string.admin_hint_history_note)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedStatus, selectedPriority, assignedToText, catatan)
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
