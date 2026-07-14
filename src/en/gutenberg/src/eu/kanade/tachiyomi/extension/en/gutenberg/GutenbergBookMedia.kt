package eu.kanade.tachiyomi.extension.en.gutenberg

import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookResourceAvailability

internal fun GutenbergAcquisition.toBookMedia(
    revision: String?,
    resolvedSize: Long?,
    headers: Map<String, String>,
): EntryMedia.Book {
    val location = BookResourceLocation.RemoteRequest(
        url = href,
        headers = headers,
    )
    val resource = BookSourceResource(
        id = EPUB_RESOURCE_ID,
        title = readerLabel,
        order = 0,
        mediaType = EPUB_MEDIA_TYPE,
        size = resolvedSize ?: length,
        revision = revision,
        availability = BookResourceAvailability.AVAILABLE,
        location = location,
    )
    return EntryMedia.Book(
        descriptor = BookContentDescriptor(format = EPUB_MEDIA_TYPE),
        publicationRevision = revision,
        catalog = BookResourceCatalog(
            resources = listOf(resource),
            revision = revision,
            coverage = BookCatalogCoverage.COMPLETE,
        ),
        initialResourceId = EPUB_RESOURCE_ID,
        initialResourceLocation = location,
    )
}

internal const val EPUB_RESOURCE_ID = "epub"
internal const val EPUB_MEDIA_TYPE = "application/epub+zip"
