package io.github.takusan23.iroenpitu

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Android の PhotoPicker を開くことを通達する Channel */
val openPlatformPhotoPickerSignalChannel = Channel<Unit>()

/** Android の PhotoPicker の選択結果を通達する Channel */
val resultPlatformPhotoPickerSignalChannel = Channel<PhotoPickerResult?>()

/**
 * Android 側
 * 写真ピッカーの処理
 */
actual val photoPicker = PhotoPicker {
    // 開くことを要求
    openPlatformPhotoPickerSignalChannel.send(Unit)
    // 結果が送られてくるまで待つ
    resultPlatformPhotoPickerSignalChannel.receive()
}

/** [PhotoPicker]を利用するためにこの関数を呼び出してください。 */
@Composable
fun PhotoPickerInitEffect() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val platformPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            scope.launch(Dispatchers.IO) {
                // 選んでない
                if (uri == null) {
                    resultPlatformPhotoPickerSignalChannel.send(null)
                    return@launch
                }
                // バイナリを取得して返す
                resultPlatformPhotoPickerSignalChannel.send(MediaStoreTool.getImage(context, uri))
            }
        }
    )

    LaunchedEffect(key1 = Unit) {
        // 来たら開く
        for (unuse in openPlatformPhotoPickerSignalChannel) {
            platformPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}