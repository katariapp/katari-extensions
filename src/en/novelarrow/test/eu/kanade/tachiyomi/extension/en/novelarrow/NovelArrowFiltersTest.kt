package eu.kanade.tachiyomi.extension.en.novelarrow

import org.junit.Assert.assertEquals
import org.junit.Test

class NovelArrowFiltersTest {
    @Test
    fun `default filters browse latest ongoing and completed titles`() {
        val selection = novelArrowFilterList().toNovelArrowSearchSelection("")

        assertEquals("all", selection.status)
        assertEquals(LATEST_SORT, selection.sort)
    }

    @Test
    fun `search uses the site keyword sort while preserving status`() {
        val filters = novelArrowFilterList()
        filters.filterIsInstance<NovelArrowStatusFilter>().single().state = 2
        filters.filterIsInstance<NovelArrowSortFilter>().single().state = 3

        val selection = filters.toNovelArrowSearchSelection("shadow slave")

        assertEquals("completed", selection.status)
        assertEquals("SEARCH_KEYWORD", selection.sort)
    }
}
