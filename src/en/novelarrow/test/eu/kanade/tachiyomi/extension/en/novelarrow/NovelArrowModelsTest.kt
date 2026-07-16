package eu.kanade.tachiyomi.extension.en.novelarrow

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class NovelArrowModelsTest {
    @Test
    fun `chapter archive retains every item from the unpaged response`() {
        val archive = json.decodeFromString<NovelArrowChapterArchive>(
            """
            {
              "items": [
                {"chapter_id":"chapter-1","chapter_name":"Chapter 1"},
                {"chapter_id":"chapter-2","chapter_name":"Chapter 2"}
              ],
              "pagination": {"page":1,"totalPages":1}
            }
            """.trimIndent(),
        )

        assertEquals(listOf("chapter-1", "chapter-2"), archive.items.map(NovelArrowChapter::id))
    }
}

private val json = Json { ignoreUnknownKeys = true }
