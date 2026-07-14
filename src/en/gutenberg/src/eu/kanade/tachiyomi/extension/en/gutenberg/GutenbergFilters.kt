package eu.kanade.tachiyomi.extension.en.gutenberg

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList

internal data class GutenbergSortOption(
    val label: String,
    val queryValue: String?,
)

internal data class GutenbergSearchSelection(
    val sortOrder: String?,
    val query: String?,
)

internal class GutenbergSortFilter : EntryFilter.Select<String>(
    name = "Sort by",
    values = SORT_OPTIONS.map(GutenbergSortOption::label).toTypedArray(),
) {
    val selected: GutenbergSortOption
        get() = SORT_OPTIONS.getOrElse(state) { SORT_OPTIONS.first() }
}

internal class GutenbergAuthorFilter : EntryFilter.Text("Author")

internal class GutenbergTitleFilter : EntryFilter.Text("Title")

internal class GutenbergSubjectFilter : EntryFilter.Text("Subject")

internal fun gutenbergFilterList(): EntryFilterList = EntryFilterList(
    GutenbergSortFilter(),
    EntryFilter.Header("Advanced search"),
    GutenbergAuthorFilter(),
    GutenbergTitleFilter(),
    GutenbergSubjectFilter(),
)

internal fun EntryFilterList.toGutenbergSearchSelection(query: String): GutenbergSearchSelection {
    val sortOrder = filterIsInstance<GutenbergSortFilter>()
        .firstOrNull()
        ?.selected
        ?.queryValue
    val searchTerms = buildList {
        query.trim().takeIf(String::isNotEmpty)?.let(::add)
        this@toGutenbergSearchSelection.filterIsInstance<GutenbergAuthorFilter>().firstOrNull()?.state
            ?.asFieldSearch("a")
            ?.let(::add)
        this@toGutenbergSearchSelection.filterIsInstance<GutenbergTitleFilter>().firstOrNull()?.state
            ?.asFieldSearch("t")
            ?.let(::add)
        this@toGutenbergSearchSelection.filterIsInstance<GutenbergSubjectFilter>().firstOrNull()?.state
            ?.asFieldSearch("s")
            ?.let(::add)
    }.joinToString(" ").ifBlank { null }
    return GutenbergSearchSelection(sortOrder, searchTerms)
}

private fun String.asFieldSearch(prefix: String): String? = trim()
    .split(WHITESPACE_REGEX)
    .filter(String::isNotBlank)
    .joinToString(" ") { "$prefix.$it" }
    .ifBlank { null }

private val SORT_OPTIONS = listOf(
    GutenbergSortOption("Default", null),
    GutenbergSortOption("Popularity", "downloads"),
    GutenbergSortOption("Release date", "release_date"),
    GutenbergSortOption("Title", "title"),
)

private val WHITESPACE_REGEX = Regex("\\s+")
