package eu.kanade.tachiyomi.extension.en.novelfull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NovelFullFiltersTest {
    @Test
    fun `genre takes precedence over status without a keyword`() {
        val filters = novelFullFilterList()
        filters.filterIsInstance<NovelFullGenreFilter>().single().state = 1
        filters.filterIsInstance<NovelFullStatusFilter>().single().state = 2

        val selection = filters.toNovelFullSearchSelection("")

        assertNull(selection.query)
        assertEquals("/genre/Action", selection.browsePath)
    }

    @Test
    fun `keyword search ignores selected browse filters`() {
        val filters = novelFullFilterList()
        filters.filterIsInstance<NovelFullGenreFilter>().single().state = 1

        val selection = filters.toNovelFullSearchSelection("cultivation")

        assertEquals("cultivation", selection.query)
        assertEquals("/genre/Action", selection.browsePath)
    }
}
