package eu.kanade.tachiyomi.extension.en.novelfull

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal object NovelFullParser {
    fun parseListing(document: Document): NovelFullListing = NovelFullListing(
        entries = document.select("#list-page .archive > .list.list-truyen > .row")
            .mapNotNull(::listingEntry),
        hasNextPage = document.selectFirst(".pagination li.next:not(.disabled) a[href]") != null,
    )

    fun parseDetails(document: Document): NovelFullDetails = NovelFullDetails(
        title = document.selectFirst("#truyen .info-holder .books h3.title")?.text()?.trim()?.ifBlank { null },
        author = document.selectFirst("#truyen .info-holder .info a[href^=/author/]")?.text()?.trim()?.ifBlank { null },
        description = document.selectFirst("#truyen .desc-text")?.text()?.trim()?.ifBlank { null },
        genres = document.select("#truyen .info-holder .info a[href^=/genre/]")
            .eachText()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct(),
        status = document.selectFirst("#truyen .info-holder .info a[href^=/status/]")?.text()?.trim(),
        thumbnailUrl = document.selectFirst("#truyen .info-holder .book img[src]")
            ?.attr("abs:src")
            ?.ifBlank { null },
    )

    fun parseChapterList(document: Document, offset: Int): List<NovelFullChapter> = document
        .select("#list-chapter .list-chapter > li > a[href]")
        .mapIndexed { index, link ->
            NovelFullChapter(
                url = link.attr("href").toEntryPath(),
                name = link.text().trim(),
                order = offset + index + 1,
            )
        }

    fun chapterPageCount(document: Document): Int = document
        .selectFirst("#list-chapter ~ ul.pagination li.last a[href]")
        ?.attr("href")
        ?.let { PAGE_QUERY_REGEX.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        ?: 1

    fun parseChapter(document: Document): NovelFullChapterContent {
        val content = document.selectFirst("#chapter-content")?.clone()
            ?: error("NovelFull returned no chapter content")
        content.select("iframe[data-aa], #frame, .ads.ads-holder.ads-middle, script, style").remove()
        content.select("div, p").filter(Element::isBlank).forEach(Element::remove)
        return NovelFullChapterContent(
            title = document.selectFirst("#chapter .chapter-title")?.text()?.trim()?.ifBlank { null },
            html = content.html().trim(),
        )
    }

    private fun listingEntry(row: Element): NovelFullEntry? {
        val link = row.selectFirst("h3.truyen-title a[href]") ?: return null
        return NovelFullEntry(
            url = link.attr("href").toEntryPath(),
            title = link.text().trim(),
            author = row.selectFirst(".author")?.text()?.trim()?.ifBlank { null },
            thumbnailUrl = row.selectFirst("img.cover[src]")?.attr("abs:src")?.ifBlank { null },
        )
    }
}

private val PAGE_QUERY_REGEX = Regex("[?&]page=(\\d+)")

internal data class NovelFullListing(
    val entries: List<NovelFullEntry>,
    val hasNextPage: Boolean,
)

internal data class NovelFullChapterContent(
    val title: String?,
    val html: String,
)

private fun String.toEntryPath(): String = removePrefix("https://novelfull.net")
    .takeIf { it.startsWith('/') }
    ?: error("NovelFull returned an invalid relative URL: $this")

private fun Element.isBlank(): Boolean = text().isBlank() && children().isEmpty()
