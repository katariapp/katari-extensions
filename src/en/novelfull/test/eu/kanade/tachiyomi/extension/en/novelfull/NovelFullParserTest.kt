package eu.kanade.tachiyomi.extension.en.novelfull

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovelFullParserTest {
    @Test
    fun `listing entries are books and expose next page`() {
        val document = Jsoup.parse(
            """
            <div id="list-page"><div class="archive"><div class="list list-truyen">
              <div class="row"><img class="cover" src="/cover.jpg"><h3 class="truyen-title"><a href="/novel.html">Novel</a></h3><span class="author">Author</span></div>
            </div></div></div>
            <ul class="pagination"><li class="next"><a href="?page=2">></a></li></ul>
            """.trimIndent(),
            "https://novelfull.net",
        )

        val result = NovelFullParser.parseListing(document)

        assertEquals(1, result.entries.size)
        assertEquals("/novel.html", result.entries.single().url)
        assertEquals("https://novelfull.net/cover.jpg", result.entries.single().thumbnailUrl)
        assertEquals("Novel", result.entries.single().title)
        assertTrue(result.hasNextPage)
    }

    @Test
    fun `details preserve book type and parse metadata`() {
        val document = Jsoup.parse(
            """
            <div id="truyen">
              <div class="info-holder"><div class="books"><h3 class="title">Novel</h3></div><div class="book"><img src="/cover.jpg"></div><div class="info"><a href="/author/name">Author</a><a href="/genre/Action">Action</a><a href="/status/Ongoing">Ongoing</a></div></div>
              <div class="desc-text"><p>A synopsis.</p></div>
            </div>
            """.trimIndent(),
            "https://novelfull.net",
        )
        val result = NovelFullParser.parseDetails(document)

        assertEquals("Novel", result.title)
        assertEquals("Author", result.author)
        assertEquals(listOf("Action"), result.genres)
        assertEquals("Ongoing", result.status)
    }

    @Test
    fun `chapter parser removes ad nodes but retains prose`() {
        val document = Jsoup.parse(
            """
            <div id="chapter"><a class="chapter-title">Chapter 1</a></div>
            <div id="chapter-content"><div id="frame"><iframe data-aa="1"></iframe></div><p></p><p>First paragraph.</p>Unwrapped text<div class="ads ads-holder ads-middle"></div></div>
            """.trimIndent(),
        )

        val result = NovelFullParser.parseChapter(document)

        assertEquals("Chapter 1", result.title)
        assertTrue(result.html.contains("First paragraph."))
        assertTrue(result.html.contains("Unwrapped text"))
        assertFalse(result.html.contains("iframe"))
        assertFalse(result.html.contains("ads-holder"))
    }

    @Test
    fun `chapter pagination accepts the relative last page URL`() {
        val document = Jsoup.parse(
            "<div id=\"list-chapter\"></div><ul class=\"pagination\"><li class=\"last\"><a href=\"/novel.html?page=11\">Last</a></li></ul>",
        )

        assertEquals(11, NovelFullParser.chapterPageCount(document))
    }
}
