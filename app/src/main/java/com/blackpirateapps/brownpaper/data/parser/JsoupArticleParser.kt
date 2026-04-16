package com.blackpirateapps.brownpaper.data.parser

import javax.inject.Inject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

data class ParsedArticle(
    val title: String,
    val extractedTextContent: String,
    val extractedHeroImageUrl: String?,
)

class JsoupArticleParser @Inject constructor() {
    fun parse(url: String): ParsedArticle {
        val document = Jsoup
            .connect(url)
            .userAgent("BrownPaper/1.0")
            .timeout(15_000)
            .get()

        val contentRoot = findContentRoot(document).also(::stripChrome)
        val title = findTitle(document, url)
        val heroImage = findHeroImage(document, contentRoot)
        val bodyText = extractBodyText(contentRoot)

        return ParsedArticle(
            title = title,
            extractedTextContent = bodyText.ifBlank { document.body().text() },
            extractedHeroImageUrl = heroImage,
        )
    }

    private fun findContentRoot(document: Document): Element {
        val candidates = listOf(
            "article",
            "main",
            "[role=main]",
            ".post-content",
            ".entry-content",
            ".article-body",
            ".article-content",
            ".story-body",
        )

        return candidates
            .firstNotNullOfOrNull(document::selectFirst)
            ?: document.body()
    }

    private fun stripChrome(element: Element) {
        element.select(
            "script, style, nav, header, footer, aside, form, iframe, noscript, .nav, .menu, .sidebar, .share, .social, .advertisement, .ads, .newsletter",
        ).remove()
    }

    private fun findTitle(document: Document, fallbackUrl: String): String {
        return document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            .ifBlank { document.selectFirst("title")?.text().orEmpty().trim() }
            .ifBlank { fallbackUrl }
    }

    private fun findHeroImage(document: Document, contentRoot: Element): String? {
        return document.selectFirst("meta[property=og:image]")?.attr("content")?.trim().orEmpty()
            .ifBlank { contentRoot.selectFirst("img[src]")?.absUrl("src").orEmpty() }
            .ifBlank { null }
    }

    private fun extractBodyText(contentRoot: Element): String {
        val blocks = contentRoot
            .select("h1, h2, h3, p, li, blockquote, pre")
            .map(Element::text)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

        return if (blocks.isNotEmpty()) {
            blocks.joinToString(separator = "\n\n")
        } else {
            contentRoot.text().trim()
        }
    }
}

