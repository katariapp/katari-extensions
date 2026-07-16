package eu.kanade.tachiyomi.extension.en.gutenberg

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookResourceAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GutenbergBookMediaTest {
    @Test
    fun `acquisition maps to one stable remote EPUB resource`() {
        val acquisition = GutenbergAcquisition(
            href = "https://www.gutenberg.org/ebooks/1342.epub3.images",
            mediaType = EPUB_MEDIA_TYPE,
            title = "EPUB3 (E-readers incl. Send-to-Kindle)",
            length = 24_835_597L,
        )
        val media = acquisition.toBookMedia(
            revision = "Wed, 01 Jul 2026 12:57:08 GMT",
            resolvedSize = 24_835_597L,
            headers = mapOf("User-Agent" to "Katari-Gutenberg/1.0"),
        )

        assertEquals(EPUB_MEDIA_TYPE, media.descriptor.format)
        assertNull(media.descriptor.profile)
        assertEquals("none", media.descriptor.protection)
        assertEquals("Wed, 01 Jul 2026 12:57:08 GMT", media.publicationRevision)
        assertEquals(BookCatalogCoverage.COMPLETE, media.catalog.coverage)
        assertEquals(EPUB_RESOURCE_ID, media.initialResourceId)

        val resource = media.catalog.resources.single()
        assertEquals(EPUB_RESOURCE_ID, resource.id)
        assertEquals("EPUB3", resource.title)
        assertEquals(24_835_597L, resource.size)
        assertEquals(BookResourceAvailability.AVAILABLE, resource.availability)
        val location = resource.location as BookResourceLocation.RemoteRequest
        assertEquals(acquisition.href, location.url)
        assertEquals("Katari-Gutenberg/1.0", location.headers["User-Agent"])
        assertEquals(location, media.initialResourceLocation)
    }

    @Test
    fun `missing revision remains unversioned for safe transient caching`() {
        val media = GutenbergAcquisition(
            href = "https://www.gutenberg.org/ebooks/11.epub.noimages",
            mediaType = EPUB_MEDIA_TYPE,
            title = "EPUB (no images, older E-readers)",
            length = 100L,
        ).toBookMedia(
            revision = null,
            resolvedSize = null,
            headers = emptyMap(),
        )

        assertNull(media.publicationRevision)
        assertNull(media.catalog.revision)
        assertNull(media.catalog.resources.single().revision)
        assertEquals(100L, media.catalog.resources.single().size)
    }
}
