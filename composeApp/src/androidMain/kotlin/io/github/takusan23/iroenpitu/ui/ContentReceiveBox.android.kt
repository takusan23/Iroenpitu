package io.github.takusan23.iroenpitu.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.IntentCompat
import io.github.takusan23.iroenpitu.MediaStoreTool
import io.github.takusan23.iroenpitu.PhotoPickerResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun ContentReceiveBox(
    modifier: Modifier,
    onReceive: (PhotoPickerResult) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current
    val context = LocalContext.current

    // Intent で共有相手として選ばれたとき
    LaunchedEffect(key1 = Unit) {
        val launchIntent = activity?.intent ?: return@LaunchedEffect
        val imageUri = IntentCompat.getParcelableExtra(launchIntent, Intent.EXTRA_STREAM, Uri::class.java)

        // データがあれば読み出す
        if (launchIntent.action == Intent.ACTION_SEND && imageUri != null) {
            val pickResult = MediaStoreTool.getImage(context, imageUri) ?: return@LaunchedEffect
            onReceive(pickResult)
        }
    }

    // ドラッグアンドドロップの操作中は true
    // 枠に色を付けたり
    val isProgressDragAndDrop = remember { mutableStateOf(false) }

    val callback = remember {
        object : DragAndDropTarget {

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val androidEvent = event.toAndroidDragEvent()
                val clipData = androidEvent.clipData
                val uri = clipData.getItemAt(0).uri

                // Uri にアクセスしますよ、requestDragAndDropPermissions を呼ぶ
                val dropPermissions = ActivityCompat.requestDragAndDropPermissions(context as Activity, androidEvent)
                if (dropPermissions != null && uri != null) {
                    scope.launch {
                        // データを取得する
                        val pickResult = MediaStoreTool.getImage(context, uri)
                        if (pickResult != null) {
                            onReceive(pickResult)
                        }
                        dropPermissions.release()
                    }
                }

                return true
            }

            override fun onStarted(event: DragAndDropEvent) {
                super.onStarted(event)
                isProgressDragAndDrop.value = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                super.onEnded(event)
                isProgressDragAndDrop.value = false
            }
        }
    }

    Box(
        modifier = modifier
            .then(
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
            )
            .dragAndDropTarget(
                shouldStartDragAndDrop = {
                    it.mimeTypes().any { receiveMimeType -> receiveMimeType in CONTENT_RECEIVE_MIME_TYPE_LIST }
                },
                target = callback
            ),
        content = content
    )
}