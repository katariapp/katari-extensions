package eu.kanade.tachiyomi.extension.en.gutenberg

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList

internal data class GutenbergSortOption(
    val label: String,
    val queryValue: String,
)

internal data class GutenbergTopicOption(
    val label: String,
    val subject: String?,
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

internal class GutenbergTopicFilter : EntryFilter.Select<String>(
    name = "Topic",
    values = TOPIC_OPTIONS.map(GutenbergTopicOption::label).toTypedArray(),
) {
    val selected: GutenbergTopicOption
        get() = TOPIC_OPTIONS.getOrElse(state) { TOPIC_OPTIONS.first() }
}

internal class GutenbergAuthorFilter : EntryFilter.Text("Author")

internal class GutenbergTitleFilter : EntryFilter.Text("Title")

internal class GutenbergSubjectFilter : EntryFilter.Text("Subject")

internal fun gutenbergFilterList(): EntryFilterList = EntryFilterList(
    GutenbergSortFilter(),
    GutenbergTopicFilter(),
    EntryFilter.Header("Advanced search (all fields are combined)"),
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
        this@toGutenbergSearchSelection.filterIsInstance<GutenbergTopicFilter>().firstOrNull()?.selected?.subject
            ?.asFieldSearch("s")
            ?.let(::add)
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

internal fun withGutenbergLanguageScope(query: String?, language: String): String {
    val languageCode = language.trim().ifBlank { error("Gutenberg language must not be blank") }
    return listOfNotNull(
        query?.trim()?.ifBlank { null },
        "l.$languageCode",
    ).joinToString(" ")
}

private fun String.asFieldSearch(prefix: String): String? = trim()
    .split(WHITESPACE_REGEX)
    .filter(String::isNotBlank)
    .joinToString(" ") { "$prefix.$it" }
    .ifBlank { null }

private val SORT_OPTIONS = listOf(
    GutenbergSortOption("Popularity", "downloads"),
    GutenbergSortOption("Release date", "release_date"),
    GutenbergSortOption("Title", "title"),
)

private val TOPIC_OPTIONS = listOf(
    GutenbergTopicOption("Any", null),
    GutenbergTopicOption("Fiction", "fiction"),
    GutenbergTopicOption("Adventure", "adventure stories"),
    GutenbergTopicOption("Science fiction", "science fiction"),
    GutenbergTopicOption("Fantasy", "fantasy fiction"),
    GutenbergTopicOption("Mystery", "detective mystery"),
    GutenbergTopicOption("Romance", "love stories"),
    GutenbergTopicOption("Horror", "horror tales"),
    GutenbergTopicOption("Historical fiction", "historical fiction"),
    GutenbergTopicOption("Children's stories", "children stories"),
    GutenbergTopicOption("Short stories", "short stories"),
    GutenbergTopicOption("Poetry", "poetry"),
    GutenbergTopicOption("Drama", "drama"),
    GutenbergTopicOption("Biography", "biography"),
    GutenbergTopicOption("History", "history"),
)

private val WHITESPACE_REGEX = Regex("\\s+")
