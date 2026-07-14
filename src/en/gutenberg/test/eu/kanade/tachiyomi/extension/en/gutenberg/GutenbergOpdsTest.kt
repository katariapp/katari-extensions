package eu.kanade.tachiyomi.extension.en.gutenberg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GutenbergOpdsTest {
    @Test
    fun `navigation maps stable ebook identities and pagination`() {
        val page = GutenbergOpdsParser.parseNavigation(NAVIGATION_FEED, BASE_URL)

        assertEquals(2, page.entries.size)
        assertEquals("1342", page.entries[0].ebookId)
        assertEquals("Pride and Prejudice", page.entries[0].title)
        assertEquals("Jane Austen", page.entries[0].author)
        assertEquals("84", page.entries[1].ebookId)
        assertTrue(page.hasNextPage)
    }

    @Test
    fun `publication merges editions and prefers epub3 with images`() {
        val publication = requireNotNull(
            GutenbergOpdsParser.parsePublication(PUBLICATION_FEED, BASE_URL, "1342"),
        )

        assertEquals("Pride and Prejudice", publication.title)
        assertEquals(listOf("Austen, Jane"), publication.authors)
        assertEquals("A test summary.", publication.summary)
        assertEquals("Public domain in the USA.", publication.rights)
        assertEquals(listOf("England -- Fiction", "Love stories"), publication.subjects)
        assertEquals("en", publication.language)
        assertEquals("https://www.gutenberg.org/cache/epub/1342/cover.jpg", publication.coverUrl)
        assertEquals(2, publication.acquisitions.size)

        val selected = requireNotNull(publication.preferredEpub())
        assertEquals("EPUB3", selected.readerLabel)
        assertEquals("https://www.gutenberg.org/ebooks/1342.epub3.images", selected.href)
        assertEquals(24_835_597L, selected.length)
    }

    @Test
    fun `publication falls back to no-images epub`() {
        val publication = GutenbergOpdsParser.parsePublication(NO_IMAGES_FEED, BASE_URL, "11")

        assertNotNull(publication)
        assertEquals("https://www.gutenberg.org/ebooks/11.epub.noimages", publication?.preferredEpub()?.href)
        assertEquals("EPUB", publication?.preferredEpub()?.readerLabel)
    }

    @Test
    fun `unknown publication is rejected without leaking another edition`() {
        assertNull(GutenbergOpdsParser.parsePublication(PUBLICATION_FEED, BASE_URL, "999"))
    }

    @Test
    fun `navigation ignores malformed records`() {
        val page = GutenbergOpdsParser.parseNavigation(MALFORMED_NAVIGATION_FEED, BASE_URL)

        assertTrue(page.entries.isEmpty())
        assertFalse(page.hasNextPage)
    }

    private companion object {
        const val BASE_URL = "https://www.gutenberg.org"

        val NAVIGATION_FEED = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <link rel="next" href="/ebooks/search.opds/?start_index=26" />
              <entry>
                <id>https://www.gutenberg.org/ebooks/1342.opds</id>
                <title>Pride and Prejudice</title>
                <content type="text">Jane Austen</content>
                <link rel="subsection" href="/ebooks/1342.opds" />
              </entry>
              <entry>
                <id>https://www.gutenberg.org/ebooks/84.opds</id>
                <title>Frankenstein</title>
                <author><name>Mary Shelley</name></author>
              </entry>
            </feed>
        """.trimIndent()

        val PUBLICATION_FEED = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom"
                  xmlns:dcterms="http://purl.org/dc/terms/">
              <entry>
                <id>urn:gutenberg:1342:2</id>
                <title>Pride and Prejudice</title>
                <content type="xhtml"><div xmlns="http://www.w3.org/1999/xhtml">
                  <p>Summary: A test summary.</p>
                  <p>Rights: Public domain in the USA.</p>
                </div></content>
                <published>1998-06-01T00:00:00+00:00</published>
                <rights>Public domain in the USA.</rights>
                <author><name>Austen, Jane</name></author>
                <category scheme="http://purl.org/dc/terms/LCSH" term="England -- Fiction" />
                <category scheme="http://purl.org/dc/terms/LCSH" term="Love stories" />
                <dcterms:language>en</dcterms:language>
                <link rel="http://opds-spec.org/acquisition" type="application/epub+zip"
                      title="EPUB (no images, older E-readers)" length="558543"
                      href="/ebooks/1342.epub.noimages" />
              </entry>
              <entry>
                <id>urn:gutenberg:1342:3</id>
                <title>Pride and Prejudice</title>
                <author><name>Austen, Jane</name></author>
                <link rel="http://opds-spec.org/acquisition" type="application/epub+zip"
                      title="EPUB3 (E-readers incl. Send-to-Kindle)" length="24835597"
                      href="/ebooks/1342.epub3.images" />
                <link rel="http://opds-spec.org/image" type="image/jpeg"
                      href="/cache/epub/1342/cover.jpg" />
              </entry>
            </feed>
        """.trimIndent()

        val NO_IMAGES_FEED = """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <id>urn:gutenberg:11:2</id>
                <title>Alice's Adventures in Wonderland</title>
                <link rel="http://opds-spec.org/acquisition" type="application/epub+zip"
                      title="EPUB (no images, older E-readers)" href="/ebooks/11.epub.noimages" />
              </entry>
            </feed>
        """.trimIndent()

        val MALFORMED_NAVIGATION_FEED = """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry><id>missing-ebook-id</id><title>Invalid</title></entry>
              <entry><id>https://www.gutenberg.org/ebooks/1.opds</id><title></title></entry>
            </feed>
        """.trimIndent()
    }
}
