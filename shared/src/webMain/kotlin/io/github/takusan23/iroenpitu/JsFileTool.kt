package io.github.takusan23.iroenpitu

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** [File]からバイナリを取得する */
suspend fun File.readBytes() = suspendCoroutine { continuation ->
    val fileReader = FileReader()
    fileReader.onload = {
        val arrayBuffer = fileReader.result as ArrayBuffer
        continuation.resume(Int8Array(arrayBuffer))
    }
    fileReader.readAsArrayBuffer(this)
}
