package com.blackpirateapps.brownpaper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import coil3.compose.AsyncImage
import com.blackpirateapps.brownpaper.core.util.highlightMatches
import com.blackpirateapps.brownpaper.core.util.toReadableArticleDate
import com.blackpirateapps.brownpaper.domain.model.ArticleSummary

private fun Long.toReadableViews(): String {
    return when {
        this < 1000 -> "$this views"
        this < 1000000 -> String.format("%.1fK views", this / 1000f).replace(".0K", "K")
        else -> String.format("%.1fM views", this / 1000000f).replace(".0M", "M")
    }
}

@Composable
fun ArticleCard(
    article: ArticleSummary,
    searchQuery: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!article.heroImageUrl.isNullOrBlank()) {
                Box {
                    AsyncImage(
                        model = article.heroImageUrl,
                        contentDescription = article.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    if (article.isVideo) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center).height(48.dp).fillMaxWidth(0.3f),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                            if (!article.videoRuntimeText.isNullOrBlank()) {
                                Text(
                                    text = article.videoRuntimeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                )
            }

            val highlightColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f)
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = article.title.highlightMatches(searchQuery, highlightColor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.isVideo && !article.channelName.isNullOrBlank()) {
                    Text(
                        text = "${article.channelName} • ${article.viewCount.toReadableViews()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = article.excerpt.highlightMatches(searchQuery, highlightColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = article.dateAdded.toReadableArticleDate(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (article.isLiked) {
                        Icon(
                            imageVector = Icons.Outlined.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

