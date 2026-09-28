package es.tendencias.app.data

import es.tendencias.app.model.Trend
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialización y deserialización compartida de tendencias en formato JSON con org.json.
 */
object TrendsJson {

    fun serializeTrends(trends: List<Trend>): String {
        val array = JSONArray()
        for (trend in trends) {
            val obj = JSONObject()
            obj.put("rank", trend.rank)
            obj.put("title", trend.title)
            if (trend.approxTraffic != null) obj.put("approxTraffic", trend.approxTraffic)
            if (trend.imageUrl != null) obj.put("imageUrl", trend.imageUrl)
            if (trend.imageSource != null) obj.put("imageSource", trend.imageSource)
            if (trend.pubDate != null) obj.put("pubDate", trend.pubDate)
            obj.put("trendUrl", trend.trendUrl)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeTrends(json: String): List<Trend> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<Trend>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Trend(
                        rank = obj.optInt("rank", i + 1),
                        title = obj.getString("title"),
                        approxTraffic = if (obj.has("approxTraffic")) obj.optString("approxTraffic") else null,
                        imageUrl = if (obj.has("imageUrl")) obj.optString("imageUrl") else null,
                        imageSource = if (obj.has("imageSource")) obj.optString("imageSource") else null,
                        pubDate = if (obj.has("pubDate")) obj.optString("pubDate") else null,
                        trendUrl = obj.optString("trendUrl", "")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }
}
