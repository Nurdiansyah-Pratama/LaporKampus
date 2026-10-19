package lat.pam.laporkampus.ui.report

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/**
 * Komponen pemilih lokasi berbasis OpenStreetMap (`osmdroid-android`).
 * Pengguna dapat mengetuk peta untuk memindahkan pin koordinat lokasi laporan.
 */
@Composable
fun OsmLocationPicker(
    selectedLat: Double,
    selectedLng: Double,
    onLocationSelected: (lat: Double, lng: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.5)
            controller.setCenter(GeoPoint(selectedLat, selectedLng))
        }
    }

    val marker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Titik Lokasi Laporan"
            mapView.overlays.add(this)
        }
    }

    DisposableEffect(mapView) {
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    onLocationSelected(p.latitude, p.longitude)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    onLocationSelected(p.latitude, p.longitude)
                }
                return true
            }
        })
        mapView.overlays.add(0, eventsOverlay)
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        update = { view ->
            val targetPoint = GeoPoint(selectedLat, selectedLng)
            marker.position = targetPoint
            view.controller.animateTo(targetPoint)
            view.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}
