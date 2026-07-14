package eu.kanade.tachiyomi.extension.en.gutenberg

import eu.kanade.tachiyomi.source.entry.EntrySourceFactory
import eu.kanade.tachiyomi.source.entry.UnifiedSource

class GutenbergFactory : EntrySourceFactory {
    override fun createSources(): List<UnifiedSource> = listOf(GutenbergSource())
}
