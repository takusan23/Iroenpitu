package io.github.takusan23.iroenpitu

import io.ktor.util.toByteArray
import kotlinx.browser.document
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** <input> */
val inputElement = (document.createElement("input") as HTMLInputElement).apply {
    setAttribute("type", "file")
    setAttribute("accept", ".jpg, .png, .webp")
}

/** ファイル取得 Flow */
val inputChangeEventFlow = callbackFlow {
    // ファイル選択イベント
    inputElement.onchange = {
        trySend(inputElement.files?.get(0))
        Unit
    }
    // 選択画面を閉じたイベント
    inputElement.oncancel = {
        trySend(null)
    }
    // ファイル
    awaitClose {
        inputElement.onchange = null
        inputElement.oncancel = null
    }
}

/**
 * Web 側
 * 写真ピッカーの処理
 */

actual val photoPicker = PhotoPicker {
    // 開く
    inputElement.click()

    // 選ぶのを待つ
    val file = inputChangeEventFlow.firstOrNull()

    // 返す
    if (file == null) {
        null
    } else {
        PhotoPicker.PhotoPickerResult(
            name = file.name,
            byteArray = file.readBytes().toByteArray()
        )
    }
}

/** [File]からバイナリを取得する */
private suspend fun File.readBytes() = suspendCoroutine { continuation ->
    val fileReader = FileReader()
    fileReader.onload = {
        val arrayBuffer = fileReader.result as ArrayBuffer
        continuation.resume(Int8Array(arrayBuffer))
    }
    fileReader.readAsArrayBuffer(this)
}