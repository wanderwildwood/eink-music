package com.wanderwildwood.einkmusic

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * YouTube Music-only search client backed by NewPipe Extractor.
 *
 * This avoids the official YouTube Data API and restricts results to
 * YouTube Music songs so that only music tracks surface in CalmMusic.
 */
internal class YouTubeMusicSearchClientImpl(
    private val httpClient: OkHttpClient,
) : YouTubeMusicSearchClient {

    override suspend fun searchSongs(term: String, limit: Int): List<YouTubeSongResult> {
        if (term.isBlank() || limit <= 0) return emptyList()

        return withContext(Dispatchers.IO) {
            // Ensure NewPipe is initialized with an OkHttp-backed Downloader.
            YouTubeStreamResolver.ensureInitialized(httpClient)

            // Use the standard YouTube service and apply a YouTube Music filter in the query.
            val service = ServiceList.YouTube

            // Newer extractor versions moved from raw string content-filter keys to FilterItem
            // objects looked up from the factory's available filter groups.
            val musicSongsFilter = service.searchQHFactory.availableContentFilter
                .filterGroups
                .flatMap { it.filterItems.toList() }
                .firstOrNull { it.name == "music_songs" }
                ?: error("Could not find music_songs content filter in extractor")

            // Build a search query handler for this service, restricted to YouTube Music songs.
            val queryHandler = service.searchQHFactory.fromQuery(
                term,
                listOf(musicSongsFilter),
                emptyList(),
            )

            // Perform a YouTube Music search for the query term.
            val searchInfo = SearchInfo.getInfo(service, queryHandler)
            val items = searchInfo.relatedItems

            items.asSequence()
                .filterIsInstance<StreamInfoItem>()
                .mapNotNull { item ->
                    val videoId = extractVideoIdFromUrl(item.url) ?: return@mapNotNull null
                    val durationSeconds = item.duration

                    YouTubeSongResult(
                        videoId = videoId,
                        title = item.name.orEmpty(),
                        artist = item.uploaderName,
                        durationMillis = durationSeconds.takeIf { it > 0 }?.let { it * 1000L },
                    )
                }
                .distinctBy { it.videoId }
                .take(limit)
                .toList()
        }
    }

    companion object {
        fun create(): YouTubeMusicSearchClient {
            val client = OkHttpClient.Builder().build()
            return YouTubeMusicSearchClientImpl(client)
        }
    }
}

private fun extractVideoIdFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null

    return try {
        val uri = Uri.parse(url)

        val vParam = uri.getQueryParameter("v")
        if (!vParam.isNullOrBlank()) return vParam

        val segments = uri.pathSegments
        segments.lastOrNull()?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}
