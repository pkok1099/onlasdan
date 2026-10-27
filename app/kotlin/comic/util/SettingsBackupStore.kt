package comic.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import comic.source.SourcePreferences

/**
 * Snapshot JSON untuk pengaturan (dipisah dari DatabaseBackupStore, bug B17):
 * SharedPreferences comic_prefs + nilai DataStore (ui_prefs, source_prefs)
 * dalam satu file settings_backup.json. Nilai DataStore hidup di bagian
 * "__datastore__" dengan kunci berawalan "ui."/"src."; pemilik kunci tetap
 * object preferensi masing-masing (daftar putih saat impor).
 */
object SettingsBackupStore {

    private val SETTINGS_PAIR_REGEX =
        Regex(""""([^"\\]*(?:\\.[^"\\]*)*)"\s*:\s*("(?:[^"\\]*(?:\\.[^"\\]*)*)"|true|false|-?\d+(?:\.\d+)?)""")

    suspend fun collectDataStoreValues(context: Context): Map<String, String> {
        val values = LinkedHashMap<String, String>()
        UiPreferences.exportValues(context).forEach { (key, value) -> values["ui.$key"] = value }
        SourcePreferences.exportValues(context).forEach { (key, value) -> values["src.$key"] = value }
        return values
    }

    suspend fun applyDataStoreValues(context: Context, values: Map<String, String>) {
        if (values.isEmpty()) return
        UiPreferences.importValues(context, values.filterKeys { it.startsWith("ui.") }.mapKeys { it.key.removePrefix("ui.") })
        SourcePreferences.importValues(
            context,
            values.filterKeys { it.startsWith("src.") }.mapKeys { it.key.removePrefix("src.") },
        )
    }

    fun exportSettingsToJson(context: Context, datastoreValues: Map<String, String> = emptyMap()): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allEntries = prefs.all
        val sb = StringBuilder("{\n")
        var first = true
        for ((key, value) in allEntries) {
            if (key == KEY_TREE_URI) continue
            if (!first) sb.append(",\n")
            first = false
            sb.append("  \"").append(escapeJson(key)).append("\": ")
            when (value) {
                is Boolean, is Number -> sb.append(value)
                else -> sb.append("\"").append(escapeJson(value.toString())).append("\"")
            }
        }
        if (datastoreValues.isNotEmpty()) {
            if (!first) sb.append(",\n")
            sb.append("  \"__datastore__\": {")
            sb.append(
                datastoreValues.entries.joinToString(", ") { (key, value) ->
                    "\"${escapeJson(key)}\": \"${escapeJson(value)}\""
                },
            )
            sb.append("}")
        }
        sb.append("\n}")
        return sb.toString()
    }

    // Mengembalikan nilai DataStore yang terbaca dari bagian "__datastore__"
    // agar pemanggil suspend menerapkannya; backup lama tanpa bagian itu
    // mengembalikan peta kosong. Pasangan berkunci awalan "ui."/"src." tidak
    // ditulis ke SharedPreferences.
    fun restoreSettingsFromJson(context: Context, jsonStr: String): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            for (match in SETTINGS_PAIR_REGEX.findAll(jsonStr)) {
                val key = unescapeJson(match.groupValues[1])
                if (key == KEY_TREE_URI) continue
                if (key.startsWith("ui.") || key.startsWith("src.")) continue
                putSettingsPair(key, match.groupValues[2])
            }
        }
        return parseDataStoreSection(jsonStr)
    }

    private fun SharedPreferences.Editor.putSettingsPair(key: String, rawValue: String) {
        when {
            rawValue == "true" -> putBoolean(key, true)

            rawValue == "false" -> putBoolean(key, false)

            rawValue.startsWith("\"") && rawValue.endsWith("\"") -> {
                putString(key, unescapeJson(rawValue.substring(1, rawValue.length - 1)))
            }

            rawValue.contains(".") -> rawValue.toFloatOrNull()?.let { putFloat(key, it) }

            else -> rawValue.toLongOrNull()?.let {
                if (it in Int.MIN_VALUE..Int.MAX_VALUE) putInt(key, it.toInt()) else putLong(key, it)
            }
        }
    }

    private fun parseDataStoreSection(jsonStr: String): Map<String, String> {
        val range = dataStoreSectionRange(jsonStr) ?: return emptyMap()
        return SETTINGS_PAIR_REGEX.findAll(jsonStr.substring(range.first, range.last + 1)).mapNotNull { match ->
            val rawValue = match.groupValues[2]
            if (rawValue.startsWith("\"")) {
                unescapeJson(match.groupValues[1]) to unescapeJson(rawValue.substring(1, rawValue.length - 1))
            } else {
                null
            }
        }.toMap()
    }

    // Rentang kurung bagian "__datastore__". Pencocokan mengabaikan kurung di
    // dalam string — aman untuk nilai yang ditulis exporter sendiri (nama
    // enum, id source).
    private fun dataStoreSectionRange(jsonStr: String): IntRange? {
        val marker = jsonStr.indexOf("\"__datastore__\"")
        if (marker < 0) return null
        val start = jsonStr.indexOf('{', marker)
        if (start < 0) return null
        var depth = 0
        for (i in start until jsonStr.length) {
            when (jsonStr[i]) {
                '{' -> depth++

                '}' -> {
                    depth--
                    if (depth == 0) return start..i
                }
            }
        }
        return null
    }

    private fun escapeJson(str: String): String = str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun unescapeJson(str: String): String = str.replace("\\\"", "\"")
        .replace("\\\\", "\\")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
}
