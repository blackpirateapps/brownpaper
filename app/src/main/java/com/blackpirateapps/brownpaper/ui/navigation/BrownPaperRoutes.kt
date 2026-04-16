package com.blackpirateapps.brownpaper.ui.navigation

import com.blackpirateapps.brownpaper.domain.model.ArticleListSource

object BrownPaperRoutes {
    const val listTemplate = "list/{source}/{sourceId}"
    const val readerTemplate = "reader/{articleId}"

    fun listRoute(source: ArticleListSource, sourceId: Long = -1L): String =
        "list/${source.routeValue}/$sourceId"

    fun readerRoute(articleId: Long): String = "reader/$articleId"
}

