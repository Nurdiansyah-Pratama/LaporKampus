package lat.pam.laporkampus.ui.technician

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import lat.pam.laporkampus.R
import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.model.ReportStatus
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.ui.tracking.SmallInfoChip
import lat.pam.laporkampus.ui.tracking.StatusBadge
import lat.pam.laporkampus.util.DateAndPrivacyUtils
import lat.pam.laporkampus.viewmodel.TechnicianUiState
import java.io.File

@Composable
fun TechnicianTasksScreen(
    currentUser: UserProfile,
    uiState: TechnicianUiState,
    onOpenDetail: (reportId: String) -> Unit,
    onCompleteTask: (report: Report, proofPhotos: List<Uri>, catatan: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var reportToComplete by remember { mutableStateOf<Report?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.tech_tasks_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(id = R.string.tech_tasks_subtitle),
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

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.tasks.isEmpty() -> {
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
                            text = stringResource(id = R.string.tech_empty_tasks),
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
                        items = uiState.tasks,
                        key = { it.id }
                    ) { task ->
                        TechnicianTaskCard(
                            report = task,
                            viewer = currentUser,
                            onViewDetailClick = { onOpenDetail(task.id) },
                            onMarkCompleteClick = { reportToComplete = task }
                        )
                    }
                }
            }
        }
    }

    val selectedTask = reportToComplete
    if (selectedTask != null) {
        TechnicianCompleteDialog(
            report = selectedTask,
            isSubmitting = uiState.isSubmittingCompletion,
            onDismiss = { reportToComplete = null },
            onSubmit = { photos, note ->
                onCompleteTask(selectedTask, photos, note)
                reportToComplete = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TechnicianTaskCard(
    report: Report,
    viewer: UserProfile,
    onViewDetailClick: () -> Unit,
    onMarkCompleteClick: () -> Unit
) {
    val visibleReporter = DateAndPrivacyUtils.getVisibleReporterLabel(
        report = report,
        viewerRole = viewer.roleEnum,
        viewerUserId = viewer.id
    )
    val canMarkComplete = report.statusEnum == ReportStatus.DIVERIFIKASI ||
        report.statusEnum == ReportStatus.DIKERJAKAN

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
                if (canMarkComplete) {
                    Button(
                        onClick = onMarkCompleteClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.tech_btn_mark_complete))
                    }
                }
            }
        }
    }
}

@Composable
private fun TechnicianCompleteDialog(
    report: Report,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (proofPhotos: List<Uri>, catatan: String) -> Unit
) {
    val context = LocalContext.current
    var proofPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var catatan by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && proofPhotoUris.size < 3 && !proofPhotoUris.contains(uri)) {
            proofPhotoUris = proofPhotoUris + uri
            validationError = null
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val captured = pendingCameraUri
        if (success && captured != null && proofPhotoUris.size < 3) {
            proofPhotoUris = proofPhotoUris + captured
            validationError = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val uri = createTechTempImageUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.tech_dialog_complete_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = report.kategori,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.tech_dialog_complete_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = proofPhotoUris.size < 3,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.report_btn_pick_gallery))
                    }

                    OutlinedButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                val uri = createTechTempImageUri(context)
                                pendingCameraUri = uri
                                takePictureLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        enabled = proofPhotoUris.size < 3,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.report_btn_take_photo))
                    }
                }

                if (proofPhotoUris.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(proofPhotoUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        proofPhotoUris = proofPhotoUris.filterNot { it == uri }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(24.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = catatan,
                    onValueChange = {
                        catatan = it
                        validationError = null
                    },
                    label = { Text(stringResource(id = R.string.tech_label_completion_note)) },
                    placeholder = { Text(stringResource(id = R.string.tech_hint_completion_note)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        proofPhotoUris.isEmpty() -> {
                            validationError = "Wajib melampirkan minimal 1 foto bukti hasil perbaikan."
                        }
                        catatan.trim().isEmpty() -> {
                            validationError = "Catatan penyelesaian pekerjaan wajib diisi."
                        }
                        else -> {
                            onSubmit(proofPhotoUris, catatan)
                        }
                    }
                },
                enabled = !isSubmitting
            ) {
                Text(text = stringResource(id = R.string.tech_btn_submit_completion))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.admin_btn_cancel))
            }
        }
    )
}

private fun createTechTempImageUri(context: Context): Uri {
    val captureDir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
    val tempFile = File.createTempFile("bukti_teknisi_", ".jpg", captureDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}
