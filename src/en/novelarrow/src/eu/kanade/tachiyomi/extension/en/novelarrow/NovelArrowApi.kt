package eu.kanade.tachiyomi.extension.en.novelarrow

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

internal class NovelArrowApi(
    private val client: OkHttpClient,
    private val headers: Headers,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun catalogue(
        page: Int,
        status: String,
        sort: String,
        keyword: String?,
    ): NovelArrowNovelPage = get(
        "$BASE_URL/api-web/novels".toHttpUrl().newBuilder()
            .addQueryParameter("limit", PAGE_SIZE.toString())
            .addQueryParameter("page", page.toString())
            .addQueryParameter("status", status)
            .addQueryParameter("sort", sort)
            .addQueryParameter("genre", "ALL")
            .apply { keyword?.trim()?.takeIf(String::isNotEmpty)?.let { addQueryParameter("keyword", it) } }
            .build()
            .toString(),
    )

    suspend fun novel(novelId: String): NovelArrowNovel = get<NovelArrowDetailResponse>(
        "$BASE_URL/api-web/novels/$novelId",
    ).item.novelInfo

    suspend fun chapters(novelId: String): List<NovelArrowChapter> {
        val chapters = get<NovelArrowChapterArchive>(
            novelArrowChapterArchiveUrl(novelId),
        ).items
        require(chapters.size <= MAX_CHAPTERS) { "NovelArrow returned too many chapters" }
        require(chapters.distinctBy(NovelArrowChapter::id).size == chapters.size) {
            "NovelArrow returned duplicate chapter identities"
        }
        return chapters
    }

    suspend fun chapter(novelId: String, chapterId: String): NovelArrowChapterContent =
        get<NovelArrowChapterResponse>("$BASE_URL/api-web/novels/$novelId/chapters/$chapterId").item.chapterInfo

    private suspend inline fun <reified T> get(url: String): T = client.newCall(GET(url, headers)).awaitSuccess().use {
        json.decodeFromString(it.body.string())
    }

    private companion object {
        const val PAGE_SIZE = 12
        const val MAX_CHAPTERS = 20_000
    }
}

internal fun novelArrowChapterArchiveUrl(novelId: String): String =
    "$BASE_URL/api-web/novels/$novelId/chapters".toHttpUrl().newBuilder()
        .addQueryParameter("sort", "asc")
        .build()
        .toString()

private const val BASE_URL = "https://novelarrow.com"
