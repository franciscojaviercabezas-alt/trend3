package es.tendencias.app.data

import android.content.Context
import android.content.SharedPreferences
import es.tendencias.app.model.Trend

class TrendsCache(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "trends_cache",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_TRENDS_JSON = "trends_json"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_LAST_ERROR = "last_error"
    }

    fun saveTrends(trends: List<Trend>) {
        val json = TrendsJson.serializeTrends(trends)
        prefs.edit().putString(KEY_TRENDS_JSON, json).apply()
    }

    fun loadTrends(): List<Trend>? {
        val json = prefs.getString(KEY_TRENDS_JSON, null) ?: return null
        if (json.isBlank()) return null
        val trends = TrendsJson.deserializeTrends(json)
        return trends.ifEmpty { null }
    }

    fun saveLastUpdate(epochMillis: Long) {
        prefs.edit().putLong(KEY_LAST_UPDATE, epochMillis).apply()
    }

    fun loadLastUpdate(): Long? {
        return if (prefs.contains(KEY_LAST_UPDATE)) {
            prefs.getLong(KEY_LAST_UPDATE, 0L)
        } else {
            null
        }
    }

    fun saveLastError(message: String?) {
        if (message != null) {
            prefs.edit().putString(KEY_LAST_ERROR, message).apply()
        } else {
            clearError()
        }
    }

    fun loadLastError(): String? {
        return prefs.getString(KEY_LAST_ERROR, null)
    }

    fun clearError() {
        prefs.edit().remove(KEY_LAST_ERROR).apply()
    }
}
