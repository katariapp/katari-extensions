package eu.kanade.tachiyomi.extension.en.gutenberg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GutenbergFiltersTest {
    @Test
    fun `default filters leave sort and query unchanged`() {
        val selection = gutenbergFilterList().toGutenbergSearchSelection("  Sherlock Holmes  ")

        assertNull(selection.sortOrder)
        assertEquals("Sherlock Holmes", selection.query)
    }

    @Test
    fun `selected filters compose Gutenberg field search`() {
        val filters = gutenbergFilterList()
        filters.filterIsInstance<GutenbergSortFilter>().single().state = 2
        filters.filterIsInstance<GutenbergAuthorFilter>().single().state = "Jane Austen"
        filters.filterIsInstance<GutenbergTitleFilter>().single().state = "Pride Prejudice"
        filters.filterIsInstance<GutenbergSubjectFilter>().single().state = "Love stories"

        val selection = filters.toGutenbergSearchSelection("classic")

        assertEquals("release_date", selection.sortOrder)
        assertEquals(
            "classic a.Jane a.Austen t.Pride t.Prejudice s.Love s.stories",
            selection.query,
        )
    }

    @Test
    fun `filters can browse without a text query`() {
        val filters = gutenbergFilterList()
        filters.filterIsInstance<GutenbergSortFilter>().single().state = 3
        filters.filterIsInstance<GutenbergSubjectFilter>().single().state = "Detective fiction"

        val selection = filters.toGutenbergSearchSelection("")

        assertEquals("title", selection.sortOrder)
        assertEquals("s.Detective s.fiction", selection.query)
    }
}
