package lat.pam.laporkampus.data.firebase

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import lat.pam.laporkampus.MainActivity
import lat.pam.laporkampus.R

/**
 * Layanan penerima pesan Firebase Cloud Messaging (FCM) ketika backend / Cloud Functions
 * mengirimkan push notification saat status laporan berubah.
 */
class LaporKampusMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        StatusNotificationHelper.syncFcmTokenToCurrentUser(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        StatusNotificationHelper.showSystemNotification(
            context = applicationContext,
            notificationId = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt(),
            title = title,
            body = body
        )
    }
}

/**
 * Helper untuk membuat NotificationChannel Android, menyinkronkan token FCM pengguna ke Firestore,
 * serta menampilkan notifikasi sistem saat status laporan berubah (baik dari push FCM maupun dari
 * pemantau real-time Firestore di sisi klien sebagai alternatif tanpa server berbayar).
 */
object StatusNotificationHelper {

    const val CHANNEL_ID = "lapor_kampus_status_channel"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun refreshAndSaveFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (!token.isNullOrBlank()) {
                FirebaseFirestore.getInstance()
                    .collection(FirestoreCollections.USERS)
                    .document(uid)
                    .update("fcmToken", token)
            }
        }
    }

    fun syncFcmTokenToCurrentUser(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (token.isNotBlank()) {
            FirebaseFirestore.getInstance()
                .collection(FirestoreCollections.USERS)
                .document(uid)
                .update("fcmToken", token)
        }
    }

    fun showSystemNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        ensureNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
