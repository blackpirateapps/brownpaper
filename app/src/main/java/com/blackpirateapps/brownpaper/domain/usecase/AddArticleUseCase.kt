package com.blackpirateapps.brownpaper.domain.usecase

import com.blackpirateapps.brownpaper.domain.model.AddArticleResult
import com.blackpirateapps.brownpaper.domain.repository.ArticleRepository
import javax.inject.Inject

class AddArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository,
) {
    suspend operator fun invoke(url: String): AddArticleResult = articleRepository.addArticleFromUrl(url)
}

