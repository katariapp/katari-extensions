package eu.kanade.tachiyomi.extension.en.novelbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NovelBuddyFiltersTest {
    @Test
    fun `default filters use best match without restricting content`() {
        val selection = novelBuddyFilterList().toNovelBuddySearchSelection("")

        assertNull(selection.sort)
        assertNull(selection.query)
        assertNull(selection.status)
        assertNull(selection.contentRating)
    }

    @Test
    fun `selected filters map to API values`() {
        val filters = novelBuddyFilterList()
        filters.filterIsInstance<NovelBuddyStatusFilter>().single().state = 2
        filters.filterIsInstance<NovelBuddyContentRatingFilter>().single().state = 1
        filters.filterIsInstance<NovelBuddySortFilter>().single().state = 2
        filters.filterIsInstance<NovelBuddyGenreFilter>().single().state = " fantasy "
        filters.filterIsInstance<NovelBuddyMinimumChaptersFilter>().single().state = "100"

        val selection = filters.toNovelBuddySearchSelection(" shadow ")

        assertEquals("shadow", selection.query)
        assertEquals("completed", selection.status)
        assertEquals("safe", selection.contentRating)
        assertEquals(POPULAR_SORT, selection.sort)
        assertEquals("fantasy", selection.genre)
        assertEquals(100, selection.minimumChapters)
    }
}
