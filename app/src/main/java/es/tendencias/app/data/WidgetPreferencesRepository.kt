package es.tendencias.app.data

import android.content.Context
import android.content.SharedPreferences

enum class TextFormat { FULL, SHORT_PHRASE, ONE_WORD, TWO_WORDS, ONE_LINE }
enum class ImageMode { NONE, IF_AVAILABLE, IMAGE_AND_TEXT }
enum class Density { COMPACT, NORMAL, WIDE, AUTO }
enum class Background { TRANSPARENT, LIGHT, DARK, SYSTEM }
enum class TapAction { OPEN_TRENDS, OPEN_LINK, SEARCH_WEB, NONE }
enum class UpdateFrequency { MANUAL, MIN_15, MIN_30, ONE_HOUR, TWO_HOURS }

class WidgetPreferencesRepository(context: Context) {

    data class WidgetConfig(
        val numberOfTrends: Int = AUTO,             // AUTO (-1) o 1..10
        val textFormat: TextFormat = TextFormat.FULL,
        val showRanking: Boolean = true,
        val imageMode: ImageMode = ImageMode.NONE,
        val density: Density = Density.NORMAL,
        val background: Background = Background.SYSTEM,
        val showLastUpdate: Boolean = true,
        val tapAction: TapAction = TapAction.OPEN_TRENDS,
        val showManualRefresh: Boolean = true,
        val updateFrequency: UpdateFrequency = UpdateFrequency.ONE_HOUR,
        val showVolume: Boolean = false,
        val showPubDate: Boolean = false
    )

    companion object {
        const val AUTO = -1
        private const val PREFS_NAME = "widget_prefs"
        private const val KEY_PREFIX = "widget_"
        private const val KEY_NUMBER_OF_TRENDS = "_number_of_trends"
        private const val KEY_TEXT_FORMAT = "_text_format"
        private const val KEY_SHOW_RANKING = "_show_ranking"
        private const val KEY_IMAGE_MODE = "_image_mode"
        private const val KEY_DENSITY = "_density"
        private const val KEY_BACKGROUND = "_background"
        private const val KEY_SHOW_LAST_UPDATE = "_show_last_update"
        private const val KEY_TAP_ACTION = "_tap_action"
        private const val KEY_SHOW_MANUAL_REFRESH = "_show_manual_refresh"
        private const val KEY_UPDATE_FREQUENCY = "_update_frequency"
        private const val KEY_SHOW_VOLUME = "_show_volume"
        private const val KEY_SHOW_PUB_DATE = "_show_pub_date"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveConfig(appWidgetId: Int, config: WidgetConfig) {
        prefs.edit().apply {
            putInt("${KEY_PREFIX}${appWidgetId}${KEY_NUMBER_OF_TRENDS}", config.numberOfTrends)
            putString("${KEY_PREFIX}${appWidgetId}${KEY_TEXT_FORMAT}", config.textFormat.name)
            putBoolean("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_RANKING}", config.showRanking)
            putString("${KEY_PREFIX}${appWidgetId}${KEY_IMAGE_MODE}", config.imageMode.name)
            putString("${KEY_PREFIX}${appWidgetId}${KEY_DENSITY}", config.density.name)
            putString("${KEY_PREFIX}${appWidgetId}${KEY_BACKGROUND}", config.background.name)
            putBoolean("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_LAST_UPDATE}", config.showLastUpdate)
            putString("${KEY_PREFIX}${appWidgetId}${KEY_TAP_ACTION}", config.tapAction.name)
            putBoolean("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_MANUAL_REFRESH}", config.showManualRefresh)
            putString("${KEY_PREFIX}${appWidgetId}${KEY_UPDATE_FREQUENCY}", config.updateFrequency.name)
            putBoolean("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_VOLUME}", config.showVolume)
            putBoolean("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_PUB_DATE}", config.showPubDate)
            apply()
        }
    }

    fun loadConfig(appWidgetId: Int): WidgetConfig {
        val numTrends = prefs.getInt("${KEY_PREFIX}${appWidgetId}${KEY_NUMBER_OF_TRENDS}", AUTO)
        val textFormat = try {
            TextFormat.valueOf(prefs.getString("${KEY_PREFIX}${appWidgetId}${KEY_TEXT_FORMAT}", TextFormat.FULL.name) ?: TextFormat.FULL.name)
        } catch (_: Exception) { TextFormat.FULL }

        val showRanking = prefs.getBoolean("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_RANKING}", true)

        val imageMode = try {
            ImageMode.valueOf(prefs.getString("${KEY_PREFIX}${appWidgetId}${KEY_IMAGE_MODE}", ImageMode.NONE.name) ?: ImageMode.NONE.name)
        } catch (_: Exception) { ImageMode.NONE }

        val density = try {
            Density.valueOf(prefs.getString("${KEY_PREFIX}${appWidgetId}${KEY_DENSITY}", Density.NORMAL.name) ?: Density.NORMAL.name)
        } catch (_: Exception) { Density.NORMAL }

        val background = try {
            Background.valueOf(prefs.getString("${KEY_PREFIX}${appWidgetId}${KEY_BACKGROUND}", Background.SYSTEM.name) ?: Background.SYSTEM.name)
        } catch (_: Exception) { Background.SYSTEM }

        val showLastUpdate = prefs.getBoolean("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_LAST_UPDATE}", true)

        val tapAction = try {
            TapAction.valueOf(prefs.getString("${KEY_PREFIX}${appWidgetId}${KEY_TAP_ACTION}", TapAction.OPEN_TRENDS.name) ?: TapAction.OPEN_TRENDS.name)
        } catch (_: Exception) { TapAction.OPEN_TRENDS }

        val showManualRefresh = prefs.getBoolean("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_MANUAL_REFRESH}", true)

        val updateFrequency = try {
            UpdateFrequency.valueOf(prefs.getString("${KEY_PREFIX}${appWidgetId}${KEY_UPDATE_FREQUENCY}", UpdateFrequency.ONE_HOUR.name) ?: UpdateFrequency.ONE_HOUR.name)
        } catch (_: Exception) { UpdateFrequency.ONE_HOUR }

        val showVolume = prefs.getBoolean("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_VOLUME}", false)
        val showPubDate = prefs.getBoolean("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_PUB_DATE}", false)

        return WidgetConfig(
            numberOfTrends = numTrends,
            textFormat = textFormat,
            showRanking = showRanking,
            imageMode = imageMode,
            density = density,
            background = background,
            showLastUpdate = showLastUpdate,
            tapAction = tapAction,
            showManualRefresh = showManualRefresh,
            updateFrequency = updateFrequency,
            showVolume = showVolume,
            showPubDate = showPubDate
        )
    }

    fun deleteConfig(appWidgetId: Int) {
        prefs.edit().apply {
            remove("${KEY_PREFIX}${appWidgetId}${KEY_NUMBER_OF_TRENDS}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_TEXT_FORMAT}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_RANKING}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_IMAGE_MODE}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_DENSITY}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_BACKGROUND}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_LAST_UPDATE}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_TAP_ACTION}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_MANUAL_REFRESH}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_UPDATE_FREQUENCY}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_VOLUME}")
            remove("${KEY_PREFIX}${appWidgetId}${KEY_SHOW_PUB_DATE}")
            apply()
        }
    }

    fun hasConfig(appWidgetId: Int): Boolean {
        return prefs.contains("${KEY_PREFIX}${appWidgetId}${KEY_NUMBER_OF_TRENDS}")
    }
}
