package eu.kanade.tachiyomi.extension.en.readnovelfull

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import mihon.book.api.BookCatalogCoverage
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadNovelFullBookMediaTest {
    @Test
    fun `chapter creates one selected inline html resource`() {
        val media = ReadNovelFullChapterContent("Chapter 1", "<p>Once upon a time.</p>")
            .toBookMedia("/novel/chapter-1.html", "Fallback")

        assertEquals("text/html", media.descriptor.format)
        assertEquals("prose-chapter", media.descriptor.profile)
        assertEquals(BookCatalogCoverage.PARTIAL, media.catalog.coverage)
        assertEquals(listOf("/novel/chapter-1.html"), media.catalog.resources.map { it.id })
        assertEquals("Chapter 1", media.catalog.resources.single().title)
        assertEquals(
            "<p>Once upon a time.</p>",
            (media.initialResourceLocation as BookResourceLocation.InlineText).text,
        )
    }
}
