package io.github.takusan23.iroenpitu.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    onCopy: (AwsS3Client.ListObject) -> Unit,
    onOpenBrowser: (AwsS3Client.ListObject) -> Unit,
    onDelete: (AwsS3Client.ListObject) -> Unit
) {
    val imageUrl = "$baseUrl/${listObject.key}"

    // 削除ダイアログを出すか
    val isShowDeleteDialog = remember { mutableStateOf(false) }
    if (isShowDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { isShowDeleteDialog.value = false },
            icon = {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null
                )
            },
            title = { Text(text = "本当に削除しますか") },
            text = { Text(text = listObject.key) },
            confirmButton = {
                Button(onClick = { onDelete(listObject) }) {
                    Text(text = "削除する")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { isShowDeleteDialog.value = false }) {
                    Text(text = "閉じる")
                }
            }
        )
    }

    OutlinedCard(modifier) {
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
            IconButton(onClick = { onCopy(listObject) }) {
                Icon(
                    painter = painterResource(Res.drawable.content_paste),
                    contentDescription = null
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