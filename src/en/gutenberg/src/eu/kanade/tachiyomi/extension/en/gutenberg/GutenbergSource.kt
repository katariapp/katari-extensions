package eu.kanade.tachiyomi.extension.en.gutenberg

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryHttpSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

internal class GutenbergSource : EntryHttpSource(), SourceMetadata {
    override val id: Long = SOURCE_ID
    override val name: String = "Project Gutenberg"
    override val lang: String = "en"
    override val supportsLatest: Boolean = true
    override val supportedEntryTypes: Set<EntryType> = setOf(EntryType.BOOK)
    override val baseUrl: String = "https://www.gutenberg.org"

    private val publicationCache = PublicationCache()
    private val publicationMutex = Mutex()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .set("User-Agent", USER_AGENT)

    override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> =
        fetchNavigation(page = page, sortOrder = "downloads")

    override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> =
        fetchNavigation(page = page, sortOrder = "release_date")

    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> {
        val selection = filters.toGutenbergSearchSelection(query)
        return fetchNavigation(
            page = page,
            sortOrder = selection.sortOrder,
            query = selection.query,
        )
    }

    override fun getFilterList(): EntryFilterList = gutenbergFilterList()

    override suspend fun getContentDetails(entry: SEntry): SEntry {
        val publication = publication(entry.ebookId())
        return entry.copy().apply {
            initialized = true
            title = publication.title
            author = publication.authors.joinToString().ifBlank { null }
            description = publication.description()
            genre = publication.subjects.takeIf(List<String>::isNotEmpty)
            status = SEntry.COMPLETED
            thumbnailUrl = publication.coverUrl
            type = EntryType.BOOK
        }
    }

    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> {
        val publication = publication(entry.ebookId())
        val acquisition = publication.preferredEpub() ?: return emptyList()
        return listOf(
            SEntryChapter.create().apply {
                url = "/ebooks/${publication.ebookId}"
                name = acquisition.readerLabel
                dateUpload = publication.publishedAt
                chapterNumber = 1.0
                memo = buildJsonObject {
                    put(MEMO_EBOOK_ID, publication.ebookId)
                    put(MEMO_ACQUISITION_URL, acquisition.href)
                    acquisition.title?.let { put(MEMO_ACQUISITION_TITLE, it) }
                    acquisition.length?.let { put(MEMO_ACQUISITION_LENGTH, it) }
                }
            },
        )
    }

    override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia {
        val resolved = resolveAcquisition(chapter)
        val referer = "$baseUrl/ebooks/${resolved.ebookId}"
        val remoteMetadata = fetchRemoteMetadata(resolved.acquisition, referer)
        return resolved.acquisition.toBookMedia(
            revision = remoteMetadata.revision,
            resolvedSize = remoteMetadata.size,
            headers = downloadHeaders(referer),
        )
    }

    private suspend fun fetchNavigation(
        page: Int,
        sortOrder: String?,
        query: String? = null,
    ): EntryPageResult<SEntry> {
        require(page >= 1) { "page must be positive" }
        val url = "$baseUrl/ebooks/search.opds/".toHttpUrl().newBuilder().apply {
            sortOrder?.let { addQueryParameter("sort_order", it) }
            query?.let { addQueryParameter("query", it) }
            addQueryParameter("languages[]", lang)
            addQueryParameter("filetype", DISCOVERY_FILE_TYPE)
            if (page > 1) addQueryParameter("start_index", (((page - 1) * PAGE_SIZE) + 1).toString())
        }.build()
        val parsed = GutenbergOpdsParser.parseNavigation(fetchOpds(url.toString()), baseUrl)
        return EntryPageResult(
            items = parsed.entries.map { item ->
                SEntry.create().apply {
                    this.url = "/ebooks/${item.ebookId}"
                    title = item.title
                    author = item.author
                    thumbnailUrl = item.coverUrl
                    status = SEntry.COMPLETED
                    type = EntryType.BOOK
                }
            },
            hasNextPage = parsed.hasNextPage,
        )
    }

    private suspend fun publication(ebookId: String): GutenbergPublication {
        publicationCache[ebookId]?.let { return it }
        return publicationMutex.withLock {
            publicationCache[ebookId]?.let { return@withLock it }
            val publication = GutenbergOpdsParser.parsePublication(
                xml = fetchOpds("$baseUrl/ebooks/$ebookId.opds"),
                baseUrl = baseUrl,
                expectedEbookId = ebookId,
            ) ?: error("Project Gutenberg returned no publication metadata for eBook $ebookId")
            publicationCache.put(publication)
            publication
        }
    }

    private suspend fun fetchOpds(url: String): String {
        val opdsHeaders = headers.newBuilder()
            .set("Accept", OPDS_MEDIA_TYPE)
            .build()
        return client.newCall(GET(url, opdsHeaders)).awaitSuccess().use { it.body.string() }
    }

    private suspend fun resolveAcquisition(chapter: SEntryChapter): ResolvedAcquisition {
        val memoId = chapter.memo[MEMO_EBOOK_ID]?.jsonPrimitive?.contentOrNull
        val ebookId = memoId ?: chapter.ebookId()
        val memoUrl = chapter.memo[MEMO_ACQUISITION_URL]?.jsonPrimitive?.contentOrNull
        if (memoUrl != null) {
            return ResolvedAcquisition(
                ebookId = ebookId,
                acquisition = GutenbergAcquisition(
                    href = memoUrl,
                    mediaType = EPUB_MEDIA_TYPE,
                    title = chapter.memo[MEMO_ACQUISITION_TITLE]?.jsonPrimitive?.contentOrNull,
                    length = chapter.memo[MEMO_ACQUISITION_LENGTH]?.jsonPrimitive?.longOrNull,
                ),
            )
        }
        val acquisition = publication(ebookId).preferredEpub()
            ?: error("Project Gutenberg eBook $ebookId has no supported EPUB rendition")
        return ResolvedAcquisition(ebookId, acquisition)
    }

    private suspend fun fetchRemoteMetadata(
        acquisition: GutenbergAcquisition,
        referer: String,
    ): RemoteMetadata {
        return try {
            val request = Request.Builder()
                .url(acquisition.href)
                .headers(downloadHeaders(referer).toHeaders())
                .head()
                .build()
            client.newCall(request).awaitSuccess().use { response ->
                RemoteMetadata(
                    revision = response.header("Last-Modified")?.trim()?.ifBlank { null },
                    size = response.header("Content-Length")?.toLongOrNull(),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            RemoteMetadata(revision = null, size = acquisition.length)
        }
    }

    private fun downloadHeaders(referer: String): Map<String, String> = mapOf(
        "User-Agent" to USER_AGENT,
        "Accept" to EPUB_MEDIA_TYPE,
        "Referer" to referer,
    )

    private fun SEntry.ebookId(): String = url.gutenbergId()
        ?: error("Invalid Project Gutenberg entry URL: $url")

    private fun SEntryChapter.ebookId(): String = url.gutenbergId()
        ?: error("Invalid Project Gutenberg child URL: $url")
}

private data class ResolvedAcquisition(
    val ebookId: String,
    val acquisition: GutenbergAcquisition,
)

private data class RemoteMetadata(
    val revision: String?,
    val size: Long?,
)

private class PublicationCache {
    private val values = object : LinkedHashMap<String, CacheEntry>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    operator fun get(ebookId: String): GutenbergPublication? = synchronized(values) {
        val cached = values[ebookId] ?: return@synchronized null
        if (System.currentTimeMillis() - cached.storedAt > CACHE_TTL_MILLIS) {
            values.remove(ebookId)
            return@synchronized null
        }
        cached.publication
    }

    fun put(publication: GutenbergPublication) = synchronized(values) {
        values[publication.ebookId] = CacheEntry(publication, System.currentTimeMillis())
    }
}

private data class CacheEntry(
    val publication: GutenbergPublication,
    val storedAt: Long,
)

private fun GutenbergPublication.description(): String? = listOfNotNull(
    summary,
    rights?.let { "Rights: $it" },
).joinToString("\n\n").ifBlank { null }

private fun String.gutenbergId(): String? = Regex("/ebooks/(\\d+)", RegexOption.IGNORE_CASE)
    .find(this)
    ?.groupValues
    ?.getOrNull(1)

private const val SOURCE_ID = 500000005L
private const val PAGE_SIZE = 25
private const val DISCOVERY_FILE_TYPE = "epub.noimages"
private const val OPDS_MEDIA_TYPE = "application/atom+xml;profile=opds-catalog"
private const val USER_AGENT = "Katari-Gutenberg/1.0 (+https://github.com/katariapp/katari-extensions)"
private const val CACHE_TTL_MILLIS = 60_000L
private const val MAX_CACHE_ENTRIES = 32
private const val MEMO_EBOOK_ID = "gutenberg.ebookId"
private const val MEMO_ACQUISITION_URL = "gutenberg.acquisitionUrl"
private const val MEMO_ACQUISITION_TITLE = "gutenberg.acquisitionTitle"
private const val MEMO_ACQUISITION_LENGTH = "gutenberg.acquisitionLength"
