package lat.pam.laporkampus.ui.tracking

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import lat.pam.laporkampus.R
import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.model.ReportHistory
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.util.DateAndPrivacyUtils
import lat.pam.laporkampus.viewmodel.ReportDetailUiState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportDetailScreen(
    currentUser: UserProfile,
    uiState: ReportDetailUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.detail_btn_back)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(id = R.string.detail_btn_back))
            }
            Text(
                text = stringResource(id = R.string.detail_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
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
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            uiState.report == null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.error_report_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {
                val report: Report = uiState.report

                // Kartu Informasi Utama Laporan
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatusBadge(status = report.statusEnum)
                        }

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
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (report.isSensitive) {
                                SmallInfoChip(
                                    text = stringResource(id = R.string.badge_sensitive),
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = report.deskripsi,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        HorizontalDivider()

                        val visibleReporter = DateAndPrivacyUtils.getVisibleReporterLabel(
                            report = report,
                            viewerRole = currentUser.roleEnum,
                            viewerUserId = currentUser.id
                        )
                        Text(
                            text = stringResource(
                                id = R.string.detail_reporter_id_prefix,
                                visibleReporter
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (report.assignedTo.isNotBlank()) {
                                stringResource(
                                    id = R.string.detail_assigned_to_prefix,
                                    report.assignedTo
                                )
                            } else {
                                stringResource(id = R.string.detail_not_assigned)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = stringResource(
                                id = R.string.label_created_at_prefix,
                                DateAndPrivacyUtils.formatTimestamp(report.createdAt)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bagian Foto Bukti Laporan (ditampilkan dengan Coil)
                if (report.photoUrls.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.detail_section_photos),
                        style = MaterialTheme.typography.titleLarge
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(report.photoUrls) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }

                // Bagian Lokasi Kejadian & Pratinjau Peta OpenStreetMap
                Text(
                    text = stringResource(id = R.string.detail_section_location),
                    style = MaterialTheme.typography.titleLarge
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = report.namaLokasi,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (report.lat != null && report.lng != null) {
                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    stringResource(id = R.string.report_coordinates_format),
                                    report.lat,
                                    report.lng
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OsmReadOnlyPreview(
                                lat = report.lat,
                                lng = report.lng,
                                title = report.namaLokasi
                            )
                        }
                    }
                }

                // Bagian Timeline Status dari subkoleksi `reports/{id}/history`
                Text(
                    text = stringResource(id = R.string.detail_section_timeline),
                    style = MaterialTheme.typography.titleLarge
                )

                if (uiState.history.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.detail_timeline_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        uiState.history.forEachIndexed { index, item ->
                            TimelineHistoryItem(
                                history = item,
                                isLast = index == uiState.history.lastIndex
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineHistoryItem(
    history: ReportHistory,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp, end = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(72.dp)
                        .background(color = MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status = history.statusEnum)
                    Text(
                        text = DateAndPrivacyUtils.formatTimestamp(history.waktu),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (history.catatan.isNotBlank()) {
                    Text(
                        text = history.catatan,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (history.oleh.isNotBlank()) {
                    Text(
                        text = stringResource(id = R.string.detail_timeline_by_prefix, history.oleh),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun OsmReadOnlyPreview(
    lat: Double,
    lng: Double,
    title: String
) {
    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.5)
            controller.setCenter(GeoPoint(lat, lng))
        }
    }

    val marker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            mapView.overlays.add(this)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        update = { view ->
            val point = GeoPoint(lat, lng)
            marker.position = point
            view.controller.setCenter(point)
            view.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}
