package eu.kanade.tachiyomi.extension.en.novelarrow

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList

internal data class NovelArrowSearchSelection(
    val status: String,
    val sort: String,
)

internal class NovelArrowStatusFilter : EntryFilter.Select<String>(
    name = "Status",
    values = arrayOf("All", "Ongoing", "Completed"),
) {
    val queryValue: String
        get() = STATUS_VALUES.getOrElse(state) { STATUS_VALUES.first() }
}

internal class NovelArrowSortFilter : EntryFilter.Select<String>(
    name = "Sort by",
    values = arrayOf("Latest", "Popular", "New", "Rating", "Chapters"),
) {
    val queryValue: String
        get() = SORT_VALUES.getOrElse(state) { SORT_VALUES.first() }
}

internal fun novelArrowFilterList(): EntryFilterList = EntryFilterList(
    NovelArrowStatusFilter(),
    NovelArrowSortFilter(),
)

internal fun EntryFilterList.toNovelArrowSearchSelection(query: String): NovelArrowSearchSelection {
    val status = filterIsInstance<NovelArrowStatusFilter>().firstOrNull()?.queryValue ?: STATUS_VALUES.first()
    val selectedSort = filterIsInstance<NovelArrowSortFilter>().firstOrNull()?.queryValue ?: SORT_VALUES.first()
    return NovelArrowSearchSelection(
        status = status,
        sort = if (query.isBlank()) selectedSort else SEARCH_SORT,
    )
}

internal const val LATEST_SORT = "LASTEST"
internal const val POPULAR_SORT = "HOT"
private const val SEARCH_SORT = "SEARCH_KEYWORD"
private val STATUS_VALUES = listOf("all", "ongoing", "completed")
private val SORT_VALUES = listOf(LATEST_SORT, POPULAR_SORT, "NEW", "RATING", "CHAPTERS")
