package es.tendencias.app.model

data class Trend(
    val rank: Int,
    val title: String,
    val approxTraffic: String?,
    val imageUrl: String?,
    val imageSource: String?,
    val pubDate: String?,
    val trendUrl: String
)
