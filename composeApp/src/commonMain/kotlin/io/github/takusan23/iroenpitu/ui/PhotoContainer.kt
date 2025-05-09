package io.github.takusan23.iroenpitu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.takusan23.iroenpitu.AwsS3Client
import iroenpitu.composeapp.generated.resources.Res
import iroenpitu.composeapp.generated.resources.content_paste
import iroenpitu.composeapp.generated.resources.delete
import iroenpitu.composeapp.generated.resources.open_in_browser
import org.jetbrains.compose.resources.painterResource

/** グリッドの写真コンテナ */
@Composable
fun PhotoContainer(
    modifier: Modifier = Modifier,
    listObject: AwsS3Client.ListObject,
    baseUrl: String,
    onCopy: (copyText: String) -> Unit,
    onOpenBrowser: (AwsS3Client.ListObject) -> Unit,
    onDelete: (AwsS3Client.ListObject) -> Unit
) {
    val imageUrl = "$baseUrl/${listObject.key}"

    // 削除ダイアログを出すか
    val isShowDeleteDialog = remember { mutableStateOf(false) }
    if (isShowDeleteDialog.value) {
        DeleteDialog(
            onDismissRequest = { isShowDeleteDialog.value = false },
            imageUrl = imageUrl,
            listObject = listObject,
            onDelete = { onDelete(listObject) }
        )
    }

    // プレビュー Dialog
    val isShowPreviewDialog = remember { mutableStateOf(false) }
    if (isShowPreviewDialog.value) {
        PreviewDialog(
            onDismissRequest = { isShowPreviewDialog.value = false },
            imageUrl = imageUrl
        )
    }

    // コピー時に何をコピーするか
    val isShowCopyMenu = remember { mutableStateOf(false) }
    fun invokeOnCopy(copyText: String) {
        onCopy(copyText)
        isShowCopyMenu.value = false
    }

    OutlinedCard(modifier = modifier.clickable { isShowPreviewDialog.value = true }) {
        Text(
            modifier = Modifier.padding(horizontal = 5.dp),
            text = listObject.key,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false
        )
        HorizontalDivider()

        AsyncImage(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        HorizontalDivider()
        Row {

            IconButton(onClick = { isShowCopyMenu.value = true }) {
                Icon(
                    painter = painterResource(Res.drawable.content_paste),
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = isShowCopyMenu.value,
                onDismissRequest = { isShowCopyMenu.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = "Markdown に貼り付け") },
                    onClick = { invokeOnCopy("""![Imgur]($imageUrl)""") }
                )
                DropdownMenuItem(
                    text = { Text(text = "HTML に貼り付け") },
                    onClick = { invokeOnCopy("""<img src="$imageUrl">""") }
                )
                DropdownMenuItem(
                    text = { Text(text = "URL をコピー") },
                    onClick = { invokeOnCopy(imageUrl) }
                )
            }

            IconButton(onClick = { onOpenBrowser(listObject) }) {
                Icon(
                    painter = painterResource(Res.drawable.open_in_browser),
                    contentDescription = null
                )
            }
            IconButton(onClick = { isShowDeleteDialog.value = true }) {
                Icon(
                    painter = painterResource(Res.drawable.delete),
                    contentDescription = null
                )
            }
        }
    }
}

/**
 * 削除ダイアログ
 */
@Composable
private fun DeleteDialog(
    onDismissRequest: () -> Unit,
    listObject: AwsS3Client.ListObject,
    imageUrl: String,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            AsyncImage(
                model = imageUrl,
                contentDescription = null
            )
        },
        title = { Text(text = "本当に削除しますか") },
        text = { Text(text = listObject.key) },
        confirmButton = {
            Button(onClick = onDelete) {
                Text(text = "削除する")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = "閉じる")
            }
        }
    )
}

/**
 * プレビューダイアログ
 *
 * @param onDismissRequest 閉じる要求
 * @param imageUrl 画像 URL
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewDialog(
    onDismissRequest: () -> Unit,
    imageUrl: String
) {
    // ピンチイン・ピンチアウト、移動
    val offset = remember { mutableStateOf(Offset.Zero) }
    val scale = remember { mutableStateOf(1f) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale.value *= zoomChange
        offset.value += offsetChange
    }

    // 余分なものがない Dialog
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Card {
            Column(
                modifier = Modifier.padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                AsyncImage(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            translationX = offset.value.x
                            translationY = offset.value.y
                        }
                        .transformable(state)
                        .aspectRatio(1f)
                        .fillMaxWidth(),
                    model = imageUrl,
                    contentDescription = null
                )
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = onDismissRequest
                ) {
                    Text(text = "閉じる")
                }
            }
        }
    }
}
