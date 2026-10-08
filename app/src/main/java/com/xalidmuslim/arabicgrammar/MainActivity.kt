package com.xalidmuslim.arabicgrammar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xalidmuslim.arabicgrammar.data.BookRepository
import com.xalidmuslim.arabicgrammar.data.ProgressStore
import com.xalidmuslim.arabicgrammar.ui.ArabicGrammarApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val progress = ProgressStore(this)
        setContent {
            var repository by remember { mutableStateOf<BookRepository?>(null) }
            LaunchedEffect(Unit) {
                repository = withContext(Dispatchers.IO) { BookRepository(applicationContext) }
            }
            AnimatedContent(
                targetState = repository,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(70)) },
                label = "startup"
            ) { repo ->
                if (repo == null) {
                    Box(Modifier.fillMaxSize().background(Color(0xFFF5F1E8)))
                } else {
                    ArabicGrammarApp(repository = repo, progress = progress)
                }
            }
        }
    }
}
