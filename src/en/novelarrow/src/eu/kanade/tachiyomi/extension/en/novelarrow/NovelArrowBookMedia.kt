package eu.kanade.tachiyomi.extension.en.novelarrow

import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookResourceAvailability

internal fun NovelArrowChapterContent.toBookMedia(): EntryMedia.Book {
    require(content.length <= BookResourceLocation.MAX_INLINE_TEXT_LENGTH) {
        "NovelArrow chapter $id exceeds the supported inline content limit"
    }
    val location = BookResourceLocation.InlineText(content, PROSE_CHAPTER_MEDIA_TYPE)
    return chapterMedia(
        id = id,
        title = name,
        availability = BookResourceAvailability.AVAILABLE,
        location = location,
        size = content.encodeToByteArray().size.toLong(),
    )
}

internal fun NovelArrowResourceKey.toUnavailableBookMedia(title: String): EntryMedia.Book = chapterMedia(
    id = chapterId,
    title = title,
    availability = BookResourceAvailability.PURCHASE_REQUIRED,
    location = null,
    size = null,
)

private fun chapterMedia(
    id: String,
    title: String,
    availability: BookResourceAvailability,
    location: BookResourceLocation?,
    size: Long?,
): EntryMedia.Book {
    val resource = BookSourceResource(
        id = id,
        title = title,
        order = 0,
        mediaType = PROSE_CHAPTER_MEDIA_TYPE,
        size = size,
        availability = availability,
        location = location,
    )
    return EntryMedia.Book(
        descriptor = PROSE_CHAPTER_DESCRIPTOR,
        catalog = BookResourceCatalog(
            resources = listOf(resource),
            coverage = BookCatalogCoverage.PARTIAL,
        ),
        initialResourceId = id,
        initialResourceLocation = location,
    )
}

internal fun chapterKey(novelId: String, chapterId: String): String = "novelarrow:chapter/$novelId/$chapterId"

internal data class NovelArrowResourceKey(
    val novelId: String,
    val chapterId: String,
)

internal fun String.toNovelArrowResourceKey(): NovelArrowResourceKey? {
    if (!startsWith(CHAPTER_PREFIX)) return null
    val parts = removePrefix(CHAPTER_PREFIX).split('/', limit = 2)
    return parts.takeIf { it.size == 2 && it.all(String::isNotBlank) }?.let {
        NovelArrowResourceKey(it[0], it[1])
    }
}

internal val PROSE_CHAPTER_DESCRIPTOR = BookContentDescriptor(
    format = PROSE_CHAPTER_MEDIA_TYPE,
    profile = "prose-chapter",
)
internal const val PROSE_CHAPTER_MEDIA_TYPE = "text/html"
private const val CHAPTER_PREFIX = "novelarrow:chapter/"
