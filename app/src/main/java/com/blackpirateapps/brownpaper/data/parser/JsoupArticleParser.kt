package com.blackpirateapps.brownpaper.data.parser

import javax.inject.Inject
import org.jsoup.Jsoup
import net.dankito.readability4j.Readability4J

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

        // Extract metadata BEFORE Readability4J potentially mutates or strips the document
        val fallbackTitle = findTitle(document, url)
        val heroImage = findHeroImage(document)

        val readability4J = Readability4J(url, document)
        val article = readability4J.parse()

        val title = article.title?.takeIf { it.isNotBlank() } ?: fallbackTitle

        // Readability4J's textContent lacks formatting. We use the DOM structure.
        val bodyText = article.articleContent?.let { contentElement ->
            contentElement.select("p, h1, h2, h3, h4, h5, h6, li")
                .map { it.text().trim() }
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
        }?.takeIf { it.isNotBlank() } ?: article.textContent?.trim() ?: document.body().text()

        return ParsedArticle(
            title = title,
            extractedTextContent = bodyText,
            extractedHeroImageUrl = heroImage,
        )
    }

    private fun findTitle(document: org.jsoup.nodes.Document, fallbackUrl: String): String {
        return document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            .ifBlank { document.selectFirst("title")?.text().orEmpty().trim() }
            .ifBlank { fallbackUrl }
    }

    private fun findHeroImage(document: org.jsoup.nodes.Document): String? {
        return document.selectFirst("meta[property=og:image]")?.attr("content")?.trim().orEmpty()
            .ifBlank { document.selectFirst("article img[src], main img[src], [role=main] img[src]")?.absUrl("src").orEmpty() }
            .ifBlank { null }
    }
}

