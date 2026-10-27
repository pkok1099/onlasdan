package comic

import android.app.Application
import android.content.pm.PackageInfo
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import comic.download.ChapterDownloadManager
import comic.util.ImageIntegrityDecoderFactory
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@HiltAndroidApp
class ComicApp :
    Application(),
    ImageLoaderFactory {
    companion object {
        lateinit var instance: ComicApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashLogger.install(this)
        ChapterDownloadManager.reconcileStuckDownloads(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(ImageIntegrityDecoderFactory()) }
        .respectCacheHeaders(false)
        .build()
}

object CrashLogger {
    private val installed = AtomicBoolean(false)

    fun install(app: Application) {
        if (!installed.compareAndSet(false, true)) return

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashLog(app, thread, throwable)
            } catch (_: Exception) {
                // Avoid masking the original crash if logging fails.
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun writeCrashLog(app: Application, thread: Thread, throwable: Throwable) {
        // External app dir (/storage/emulated/0/Android/data/<pkg>/files) so the user
        // can open it with a file manager. Falls back to internal filesDir.
        val baseDir = app.getExternalFilesDir(null) ?: app.filesDir
        val logDir = File(baseDir, "crash_logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val logFile = File(logDir, "crash-$ts.log")

        logFile.bufferedWriter().use { out ->
            out.appendLine("crash_time=$ts")
            out.appendLine("thread=${thread.name}")
            out.appendLine("android_sdk=${Build.VERSION.SDK_INT}")
            out.appendLine("android_release=${Build.VERSION.RELEASE}")
            out.appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
            val pkgInfo = getPackageInfoSafe(app)
            if (pkgInfo != null) {
                out.appendLine("app_version_name=${pkgInfo.versionName}")
                out.appendLine("app_version_code=${pkgInfo.longVersionCode}")
            }
            out.appendLine("stacktrace:")
            out.appendLine(getStackTrace(throwable))
        }
    }

    private fun getPackageInfoSafe(app: Application): PackageInfo? = try {
        app.packageManager.getPackageInfo(
            app.packageName,
            android.content.pm.PackageManager.PackageInfoFlags.of(0),
        )
    } catch (_: Exception) {
        null
    }

    private fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
}
