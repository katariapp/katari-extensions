package eu.kanade.tachiyomi.extension.en.novelbuddy

import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookResourceAvailability
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

internal fun NovelBuddyChapterContent.toBookMedia(): EntryMedia.Book {
    val normalizedContent = normalizeNovelBuddyProse(content ?: error("NovelBuddy chapter $id has no prose content"))
    require(normalizedContent.length <= BookResourceLocation.MAX_INLINE_TEXT_LENGTH) {
        "NovelBuddy chapter $id exceeds the supported inline content limit"
    }
    val location = BookResourceLocation.InlineText(normalizedContent, PROSE_MEDIA_TYPE)
    val resource = BookSourceResource(
        id = id,
        title = name,
        order = 0,
        mediaType = PROSE_MEDIA_TYPE,
        size = normalizedContent.encodeToByteArray().size.toLong(),
        revision = cv?.toString(),
        availability = BookResourceAvailability.AVAILABLE,
        location = location,
    )
    return EntryMedia.Book(
        descriptor = PROSE_DESCRIPTOR,
        catalog = BookResourceCatalog(
            resources = listOf(resource),
            coverage = BookCatalogCoverage.PARTIAL,
        ),
        initialResourceId = id,
        initialResourceLocation = location,
    )
}

internal fun normalizeNovelBuddyProse(content: String): String {
    val safelist = Safelist.none().addTags(
        "p", "div", "br", "hr", "h1", "h2", "h3", "h4", "h5", "h6",
        "blockquote", "ul", "ol", "li", "table", "thead", "tbody", "tfoot",
        "tr", "th", "td", "strong", "b", "em", "i", "u", "s", "pre", "code", "sup", "sub",
    )
    return Jsoup.clean(
        content,
        "",
        safelist,
        org.jsoup.nodes.Document.OutputSettings().prettyPrint(false),
    ).trim()
}

private val PROSE_DESCRIPTOR = BookContentDescriptor(
    format = PROSE_MEDIA_TYPE,
    profile = "prose-chapter",
)
private const val PROSE_MEDIA_TYPE = "text/html"
