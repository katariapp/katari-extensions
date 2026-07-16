package eu.kanade.tachiyomi.extension.en.novelarrow

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookResourceAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NovelArrowBookMediaTest {
    @Test
    fun `resolved chapter contains only its selected html resource`() {
        val media = NovelArrowChapterContent(
            id = "chapter-2",
            name = "Chapter 2",
            content = "<p>Once upon a time.</p>",
        ).toBookMedia()

        assertEquals("text/html", media.descriptor.format)
        assertEquals("prose-chapter", media.descriptor.profile)
        assertEquals(BookCatalogCoverage.PARTIAL, media.catalog.coverage)
        assertEquals(listOf("chapter-2"), media.catalog.resources.map { it.id })
        assertEquals("chapter-2", media.initialResourceId)
        assertEquals("<p>Once upon a time.</p>", (media.initialResourceLocation as BookResourceLocation.InlineText).text)
    }

    @Test
    fun `paid chapter remains one unavailable selected resource`() {
        val media = NovelArrowResourceKey("shadow-slave", "chapter-3")
            .toUnavailableBookMedia("Chapter 3")

        assertEquals("chapter-3", media.initialResourceId)
        assertNull(media.initialResourceLocation)
        assertEquals(BookResourceAvailability.PURCHASE_REQUIRED, media.catalog.resources.single().availability)
        assertNull(media.catalog.resources.single().location)
    }

}
