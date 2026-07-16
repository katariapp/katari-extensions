package eu.kanade.tachiyomi.extension.en.novelfull

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal class NovelFullApi(
    private val client: OkHttpClient,
    private val headers: Headers,
) {
    suspend fun document(path: String): Document = client.newCall(GET(url(path), headers)).awaitSuccess().use {
        Jsoup.parse(it.body.string(), BASE_URL)
    }

    suspend fun chapters(entryPath: String): List<NovelFullChapter> {
        val firstPage = document(entryPath)
        val lastPage = NovelFullParser.chapterPageCount(firstPage)
        require(lastPage in 1..MAX_CHAPTER_PAGES) { "NovelFull returned too many chapter pages" }

        val chapters = NovelFullParser.parseChapterList(firstPage, offset = 0).toMutableList()
        for (page in 2..lastPage) {
            chapters += NovelFullParser.parseChapterList(document(pagePath(entryPath, page)), chapters.size)
        }
        require(chapters.distinctBy(NovelFullChapter::url).size == chapters.size) {
            "NovelFull returned duplicate chapter URLs"
        }
        return chapters
    }

    fun pagePath(path: String, page: Int): String {
        require(page >= 1) { "page must be positive" }
        if (page == 1) return path
        return url(path).toHttpUrl().newBuilder()
            .setQueryParameter("page", page.toString())
            .build()
            .toString()
    }

    fun searchPath(query: String): String = "$BASE_URL/search".toHttpUrl().newBuilder()
        .addQueryParameter("keyword", query)
        .build()
        .toString()

    private fun url(path: String): String = when {
        path.startsWith("https://") -> path
        path.startsWith("http://") -> path
        else -> "$BASE_URL/${path.removePrefix("/")}"
    }

    private companion object {
        const val BASE_URL = "https://novelfull.net"
        const val MAX_CHAPTER_PAGES = 1_000
    }
}
