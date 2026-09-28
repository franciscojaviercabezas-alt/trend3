package es.tendencias.app.widget

import android.content.ComponentName
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.datastore.preferences.core.stringPreferencesKey

class TendenciasGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Tendencias España")
                }
            }
        }
    }

    suspend fun updateWidgetData(context: Context, glanceId: GlanceId, key: String, value: String) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val prefKey = stringPreferencesKey(key)
            val mutablePrefs = prefs.toMutablePreferences()
            mutablePrefs[prefKey] = value
            mutablePrefs.toPreferences()
        }
        update(context, glanceId)
    }

    suspend fun clearWidgetData(context: Context, glanceId: GlanceId, key: String) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val prefKey = stringPreferencesKey(key)
            val mutablePrefs = prefs.toMutablePreferences()
            mutablePrefs.remove(prefKey)
            mutablePrefs.toPreferences()
        }
        update(context, glanceId)
    }

    suspend fun refreshWidgetDataSuspend(context: Context) {
        // Método invocado por TrendsUpdateWorker y TendenciasWidgetReceiver
    }

    fun getComponent(context: Context): ComponentName {
        return ComponentName(context, TendenciasGlanceWidgetReceiver::class.java)
    }
}

class TendenciasGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TendenciasGlanceWidget()
}
