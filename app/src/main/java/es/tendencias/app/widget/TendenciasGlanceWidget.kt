package es.tendencias.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import es.tendencias.app.MainActivity
import es.tendencias.app.data.*
import es.tendencias.app.model.Trend
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TendenciasWidgetKeys {
    val TRENDS_JSON = stringPreferencesKey("trends_json")
    val LAST_UPDATE = stringPreferencesKey("last_update")
    val LAST_ERROR = stringPreferencesKey("last_error")
}

fun applyTextFormat(title: String, format: TextFormat): String {
    return when (format) {
        TextFormat.FULL -> title
        TextFormat.SHORT_PHRASE -> {
            val delimiterIndex = title.indexOfAny(charArrayOf(':', '-', '|'))
            if (delimiterIndex > 0) {
                title.substring(0, delimiterIndex).trim()
            } else {
                val words = title.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (words.size > 5) words.take(5).joinToString(" ") else title
            }
        }
        TextFormat.ONE_WORD -> {
            val words = title.split("\\s+".toRegex()).filter { it.isNotBlank() }
            words.firstOrNull() ?: title
        }
        TextFormat.TWO_WORDS -> {
            val words = title.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.size > 2) words.take(2).joinToString(" ") else title
        }
        TextFormat.ONE_LINE -> {
            if (title.length > 40) title.take(39).trimEnd() + "…" else title
        }
    }
}

private fun resolveDensity(config: WidgetPreferencesRepository.WidgetConfig, size: DpSize): Density {
    if (config.density != Density.AUTO) return config.density
    return when (size) {
        TendenciasGlanceWidget.SIZE_2X2 -> Density.COMPACT
        TendenciasGlanceWidget.SIZE_2X4 -> Density.NORMAL
        TendenciasGlanceWidget.SIZE_4X2 -> Density.NORMAL
        TendenciasGlanceWidget.SIZE_4X4 -> Density.WIDE
        else -> Density.NORMAL
    }
}

suspend fun refreshWidgetDataSuspend(context: Context) {
    try {
        val repository = TrendsRepositoryImpl()
        val cache = TrendsCache(context)
        val result = repository.getTrends()
        val glanceManager = GlanceAppWidgetManager(context)
        val glanceIds = glanceManager.getGlanceIds(TendenciasGlanceWidget::class.java)

        result.fold(
            onSuccess = { trends ->
                val now = System.currentTimeMillis()
                cache.saveTrends(trends)
                cache.saveLastUpdate(now)
                cache.clearError()

                val json = TrendsJson.serializeTrends(trends)
                val nowStr = now.toString()
                for (glanceId in glanceIds) {
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs[TendenciasWidgetKeys.TRENDS_JSON] = json
                        prefs[TendenciasWidgetKeys.LAST_UPDATE] = nowStr
                        prefs.remove(TendenciasWidgetKeys.LAST_ERROR)
                    }
                }
            },
            onFailure = { error ->
                val errorMsg = error.localizedMessage ?: "No se pudo actualizar"
                cache.saveLastError(errorMsg)

                for (glanceId in glanceIds) {
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs[TendenciasWidgetKeys.LAST_ERROR] = errorMsg
                    }
                }
            }
        )
        TendenciasGlanceWidget().updateAll(context)
    } catch (_: Exception) {
    }
}

class RefreshTrendsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        refreshWidgetDataSuspend(context)
    }
}

class TendenciasGlanceWidget : GlanceAppWidget() {

    companion object {
        internal val SIZE_2X2 = DpSize(120.dp, 120.dp)
        internal val SIZE_4X2 = DpSize(260.dp, 120.dp)
        internal val SIZE_2X4 = DpSize(120.dp, 260.dp)
        internal val SIZE_4X4 = DpSize(260.dp, 260.dp)

        private const val MAX_2X2 = 3
        private const val MAX_4X2 = 5
        private const val MAX_2X4 = 8
        private const val MAX_4X4 = 10
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_2X2, SIZE_4X2, SIZE_2X4, SIZE_4X4)
    )

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) {
            AppWidgetManager.INVALID_APPWIDGET_ID
        }
        val config = WidgetPreferencesRepository(context).loadConfig(appWidgetId)

        provideContent {
            val size = LocalSize.current
            val prefs = currentState<Preferences>()
            val trendsJson = prefs[TendenciasWidgetKeys.TRENDS_JSON] ?: ""
            val lastError = prefs[TendenciasWidgetKeys.LAST_ERROR]
            val lastUpdateStr = prefs[TendenciasWidgetKeys.LAST_UPDATE]
            val allTrends = TrendsJson.deserializeTrends(trendsJson)

            val sizeLimit = when (size) {
                SIZE_2X2 -> MAX_2X2
                SIZE_4X2 -> MAX_4X2
                SIZE_2X4 -> MAX_2X4
                SIZE_4X4 -> MAX_4X4
                else -> MAX_4X4
            }
            val userLimit = if (config.numberOfTrends == WidgetPreferencesRepository.AUTO) {
                allTrends.size
            } else {
                config.numberOfTrends
            }
            val visibleTrends = allTrends.take(minOf(sizeLimit, userLimit))

            GlanceTheme {
                WidgetRoot(
                    trends = visibleTrends,
                    config = config,
                    size = size,
                    hasError = !lastError.isNullOrBlank(),
                    lastUpdateMillis = lastUpdateStr?.toLongOrNull()
                )
            }
        }
    }

    @Composable
    private fun WidgetRoot(
        trends: List<Trend>,
        config: WidgetPreferencesRepository.WidgetConfig,
        size: DpSize,
        hasError: Boolean,
        lastUpdateMillis: Long?
    ) {
        val resolvedDensity = resolveDensity(config, size)

        val is2x2 = size == SIZE_2X2
        val is2x4 = size == SIZE_2X4
        val is4x2 = size == SIZE_4X2
        val is4x4 = size == SIZE_4X4

        val rootPadding = if (is2x4) 6.dp else 8.dp
        val headerSpacerHeight = if (is2x4) 2.dp else 4.dp
        val headerFontSize = if (is2x4) 10.sp else 11.sp

        val (fontSize, verticalItemPadding) = when (resolvedDensity) {
            Density.COMPACT -> 9.sp to 1.dp
            Density.NORMAL -> 10.sp to (if (is2x4) 1.dp else 2.dp)
            Density.WIDE -> 12.sp to 4.dp
            Density.AUTO -> 10.sp to (if (is2x4) 1.dp else 2.dp)
        }

        // Fondo y colores de texto
        val (backgroundModifier, textColor, secondaryColor) = when (config.background) {
            Background.TRANSPARENT -> Triple(
                GlanceModifier,
                GlanceTheme.colors.onSurface,
                GlanceTheme.colors.outline
            )
            Background.LIGHT -> Triple(
                GlanceModifier.background(Color(0xFFFFFFFF)),
                ColorProvider(Color(0xFF000000)),
                ColorProvider(Color(0xFF666666))
            )
            Background.DARK -> Triple(
                GlanceModifier.background(Color(0xFF1E1E1E)),
                ColorProvider(Color(0xFFFFFFFF)),
                ColorProvider(Color(0xFFB0B0B0))
            )
            Background.SYSTEM -> Triple(
                GlanceModifier.background(GlanceTheme.colors.surface),
                GlanceTheme.colors.onSurface,
                GlanceTheme.colors.outline
            )
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .then(backgroundModifier)
                .padding(rootPadding)
        ) {
            // Cabecera según tamaño
            if (is2x2) {
                if (config.showManualRefresh) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "↻",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryColor
                            ),
                            modifier = GlanceModifier.clickable(actionRunCallback<RefreshTrendsAction>())
                        )
                    }
                }
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tendencias España",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = headerFontSize,
                            color = textColor
                        ),
                        modifier = GlanceModifier
                            .defaultWeight()
                            .clickable(actionStartActivity<MainActivity>())
                    )

                    if (is4x2 && config.showLastUpdate && lastUpdateMillis != null) {
                        val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastUpdateMillis))
                        Text(
                            text = "Actualizado: $formattedTime",
                            style = TextStyle(
                                fontSize = 8.sp,
                                color = secondaryColor
                            ),
                            modifier = GlanceModifier.padding(end = 4.dp)
                        )
                    }

                    if (config.showManualRefresh) {
                        Text(
                            text = "↻",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryColor
                            ),
                            modifier = GlanceModifier
                                .padding(start = 6.dp)
                                .clickable(actionRunCallback<RefreshTrendsAction>())
                        )
                    }
                }
            }

            if (hasError && trends.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "No se pudo actualizar",
                    style = TextStyle(
                        fontSize = 8.sp,
                        color = GlanceTheme.colors.error
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(headerSpacerHeight))

            if (trends.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin datos",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = textColor
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    items(trends) { trend ->
                        val formattedTitle = applyTextFormat(trend.title, config.textFormat)
                        val displayText = if (config.showRanking) {
                            "${trend.rank}. $formattedTitle"
                        } else {
                            formattedTitle
                        }

                        val clickModifier = getTrendClickModifier(trend, config.tapAction)

                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = verticalItemPadding)
                                .then(clickModifier)
                        ) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayText,
                                    style = TextStyle(
                                        fontSize = fontSize,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    ),
                                    modifier = GlanceModifier.defaultWeight()
                                )

                                if (!is2x2 && config.showVolume && !trend.approxTraffic.isNullOrBlank()) {
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                    Text(
                                        text = trend.approxTraffic,
                                        style = TextStyle(
                                            fontSize = 8.sp,
                                            color = secondaryColor
                                        )
                                    )
                                }
                            }

                            if (is4x4 && config.showPubDate && !trend.pubDate.isNullOrBlank()) {
                                Text(
                                    text = trend.pubDate,
                                    style = TextStyle(
                                        fontSize = 8.sp,
                                        color = secondaryColor
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Mostrar última actualización en una línea discreta al pie para 2x4 y 4x4
            if ((is2x4 || is4x4) && config.showLastUpdate && lastUpdateMillis != null) {
                val lastUpdateFontSize = if (is2x4) 7.sp else 8.sp
                val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastUpdateMillis))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Actualizado: $formattedTime",
                        style = TextStyle(
                            fontSize = lastUpdateFontSize,
                            color = secondaryColor
                        )
                    )
                }
            }
        }
    }

    private fun getTrendClickModifier(
        trend: Trend,
        action: TapAction
    ): GlanceModifier {
        return when (action) {
            TapAction.OPEN_TRENDS, TapAction.OPEN_LINK -> {
                val targetUrl = trend.trendUrl.ifBlank { "https://trends.google.es" }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                GlanceModifier.clickable(actionStartActivity(intent))
            }
            TapAction.SEARCH_WEB -> {
                val query = try {
                    URLEncoder.encode(trend.title, "UTF-8")
                } catch (_: Exception) {
                    trend.title
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                GlanceModifier.clickable(actionStartActivity(intent))
            }
            TapAction.NONE -> GlanceModifier
        }
    }
}
