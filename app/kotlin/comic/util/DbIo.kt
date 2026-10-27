package comic.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal object DbIo {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun launch(context: Context, block: suspend (Context) -> Unit) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching { block(appContext) }
        }
    }
}
