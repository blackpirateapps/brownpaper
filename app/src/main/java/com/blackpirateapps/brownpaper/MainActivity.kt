package com.blackpirateapps.brownpaper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.blackpirateapps.brownpaper.ui.navigation.BrownPaperApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sharedUrlEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialSharedUrl = intent.extractSharedUrl()

        setContent {
            val sharedUrlFlow = remember { sharedUrlEvents.asSharedFlow() }
            BrownPaperApp(
                initialSharedUrl = initialSharedUrl,
                sharedUrlFlow = sharedUrlFlow,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.extractSharedUrl()?.let(sharedUrlEvents::tryEmit)
    }
}

private val sharedUrlRegex = Regex("""https?://\S+|www\.\S+""")

private fun Intent.extractSharedUrl(): String? {
    if (action != Intent.ACTION_SEND || type != "text/plain") {
        return null
    }

    val rawText = getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
    if (rawText.isBlank()) {
        return null
    }

    return sharedUrlRegex.find(rawText)?.value ?: rawText
}

