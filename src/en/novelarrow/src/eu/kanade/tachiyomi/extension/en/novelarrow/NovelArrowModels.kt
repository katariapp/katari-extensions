package eu.kanade.tachiyomi.extension.en.novelarrow

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jsoup.Jsoup

@Serializable
internal data class NovelArrowNovelPage(
    val items: List<NovelArrowNovel>,
    val pagination: NovelArrowPagination,
)

@Serializable
internal data class NovelArrowDetailResponse(
    val item: NovelArrowDetailItem,
)

@Serializable
internal data class NovelArrowDetailItem(
    val novelInfo: NovelArrowNovel,
)

@Serializable
internal data class NovelArrowNovel(
    @SerialName("novel_id") val id: String,
    @SerialName("novel_name") val name: String,
    @SerialName("novel_author") val author: String? = null,
    @SerialName("novel_status") val status: Int = -1,
    @SerialName("novel_desc") val description: String? = null,
    @SerialName("novel_genres") val genres: List<String> = emptyList(),
    @SerialName("novel_tags") val tags: List<String> = emptyList(),
)

@Serializable
internal data class NovelArrowChapterArchive(
    val items: List<NovelArrowChapter>,
)

@Serializable
internal data class NovelArrowChapterResponse(
    val item: NovelArrowChapterItem,
)

@Serializable
internal data class NovelArrowChapterItem(
    val chapterInfo: NovelArrowChapterContent,
)

@Serializable
internal data class NovelArrowChapter(
    @SerialName("chapter_id") val id: String,
    @SerialName("chapter_name") val name: String,
    @SerialName("premium_content") val premium: Boolean = false,
    @SerialName("platinum_content") val platinum: Boolean = false,
    @SerialName("coin_price") val coinPrice: Int = 0,
    @SerialName("is_free") val isFree: Boolean? = null,
)

@Serializable
internal data class NovelArrowChapterContent(
    @SerialName("chapter_id") val id: String,
    @SerialName("chapter_name") val name: String,
    @SerialName("chapter_content") val content: String,
)

@Serializable
internal data class NovelArrowPagination(
    val page: Int,
    val totalPages: Int,
)

internal fun NovelArrowNovel.toSEntry(): SEntry = SEntry.create().apply {
    url = novelUrl(id)
    title = name
    author = author?.trim()?.ifBlank { null }
    description = description?.let(Jsoup::parse)?.text()?.trim()?.ifBlank { null }
    genre = (genres + tags).map(String::trim).filter(String::isNotBlank).distinct().takeIf(List<String>::isNotEmpty)
    status = when (status) {
        0 -> SEntry.ONGOING
        1 -> SEntry.COMPLETED
        else -> SEntry.UNKNOWN
    }
    thumbnailUrl = coverUrl(id)
    type = EntryType.BOOK
}

internal fun NovelArrowPagination.hasNextPage(): Boolean = page < totalPages

internal fun NovelArrowChapter.isPaid(): Boolean = premium || platinum || coinPrice > 0 || isFree == false

internal fun NovelArrowChapter.toSEntryChapter(novelId: String, index: Int): SEntryChapter =
    SEntryChapter.create().apply {
        url = chapterKey(novelId, id)
        name = this@toSEntryChapter.name
        dateUpload = 0L
        chapterNumber = (index + 1).toDouble()
        memo = buildJsonObject { put(PAID_MEMO_KEY, isPaid()) }
    }

internal fun SEntryChapter.isNovelArrowPaid(): Boolean =
    memo[PAID_MEMO_KEY]?.jsonPrimitive?.booleanOrNull == true

internal fun novelUrl(novelId: String): String = "/novel/$novelId"

internal fun coverUrl(novelId: String): String = "https://images.novelarrow.com/novel_240_360/$novelId.jpg"

private const val PAID_MEMO_KEY = "novelarrow.paid"
