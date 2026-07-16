package eu.kanade.tachiyomi.extension.en.novelarrow

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryHttpSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import okhttp3.Headers
import okhttp3.OkHttpClient

internal class NovelArrowSource : EntryHttpSource(), SourceMetadata {
    override val id: Long = SOURCE_ID
    override val name: String = "NovelArrow"
    override val lang: String = "en"
    override val supportsLatest: Boolean = true
    override val supportedEntryTypes: Set<EntryType> = setOf(EntryType.BOOK)
    override val baseUrl: String = "https://novelarrow.com"

    override val client: OkHttpClient = network.client.newBuilder()
        .addInterceptor(NovelArrowThrottleInterceptor())
        .build()

    private val api by lazy { NovelArrowApi(client, headers) }

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .set("User-Agent", BROWSER_USER_AGENT)
        .set("Accept", "application/json, text/plain, */*")
        .set("Accept-Language", "en-US,en;q=0.9")

    override fun getFilterList(): EntryFilterList = novelArrowFilterList()

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> = catalogue(
        page = page,
        status = "all",
        sort = POPULAR_SORT,
        keyword = null,
    )

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> = catalogue(
        page = page,
        status = "all",
        sort = LATEST_SORT,
        keyword = null,
    )

    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> {
        val selection = filters.toNovelArrowSearchSelection(query)
        return catalogue(page, selection.status, selection.sort, query)
    }

    override suspend fun getContentDetails(entry: SEntry): SEntry = api.novel(entry.novelId()).toSEntry().also {
        it.initialized = true
    }

    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> {
        val novelId = entry.novelId()
        return api.chapters(novelId).mapIndexed { index, chapter ->
            chapter.toSEntryChapter(novelId, index)
        }
    }

    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia {
        val key = chapter.url.toNovelArrowResourceKey()
            ?: error("Invalid NovelArrow BOOK resource key: ${chapter.url}")
        if (chapter.isNovelArrowPaid()) return key.toUnavailableBookMedia(chapter.name)
        val content = api.chapter(key.novelId, key.chapterId)
        require(content.id == key.chapterId) { "NovelArrow returned a different chapter" }
        return content.toBookMedia()
    }

    private suspend fun catalogue(
        page: Int,
        status: String,
        sort: String,
        keyword: String?,
    ): EntryPageResult<SEntry> {
        require(page >= 1) { "page must be positive" }
        val result = api.catalogue(page, status, sort, keyword)
        return EntryPageResult(
            items = result.items.map(NovelArrowNovel::toSEntry),
            hasNextPage = result.pagination.hasNextPage(),
        )
    }

    private fun SEntry.novelId(): String = url.removePrefix("/novel/")
        .substringBefore('?')
        .takeIf { it.isNotBlank() && '/' !in it }
        ?: error("Invalid NovelArrow entry URL: $url")

    private companion object {
        const val SOURCE_ID = 500000006L
        const val BROWSER_USER_AGENT = "Mozilla/5.0 (Linux; Android 15; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36"
    }
}
