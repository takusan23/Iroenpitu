package io.github.takusan23.iroenpitu

fun interface PhotoPicker {

    /**
     * 写真ピッカーを開く。
     * 選び終わるまで一時停止し、選んだ画像を[PhotoPickerResult]で返す。
     * 選ぶのを辞めたら null を返す
     */
    suspend fun startPhotoPicker(): PhotoPickerResult?
}

expect val photoPicker: PhotoPicker