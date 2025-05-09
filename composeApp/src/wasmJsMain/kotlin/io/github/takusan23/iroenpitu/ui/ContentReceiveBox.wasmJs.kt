package io.github.takusan23.iroenpitu.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.takusan23.iroenpitu.PhotoPickerResult
import io.github.takusan23.iroenpitu.readBytes
import io.ktor.util.toByteArray
import kotlinx.browser.document
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.w3c.files.File
import org.w3c.files.get

private sealed interface DragAndDropEvent {
    data object Over : DragAndDropEvent
    data object Leave : DragAndDropEvent
    data class Drop(val file: File?) : DragAndDropEvent
}

/** document.onpaste コールバック関数を Flow に */
private val documentPasteFlow = callbackFlow {
    document.onpaste = { clipboardEvent ->
        clipboardEvent.preventDefault()
        trySend(clipboardEvent.clipboardData?.files?.get(0))
    }
    awaitClose { document.onpaste = null }
}

/** document ドラッグアンドドロップのコールバック関数を Flow に */
private val documentDragAndDropFlow = callbackFlow {
    document.ondrop = { dragEvent ->
        dragEvent.preventDefault()
        trySend(DragAndDropEvent.Drop(dragEvent.dataTransfer?.files?.get(0)))
    }
    document.ondragover = { dragEvent ->
        dragEvent.preventDefault()
        trySend(DragAndDropEvent.Over)
    }
    document.ondragleave = { dragEvent ->
        dragEvent.preventDefault()
        trySend(DragAndDropEvent.Leave)
    }
    awaitClose {
        document.ondrop = null
        document.ondragover = null
        document.ondragleave = null
    }
}

@Composable
actual fun ContentReceiveBox(
    modifier: Modifier,
    onReceive: (PhotoPickerResult) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    // ドラッグアンドドロップの操作中は true
    // 枠に色を付けたり
    val isProgressDragAndDrop = remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        // ペースト操作を購読。画像ファイルなら
        launch {
            documentPasteFlow
                .filter { it?.type in CONTENT_RECEIVE_MIME_TYPE_LIST }
                .collect { file ->
                    file ?: return@collect
                    onReceive(
                        PhotoPickerResult(
                            name = file.name,
                            byteArray = file.readBytes().toByteArray()
                        )
                    )
                }
        }

        // ドラッグアンドドロップ
        launch {
            documentDragAndDropFlow.collect { event ->
                when (event) {

                    // 放り込まれた。画像ファイルなら
                    is DragAndDropEvent.Drop -> {
                        val file = event.file
                        if (file != null && file.type in CONTENT_RECEIVE_MIME_TYPE_LIST) {
                            onReceive(
                                PhotoPickerResult(
                                    name = file.name,
                                    byteArray = file.readBytes().toByteArray()
                                )
                            )
                        }
                        isProgressDragAndDrop.value = false
                    }

                    // 枠線の ON/OFF
                    DragAndDropEvent.Leave -> isProgressDragAndDrop.value = false
                    DragAndDropEvent.Over -> isProgressDragAndDrop.value = true
                }
            }
        }
    }

    Box(
        modifier = modifier.then(
            if (isProgressDragAndDrop.value) {

                // 枠の色。アニメーションするよう
                val targetColor = MaterialTheme.colorScheme.primary
                val infiniteTransition = rememberInfiniteTransition()
                val animateBorderColor = infiniteTransition.animateColor(
                    initialValue = Color.Transparent,
                    targetValue = targetColor,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Modifier
                    .border(
                        width = 10.dp,
                        color = animateBorderColor.value
                    )
                    .background(
                        color = targetColor.copy(alpha = 0.3f)
                    )
            } else Modifier
        ),
        content = content
    )
}