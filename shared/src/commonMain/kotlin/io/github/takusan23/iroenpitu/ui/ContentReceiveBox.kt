package io.github.takusan23.iroenpitu.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.takusan23.iroenpitu.PhotoPickerResult

/** 受け入れる MIME-Type　*/
val CONTENT_RECEIVE_MIME_TYPE_LIST = arrayOf("image/jpeg", "image/png", "image/webm")

/**
 * 各プラットフォーム向けに、ペースト操作や、ドラッグアンドドロップが実装できるようにする
 *
 * @param modifier [Modifier]
 * @param onReceive データを受け取ったら呼ばれる
 * @param content この Box を親にするので、子
 */
@Composable
expect fun ContentReceiveBox(
    modifier: Modifier = Modifier,
    onReceive: (List<PhotoPickerResult>) -> Unit,
    content: @Composable BoxScope.() -> Unit
)