package es.tendencias.app.data

import android.util.Xml
import es.tendencias.app.model.Trend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

interface TrendsRepository {
    suspend fun getTrends(): Result<List<Trend>>
}

class TrendsRepositoryImpl : TrendsRepository {

    companion object {
        private const val RSS_URL = "https://trends.google.com/trending/rss?geo=ES"
        private const val TIMEOUT_MILLIS = 15000
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
        private const val ACCEPT_HEADER = "application/rss+xml, application/xml, text/xml"
    }

    override suspend fun getTrends(): Result<List<Trend>> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(RSS_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", ACCEPT_HEADER)
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    IOException("Error HTTP $responseCode al conectar con Google Trends")
                )
            }

            val trends = connection.inputStream.use { inputStream ->
                parseRss(inputStream)
            }

            Result.success(trends)
        } catch (e: SocketTimeoutException) {
            Result.failure(e)
        } catch (e: XmlPullParserException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseRss(inputStream: InputStream): List<Trend> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(inputStream, "UTF-8")

        val trends = mutableListOf<Trend>()
        var eventType = parser.eventType
        var currentRank = 1

        var inItem = false
        var inNewsItem = false

        var title: String? = null
        var approxTraffic: String? = null
        var imageUrl: String? = null
        var imageSource: String? = null
        var pubDate: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        tagName.equals("item", ignoreCase = true) -> {
                            inItem = true
                            inNewsItem = false
                            title = null
                            approxTraffic = null
                            imageUrl = null
                            imageSource = null
                            pubDate = null
                        }
                        inItem && (tagName.equals("news_item", ignoreCase = true) || tagName.endsWith(":news_item", ignoreCase = true)) -> {
                            inNewsItem = true
                        }
                        inItem && !inNewsItem -> {
                            when {
                                tagName.equals("title", ignoreCase = true) -> {
                                    val text = parser.nextText().trim()
                                    if (text.isNotEmpty()) title = text
                                }
                                tagName.equals("approx_traffic", ignoreCase = true) || tagName.endsWith(":approx_traffic", ignoreCase = true) -> {
                                    val text = parser.nextText().trim()
                                    if (text.isNotEmpty()) approxTraffic = text
                                }
                                tagName.equals("picture", ignoreCase = true) || tagName.endsWith(":picture", ignoreCase = true) -> {
                                    val text = parser.nextText().trim()
                                    if (text.isNotEmpty()) imageUrl = text
                                }
                                tagName.equals("picture_source", ignoreCase = true) || tagName.endsWith(":picture_source", ignoreCase = true) -> {
                                    val text = parser.nextText().trim()
                                    if (text.isNotEmpty()) imageSource = text
                                }
                                tagName.equals("pubDate", ignoreCase = true) -> {
                                    val text = parser.nextText().trim()
                                    if (text.isNotEmpty()) pubDate = text
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when {
                        tagName.equals("news_item", ignoreCase = true) || tagName.endsWith(":news_item", ignoreCase = true) -> {
                            inNewsItem = false
                        }
                        tagName.equals("item", ignoreCase = true) -> {
                            if (inItem) {
                                val itemTitle = title
                                if (!itemTitle.isNullOrBlank()) {
                                    val encodedTitle = URLEncoder.encode(itemTitle, "UTF-8")
                                    val trendUrl = "https://trends.google.com/trends/explore?q=$encodedTitle&geo=ES"
                                    trends.add(
                                        Trend(
                                            rank = currentRank,
                                            title = itemTitle,
                                            approxTraffic = approxTraffic,
                                            imageUrl = imageUrl,
                                            imageSource = imageSource,
                                            pubDate = pubDate,
                                            trendUrl = trendUrl
                                        )
                                    )
                                    currentRank++
                                }
                            }
                            inItem = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return trends
    }
}
