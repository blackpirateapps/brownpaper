package com.blackpirateapps.brownpaper.data.parser

import javax.inject.Inject
import org.jsoup.Jsoup
import net.dankito.readability4j.Readability4J

data class ParsedArticle(
    val title: String,
    val extractedTextContent: String,
    val extractedHeroImageUrl: String?,
    val isVideo: Boolean = false,
    val youtubeVideoId: String? = null,
    val videoRuntimeText: String? = null,
    val channelName: String? = null,
    val viewCount: Long = 0L,
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

        val isYouTube = url.contains("youtube.com/watch") || url.contains("youtu.be/")
        var youtubeVideoId: String? = null
        var videoRuntimeText: String? = null
        var channelName: String? = null
        var viewCount = 0L
        
        if (isYouTube) {
            val html = document.html()
            
            youtubeVideoId = """"videoId":"([^"]+)"""".toRegex().find(html)?.groupValues?.get(1) 
                ?: url.substringAfter("v=").substringBefore("&")
                    .takeIf { it.length in 10..12 && it != url }
                ?: url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                    .takeIf { it.length in 10..12 && it != url }
            
            val durationSecs = """"lengthSeconds":"(\d+)"""".toRegex().find(html)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            if (durationSecs > 0) {
                videoRuntimeText = String.format("%d:%02d", durationSecs / 60, durationSecs % 60)
            }
            
            viewCount = document.select("meta[itemprop=interactionCount]").attr("content").toLongOrNull()
                ?: """"viewCount":"(\d+)"""".toRegex().find(html)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                
            channelName = document.select("span[itemprop=author] link[itemprop=name]").attr("content").takeIf { it.isNotBlank() }
                ?: """"author":"([^"]+)"""".toRegex().find(html)?.groupValues?.get(1)
        }

        val readability4J = Readability4J(url, document)
        val article = readability4J.parse()

        val title = article.title?.takeIf { it.isNotBlank() } ?: fallbackTitle

        // Readability4J's textContent lacks formatting. We use the DOM structure.
        val bodyText = article.articleContent?.let { contentElement ->
            contentElement.select("p, h1, h2, h3, h4, h5, h6, li, img")
                .map { element ->
                    if (element.tagName() == "img") {
                        val src = element.absUrl("src")
                        if (src.isNotBlank()) "![img]($src)" else ""
                    } else {
                        element.text().trim()
                    }
                }
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
        }?.takeIf { it.isNotBlank() } ?: article.textContent?.trim() ?: document.body().text()

        return ParsedArticle(
            title = title,
            extractedTextContent = bodyText,
            extractedHeroImageUrl = heroImage,
            isVideo = isYouTube,
            youtubeVideoId = youtubeVideoId,
            videoRuntimeText = videoRuntimeText,
            channelName = channelName,
            viewCount = viewCount,
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

