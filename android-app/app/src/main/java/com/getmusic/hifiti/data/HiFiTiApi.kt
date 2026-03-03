package com.getmusic.hifiti.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class HiFiTiApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    companion object {
        private const val BASE_URL = "https://www.hifiti.com"

        // APlayer config regex: name:'xxx',artist:'xxx',url:'xxx',cover:'xxx'
        private val APLAYER_REGEX =
            Regex("""name:'([^']+)',artist:'([^']+)',url:'([^']+)',cover:'([^']+)'""")

        // Title regex: 歌手《歌名》[格式]
        private val TITLE_REGEX = Regex("""([^《]+)《([^》]+)》\[([^\]]+)]""")

        // Thread ID regex
        private val THREAD_ID_REGEX = Regex("""thread-(\d+)\.htm""")
    }

    /**
     * HiFiTi uses a custom URL encoding: each UTF-8 byte is prefixed with '_' instead of '%'.
     * e.g. "毛不易" → "_E6_AF_9B_E4_B8_8D_E6_98_93"
     */
    fun encodeKeyword(keyword: String): String {
        val bytes = keyword.toByteArray(Charsets.UTF_8)
        return bytes.joinToString("") { byte ->
            val unsigned = byte.toInt() and 0xFF
            if (unsigned < 0x80 && (unsigned.toChar().isLetterOrDigit() ||
                        unsigned.toChar() == '-' || unsigned.toChar() == '.' ||
                        unsigned.toChar() == '_' || unsigned.toChar() == '~')) {
                unsigned.toChar().toString()
            } else {
                "_%02X".format(unsigned)
            }
        }
    }

    suspend fun search(keyword: String, page: Int = 1): SearchResult = withContext(Dispatchers.IO) {
        val encoded = encodeKeyword(keyword)
        val url = if (page <= 1) {
            "$BASE_URL/search-$encoded-1-0.htm"
        } else {
            "$BASE_URL/search-$encoded-1-0-$page.htm"
        }

        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)

        val items = doc.select("div.media-body").mapNotNull { body ->
            val subjectLink = body.selectFirst("div.subject a") ?: return@mapNotNull null
            val href = subjectLink.attr("href")
            val fullTitle = subjectLink.text().trim()

            val threadMatch = THREAD_ID_REGEX.find(href)
            val threadId = threadMatch?.groupValues?.get(1) ?: return@mapNotNull null

            val titleMatch = TITLE_REGEX.find(fullTitle)
            val artist = titleMatch?.groupValues?.get(1)?.trim() ?: ""
            val songName = titleMatch?.groupValues?.get(2)?.trim() ?: fullTitle
            val format = titleMatch?.groupValues?.get(3)?.trim() ?: ""

            val category = body.selectFirst("span.forumname a")?.text()?.trim() ?: ""

            SearchItem(
                threadId = threadId,
                title = fullTitle,
                artist = artist,
                songName = songName,
                format = format,
                category = category
            )
        }

        val hasMore = doc.select("a.page-link").any {
            it.text().trim().toIntOrNull()?.let { p -> p > page } == true
        }

        SearchResult(items = items, currentPage = page, hasMore = hasMore)
    }

    suspend fun getDetail(threadId: String): SongDetail = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/thread-$threadId.htm"
        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)

        // Extract APlayer config from script tags
        var songName = ""
        var artist = ""
        var audioUrl = ""
        var coverUrl = ""

        for (script in doc.select("script")) {
            val scriptText = script.data()
            val match = APLAYER_REGEX.find(scriptText)
            if (match != null) {
                songName = match.groupValues[1]
                artist = match.groupValues[2]
                audioUrl = match.groupValues[3]
                coverUrl = match.groupValues[4]
                break
            }
        }

        // Extract lyrics from <h5>歌词</h5> followed by <p>
        var lyrics: String? = null
        for (h5 in doc.select("h5")) {
            if (h5.text().contains("歌词")) {
                val lyricsP = h5.nextElementSibling()
                if (lyricsP != null && lyricsP.tagName() == "p") {
                    lyrics = lyricsP.html()
                        .replace("<br>", "\n")
                        .replace("<br/>", "\n")
                        .replace("<br />", "\n")
                        .replace(Regex("<[^>]+>"), "")
                        .trim()
                }
                break
            }
        }

        // Resolve the real audio URL by following the redirect
        val realAudioUrl = if (audioUrl.isNotEmpty()) {
            resolveAudioUrl(audioUrl)
        } else null

        SongDetail(
            songName = songName,
            artist = artist,
            audioUrl = audioUrl,
            coverUrl = coverUrl,
            lyrics = lyrics,
            realAudioUrl = realAudioUrl
        )
    }

    /**
     * Follow the getmusic.htm redirect to get the real audio file URL.
     * We make a HEAD request with no auto-redirect to capture the Location header,
     * or a GET request with redirect following.
     */
    suspend fun resolveAudioUrl(getmusicUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val noRedirectClient = client.newBuilder()
                .followRedirects(true)
                .build()

            val request = Request.Builder()
                .url(getmusicUrl)
                .header("User-Agent", userAgent)
                .build()

            noRedirectClient.newCall(request).execute().use { response ->
                response.request.url.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP ${response.code}: $url")
            }
            return response.body?.string() ?: throw RuntimeException("Empty response: $url")
        }
    }
}
