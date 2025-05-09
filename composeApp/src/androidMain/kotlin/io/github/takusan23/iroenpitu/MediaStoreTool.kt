package io.github.takusan23.iroenpitu

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android の MediaStore */
object MediaStoreTool {

    /** Uri から画像を取得する */
    suspend fun getImage(
        context: Context,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        // 名前は取得できないので、適当に作る
        val extension = when (context.contentResolver.getType(uri)) {
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> "/webp"
            else -> null
        }
        if (extension != null) {
            PhotoPickerResult(
                name = "${System.currentTimeMillis()}$extension",
                byteArray = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            )
        } else {
            null
        }
    }

}