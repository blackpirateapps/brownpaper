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

        val readability4J = Readability4J(url, document)
        val article = readability4J.parse()

        val title = article.title ?: findTitle(document, url)
        val content = article.textContent ?: document.body().text()
        val heroImage = article.byline ?: findHeroImage(document) // readability4j doesn't have a direct hero image field usually, but let's fallback to our Jsoup logic if needed.

        // Actually, article.content is the HTML content. We want text but with paragraphs.
        // Readability4J's textContent is a bit flat.
        // Let's use the content HTML and strip it better or use its structure.
        
        val bodyText = article.articleContent?.let { contentElement ->
            contentElement.select("p, h1, h2, h3, h4, h5, h6, li").joinToString("\n\n") { it.text() }
        } ?: content

        return ParsedArticle(
            title = title,
            extractedTextContent = bodyText,
            extractedHeroImageUrl = findHeroImage(document),
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

