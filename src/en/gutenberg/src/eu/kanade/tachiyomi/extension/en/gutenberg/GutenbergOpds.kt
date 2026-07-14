package eu.kanade.tachiyomi.extension.en.gutenberg

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.net.URI
import java.time.OffsetDateTime

internal data class GutenbergNavigationPage(
    val entries: List<GutenbergNavigationEntry>,
    val hasNextPage: Boolean,
)

internal data class GutenbergNavigationEntry(
    val ebookId: String,
    val title: String,
    val author: String?,
)

internal data class GutenbergPublication(
    val ebookId: String,
    val title: String,
    val authors: List<String>,
    val summary: String?,
    val subjects: List<String>,
    val language: String?,
    val rights: String?,
    val publishedAt: Long,
    val coverUrl: String?,
    val acquisitions: List<GutenbergAcquisition>,
) {
    fun preferredEpub(): GutenbergAcquisition? = acquisitions
        .asSequence()
        .filter { it.mediaType.equals(EPUB_MEDIA_TYPE, ignoreCase = true) }
        .sortedWith(
            compareBy<GutenbergAcquisition> { it.preferenceRank() }
                .thenBy { it.length ?: Long.MAX_VALUE },
        )
        .firstOrNull()
}

internal data class GutenbergAcquisition(
    val href: String,
    val mediaType: String,
    val title: String?,
    val length: Long?,
) {
    val readerLabel: String
        get() = if (title.orEmpty().contains("EPUB3", ignoreCase = true)) "EPUB3" else "EPUB"

    fun preferenceRank(): Int {
        val normalizedTitle = title.orEmpty().lowercase()
        return when {
            "epub3" in normalizedTitle -> 0
            "no images" !in normalizedTitle -> 1
            else -> 2
        }
    }
}

internal object GutenbergOpdsParser {
    fun parseNavigation(xml: String, baseUrl: String): GutenbergNavigationPage {
        val document = parse(xml, baseUrl)
        val feed = document.firstDirectChild("feed") ?: document
        val entries = feed.directChildren("entry").mapNotNull { entry ->
            val ebookId = entry.gutenbergId() ?: return@mapNotNull null
            val title = entry.firstDirectChild("title")?.text()?.trim().orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val authors = entry.directChildren("author")
                .mapNotNull { it.firstDirectChild("name")?.text()?.trim()?.ifBlank { null } }
            val fallbackAuthor = entry.firstDirectChild("content")
                ?.takeIf { it.attr("type").equals("text", ignoreCase = true) }
                ?.text()
                ?.trim()
                ?.ifBlank { null }
            GutenbergNavigationEntry(
                ebookId = ebookId,
                title = title,
                author = authors.joinToString().ifBlank { fallbackAuthor },
            )
        }
        val hasNextPage = feed.directChildren("link").any { it.attr("rel") == "next" }
        return GutenbergNavigationPage(entries, hasNextPage)
    }

    fun parsePublication(xml: String, baseUrl: String, expectedEbookId: String): GutenbergPublication? {
        val document = parse(xml, baseUrl)
        val feed = document.firstDirectChild("feed") ?: document
        val editions = feed.directChildren("entry").filter { it.gutenbergId() == expectedEbookId }
        if (editions.isEmpty()) return null

        val title = editions.firstNotBlank { it.firstDirectChild("title")?.text() } ?: return null
        val authors = editions.asSequence()
            .flatMap { edition -> edition.directChildren("author").asSequence() }
            .mapNotNull { it.firstDirectChild("name")?.text()?.trim()?.ifBlank { null } }
            .distinct()
            .toList()
        val subjects = editions.asSequence()
            .flatMap { it.directChildren("category").asSequence() }
            .filter { it.attr("scheme").endsWith("/LCSH") }
            .map { it.attr("term").trim() }
            .filter(String::isNotBlank)
            .distinct()
            .toList()
        val acquisitions = editions.asSequence()
            .flatMap { it.directChildren("link").asSequence() }
            .filter { it.attr("rel") == ACQUISITION_REL }
            .mapNotNull { link ->
                val href = link.absoluteHref(baseUrl) ?: return@mapNotNull null
                val mediaType = link.attr("type").trim()
                if (mediaType.isBlank()) return@mapNotNull null
                GutenbergAcquisition(
                    href = href,
                    mediaType = mediaType,
                    title = link.attr("title").trim().ifBlank { null },
                    length = link.attr("length").toLongOrNull(),
                )
            }
            .distinctBy(GutenbergAcquisition::href)
            .toList()
        val coverUrl = editions.asSequence()
            .flatMap { it.directChildren("link").asSequence() }
            .filter { it.attr("rel") == IMAGE_REL }
            .mapNotNull { it.absoluteHref(baseUrl) }
            .firstOrNull { it.startsWith("https://") || it.startsWith("http://") }
        val contentParagraphs = editions.asSequence()
            .mapNotNull { it.firstDirectChild("content") }
            .flatMap { it.getElementsByTag("p").asSequence() }
            .map { it.text().trim() }
            .filter(String::isNotBlank)
            .toList()

        return GutenbergPublication(
            ebookId = expectedEbookId,
            title = title,
            authors = authors,
            summary = contentParagraphs.valueAfterLabel("Summary:"),
            subjects = subjects,
            language = editions.firstNotBlank { it.firstDirectChild("dcterms:language")?.text() },
            rights = editions.firstNotBlank { it.firstDirectChild("rights")?.text() }
                ?: contentParagraphs.valueAfterLabel("Rights:"),
            publishedAt = editions.firstNotBlank { it.firstDirectChild("published")?.text() }
                ?.let(::parseTimestamp)
                ?: 0L,
            coverUrl = coverUrl,
            acquisitions = acquisitions,
        )
    }

    private fun parse(xml: String, baseUrl: String): Document =
        Jsoup.parse(xml, baseUrl, Parser.xmlParser())
}

private fun Element.gutenbergId(): String? {
    val candidates = buildList {
        firstDirectChild("id")?.text()?.let(::add)
        directChildren("link").forEach { add(it.attr("href")) }
    }
    return candidates.firstNotNullOfOrNull { value ->
        GUTENBERG_ID_PATTERNS.firstNotNullOfOrNull { it.find(value)?.groupValues?.getOrNull(1) }
    }
}

private fun Element.absoluteHref(baseUrl: String): String? {
    val absolute = absUrl("href").ifBlank {
        runCatching { URI(baseUrl).resolve(attr("href")).toString() }.getOrNull().orEmpty()
    }
    return absolute.ifBlank { null }
}

private fun Element.directChildren(tagName: String): List<Element> =
    children().filter { it.tagName().equals(tagName, ignoreCase = true) }

private fun Element.firstDirectChild(tagName: String): Element? =
    children().firstOrNull { it.tagName().equals(tagName, ignoreCase = true) }

private inline fun List<Element>.firstNotBlank(value: (Element) -> String?): String? =
    firstNotNullOfOrNull { value(it)?.trim()?.ifBlank { null } }

private fun List<String>.valueAfterLabel(label: String): String? =
    firstOrNull { it.startsWith(label, ignoreCase = true) }
        ?.substring(label.length)
        ?.trim()
        ?.ifBlank { null }

private fun parseTimestamp(value: String): Long? =
    runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()

private const val ACQUISITION_REL = "http://opds-spec.org/acquisition"
private const val IMAGE_REL = "http://opds-spec.org/image"
private val GUTENBERG_ID_PATTERNS = listOf(
    Regex("urn:gutenberg:(\\d+)", RegexOption.IGNORE_CASE),
    Regex("/ebooks/(\\d+)", RegexOption.IGNORE_CASE),
)
