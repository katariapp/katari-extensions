package eu.kanade.tachiyomi.extension.en.novelbuddy

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
    fun `title detail retains its complete chapter feed`() {
        val title = NovelBuddyTitle(
            id = "WYXwn68O",
            name = "Eternal Emperor's Path to Ascension",
            slug = "eternal-emperors-path-to-ascension",
            chapters = listOf(
                NovelBuddyChapter("chapter-210", "Chapter 210", "chapter-210"),
                NovelBuddyChapter("chapter-1", "Chapter 1", "chapter-1"),
            ),
        )

        assertEquals(listOf("chapter-210", "chapter-1"), title.chapters.map(NovelBuddyChapter::id))
    }
}
