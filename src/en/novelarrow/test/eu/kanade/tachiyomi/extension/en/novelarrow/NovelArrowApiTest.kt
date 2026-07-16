package eu.kanade.tachiyomi.extension.en.novelarrow

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NovelArrowApiTest {
    @Test
    fun `chapter archive request opts out of pagination`() {
        val url = novelArrowChapterArchiveUrl("nine-star-hegemon-body-arts").toHttpUrl()

        assertEquals("asc", url.queryParameter("sort"))
        assertNull(url.queryParameter("page"))
    }
}
