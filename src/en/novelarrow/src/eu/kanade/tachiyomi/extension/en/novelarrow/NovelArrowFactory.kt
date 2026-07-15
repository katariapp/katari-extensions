package eu.kanade.tachiyomi.extension.en.novelarrow

import eu.kanade.tachiyomi.source.entry.EntrySourceFactory
import eu.kanade.tachiyomi.source.entry.UnifiedSource

class NovelArrowFactory : EntrySourceFactory {
    override fun createSources(): List<UnifiedSource> = listOf(NovelArrowSource())
}
