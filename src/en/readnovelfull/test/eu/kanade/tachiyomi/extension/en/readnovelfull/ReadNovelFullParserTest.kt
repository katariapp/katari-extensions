package eu.kanade.tachiyomi.extension.en.readnovelfull

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadNovelFullParserTest {
    @Test
    fun `listing preserves completed entries and exposes next page`() {
        val document = Jsoup.parse(
            """
            <div id="list-page"><div class="archive"><div class="list list-novel">
              <div class="row"><img class="cover" src="/cover.jpg"><h3 class="novel-title"><a href="/novel.html">Novel</a></h3><span class="label-full"></span><span class="author">Author</span></div>
            </div></div></div>
            <ul class="pagination"><li class="next"><a href="?page=2">></a></li></ul>
            """.trimIndent(),
            "https://readnovelfull.com",
        )

        val result = ReadNovelFullParser.parseListing(document)
        val entry = result.entries.single()

        assertEquals("/novel.html", entry.url)
        assertEquals("https://readnovelfull.com/cover.jpg", entry.thumbnailUrl)
        assertTrue(entry.completed)
        assertTrue(result.hasNextPage)
    }

    @Test
    fun `details retain metadata and the archive identifier`() {
        val document = Jsoup.parse(
            """
            <div id="novel">
              <div class="info-holder"><div class="books"><h3 class="title">Novel</h3></div><div class="book"><img src="/cover.jpg"></div></div>
              <div id="rating" data-novel-id="2667"></div>
              <ul class="info-meta">
                <li><h3>Author:</h3><a>Author</a></li>
                <li><h3>Genre:</h3><a>Action</a><a>Fantasy</a></li>
                <li><h3>Status:</h3><a>Completed</a></li>
              </ul>
              <div class="desc-text"><p>A synopsis.</p></div>
            </div>
            """.trimIndent(),
            "https://readnovelfull.com",
        )

        val entry = ReadNovelFullParser.parseDetails(document)

        assertEquals("Novel", entry.title)
        assertEquals("Author", entry.author)
        assertEquals(listOf("Action", "Fantasy"), entry.genres)
        assertEquals("Completed", entry.status)
        assertEquals("2667", entry.novelId)
    }

    @Test
    fun `chapter archive preserves paths and chapter numbering`() {
        val document = Jsoup.parse(
            """
            <div class="panel-body"><ul class="list-chapter">
              <li><a href="/novel/chapter-1-first.html">Chapter 1: First</a></li>
              <li><a href="/novel/chapter-2-second.html">Chapter 2: Second</a></li>
            </ul></div>
            """.trimIndent(),
            "https://readnovelfull.com",
        )

        val chapters = ReadNovelFullParser.parseChapterArchive(document)

        assertEquals(listOf("/novel/chapter-1-first.html", "/novel/chapter-2-second.html"), chapters.map { it.url })
        assertEquals(listOf("Chapter 1: First", "Chapter 2: Second"), chapters.map { it.name })
    }

    @Test
    fun `chapter parser removes non-prose nodes`() {
        val document = Jsoup.parse(
            """
            <div id="chapter"><a class="chr-title">Chapter 1</a></div>
            <div id="chr-content"><script>bad()</script><p></p><p>First paragraph.</p><iframe src="ad"></iframe><div class="ads">Ad</div></div>
            """.trimIndent(),
        )

        val result = ReadNovelFullParser.parseChapter(document)

        assertEquals("Chapter 1", result.title)
        assertTrue(result.html.contains("First paragraph."))
        assertFalse(result.html.contains("script"))
        assertFalse(result.html.contains("iframe"))
        assertFalse(result.html.contains("Ad"))
    }
}
