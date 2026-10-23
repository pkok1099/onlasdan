package com.onlasdan.comic.ui.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.onlasdan.comic.R
import com.onlasdan.comic.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Penjaga proses unduhan berbasis User-Initiated Data Transfer (UIDT,
// Android 14+) menggantikan foreground service dataSync (beta.19): semua
// unduhan memang dipicu pengguna dari UI, jadi job inilah primitif yang
// disediakan platform untuk itu. Logika unduh tetap di
// ChapterDownloadManager; job ini hanya menjaga proses tetap hidup selama
// antrean berjalan dan menampung notifikasi progres lewat setNotification
// (tampil juga sebagai job aktif di Task Manager, bisa dihentikan dari sana).
class DownloadJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var workJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        activeService = this
        activeParams = params
        notifyProgress(null, null, null)
        // Job adalah penjamin eksekusi latar (beta.21, diperbaiki beta.25):
        // ia menggerakkan antrean persisten sampai DB bilang kosong. Bila
        // drainer in-process dari enqueue sedang bekerja, job TIDAK selesai
        // — ia menahan diri tetap aktif (memeriksa ulang tiap detik) agar
        // proses memegang hak eksekusi UIDT selama unduhan berjalan; log
        // beta.24 membuktikan job yang selesai dalam 54ms melepas
        // perlindungan itu dan unduhan latar langsung mati jaringan.
        workJob = serviceScope.launch {
            var drained = ChapterDownloadManager.drainQueue(this@DownloadJobService)
            while (!drained) {
                delay(1000)
                drained = ChapterDownloadManager.drainQueue(this@DownloadJobService)
            }
            val finishedParams = activeParams
            activeParams = null
            activeService = null
            if (finishedParams != null) jobFinished(finishedParams, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        workJob?.cancel()
        activeService = null
        activeParams = null
        // Berhenti karena sistem (jaringan hilang dsb.) -> jadwalkan ulang;
        // sisa antrean aman di DB dan dilanjutkan job berikutnya. Berhenti
        // atas kehendak pengguna di Task Manager -> hormati.
        return params?.stopReason != JobParameters.STOP_REASON_USER
    }

    companion object {
        private const val JOB_ID = 4719
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "downloads"

        @Volatile
        private var activeService: DownloadJobService? = null

        @Volatile
        private var activeParams: JobParameters? = null

        // Dipanggil ChapterDownloadManager setiap enqueue/reconcile. UIDT
        // hanya sah dijadwalkan saat aplikasi terlihat; semua titik enqueue
        // berasal dari UI. Kegagalan jadwal TIDAK ditelan diam-diam —
        // dicatat ke Logcat (pelajaran beta.19-22: SecurityException yang
        // tertelan runCatching membuat job tak pernah lahir selama empat
        // beta tanpa satu pun jejak).
        @Suppress("TooGenericExceptionCaught")
        fun schedule(context: Context) {
            if (activeParams != null) return
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            if (scheduler.allPendingJobs.any { it.id == JOB_ID }) return
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, DownloadJobService::class.java))
                .setUserInitiated(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
            try {
                scheduler.schedule(job)
            } catch (e: Exception) {
                Log.e("DownloadJobService", "gagal menjadwalkan job unduhan", e)
            }
        }

        fun notifyProgress(chapterTitle: String?, page: Int?, totalPages: Int?) {
            val service = activeService ?: return
            val params = activeParams ?: return
            service.setNotification(
                params,
                NOTIFICATION_ID,
                buildNotification(service, chapterTitle, page, totalPages),
                JOB_END_NOTIFICATION_POLICY_REMOVE,
            )
        }

        private fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.download_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
            }
        }

        private fun buildNotification(
            context: Context,
            chapterTitle: String?,
            page: Int?,
            totalPages: Int?,
        ): Notification {
            ensureChannel(context)
            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(context.getString(R.string.download_notification_title))
                .setContentIntent(openApp)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
            if (chapterTitle != null) builder.setContentText(chapterTitle)
            if (page != null && totalPages != null && totalPages > 0) {
                builder.setProgress(totalPages, page, false)
                builder.setSubText(context.getString(R.string.download_notification_page, page, totalPages))
            } else {
                builder.setProgress(0, 0, true)
            }
            return builder.build()
        }
    }
}
