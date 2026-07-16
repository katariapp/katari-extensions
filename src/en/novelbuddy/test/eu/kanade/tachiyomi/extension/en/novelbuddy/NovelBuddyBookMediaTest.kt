package eu.kanade.tachiyomi.extension.en.novelbuddy

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import mihon.book.api.BookCatalogCoverage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NovelBuddyBookMediaTest {
    @Test
    fun `selected chapter becomes one sanitized prose resource`() {
        val media = NovelBuddyChapter(
            id = "chapter-id",
            name = "Chapter 1",
            slug = "chapter-1",
            cv = 123L,
            content = "<div style=\"color:red\"><script>bad()</script><p>Hello <strong>world</strong>.</p></div>",
        ).toBookMedia()

        assertEquals("text/html", media.descriptor.format)
        assertEquals("prose-chapter", media.descriptor.profile)
        assertEquals(BookCatalogCoverage.PARTIAL, media.catalog.coverage)
        assertEquals("chapter-id", media.initialResourceId)
        assertEquals("123", media.catalog.resources.single().revision)
        val text = (media.initialResourceLocation as BookResourceLocation.InlineText).text
        assertEquals("<div><p>Hello <strong>world</strong>.</p></div>", text)
        assertFalse(text.contains("script"))
        assertFalse(text.contains("style"))
    }
}
