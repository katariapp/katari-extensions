package eu.kanade.tachiyomi.extension.en.novelbuddy

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class NovelBuddyModelsTest {
    @Test
    fun `title and chapter identities retain API ids and slugs`() {
        val titleUrl = titleUrl("eDkM6O8Q", "shadows-oath")
        val chapterUrl = novelBuddyChapterUrl("eDkM6O8Q", "shadows-oath", "8Gv4Kog2", "chapter-110")

        assertEquals(NovelBuddyTitleKey("eDkM6O8Q", "shadows-oath"), titleUrl.toNovelBuddyTitleKey())
        assertEquals(
            NovelBuddyChapterKey("eDkM6O8Q", "shadows-oath", "8Gv4Kog2", "chapter-110"),
            chapterUrl.toNovelBuddyChapterKey(),
        )
    }

    @Test
    fun `chapter archive response retains every returned chapter`() {
        val response = json.decodeFromString<NovelBuddyChaptersResponse>(
            """
            {
              "success": true,
              "data": {
                "chapters": [
                  {"id":"chapter-210","name":"Chapter 210","slug":"chapter-210","number":210},
                  {"id":"chapter-1","name":"Chapter 1","slug":"chapter-1","number":1}
                ]
              }
            }
            """.trimIndent(),
        )

        assertEquals(listOf("chapter-210", "chapter-1"), response.data.chapters.map(NovelBuddyChapter::id))
    }
}

private val json = Json { ignoreUnknownKeys = true }
