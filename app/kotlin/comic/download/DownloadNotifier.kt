package comic.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import comic.MainActivity
import comic.R

/**
 * Notifikasi hasil akhir unduhan chapter (backlog unduhan #1): selesai,
 * gagal, dibatalkan. Terpisah dari notifikasi progres foreground service —
 * channel sendiri dengan prioritas normal agar hasil akhir terlihat.
 * Tanpa izin POST_NOTIFICATIONS, notify() diam-diam tidak tampil.
 */
object DownloadNotifier {
    private const val CHANNEL_ID = "download_results"
    private const val ID_BASE = 2000

    private fun notificationId(source: String, comicSlug: String, chapterSlug: String): Int = ID_BASE + ("$source|$comicSlug|$chapterSlug".hashCode() and 0x3FFF)

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.download_results_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    private fun post(
        context: Context,
        task: DownloadTaskInfo,
        text: String,
        icon: Int,
    ) {
        ensureChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(task.comicTitle)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(notificationId(task.source, task.comicSlug, task.chapterSlug), notification)
    }

    fun notifyCompleted(context: Context, task: DownloadTaskInfo) {
        post(context, task, context.getString(R.string.download_completed_text, task.chapterTitle), android.R.drawable.stat_sys_download_done)
    }

    fun notifyFailed(context: Context, task: DownloadTaskInfo) {
        post(context, task, context.getString(R.string.download_failed_text, task.chapterTitle), android.R.drawable.stat_notify_error)
    }

    fun notifyCancelled(context: Context, task: DownloadTaskInfo) {
        post(context, task, context.getString(R.string.download_cancelled_text, task.chapterTitle), android.R.drawable.stat_sys_download)
    }
}

/** Info minimal sebuah tugas unduhan untuk keperluan notifikasi hasil. */
data class DownloadTaskInfo(
    val source: String,
    val comicSlug: String,
    val chapterSlug: String,
    val comicTitle: String,
    val chapterTitle: String,
)
