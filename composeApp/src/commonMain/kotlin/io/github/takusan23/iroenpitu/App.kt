package io.github.takusan23.iroenpitu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iroenpitu.composeapp.generated.resources.KosugiMaru_Regular
import iroenpitu.composeapp.generated.resources.Res
import iroenpitu.composeapp.generated.resources.content_paste
import iroenpitu.composeapp.generated.resources.delete
import iroenpitu.composeapp.generated.resources.open_in_browser
import iroenpitu.composeapp.generated.resources.refresh
import iroenpitu.composeapp.generated.resources.settings
import iroenpitu.composeapp.generated.resources.upload
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val baseUrl = remember { mutableStateOf("") }

    // API を叩く
    val objectList = remember { mutableStateOf<List<AwsS3Client.ListObject>?>(null) }
    suspend fun loadBucketObjectList() {
        // 設定を読み出す
        val map = preference.load()
        objectList.value = AwsS3Client.getObjectList(
            bucketName = map[Preference.KEY_OUTPUT_BUCKET] ?: return
        )
    }

    // 初回
    LaunchedEffect(key1 = Unit) {
        // API を叩く
        loadBucketObjectList()

        // 画像配信（お絵かき帳）ベース URL
        val map = preference.load()
        baseUrl.value = map[Preference.KEY_OEKAKITYOU_BASE_URL] ?: ""
    }

    // Web で日本語を表示できないので、MaterialTheme でフォントを伝搬させる
    val bundleFont = FontFamily(Font(resource = Res.font.KosugiMaru_Regular))
    val overrideFontFamily = MaterialTheme.typography.copy(
        displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = bundleFont),
        displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = bundleFont),
        displaySmall = MaterialTheme.typography.displaySmall.copy(fontFamily = bundleFont),
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = bundleFont),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = bundleFont),
        headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontFamily = bundleFont),
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = bundleFont),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = bundleFont),
        titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = bundleFont),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = bundleFont),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = bundleFont),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = bundleFont),
        labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = bundleFont),
        labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = bundleFont),
        labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = bundleFont),
    )

    MaterialTheme(typography = overrideFontFamily) {

        // 設定変更ボトムシート
        val isShowSettingBottomSheet = remember { mutableStateOf(false) }
        if (isShowSettingBottomSheet.value) {
            SettingBottomSheet(
                onDismissRequest = { isShowSettingBottomSheet.value = false }
            )
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                TopAppBar(
                    title = { Text(text = "お絵かき帳 管理画面") },
                    actions = {
                        IconButton(onClick = { scope.launch { loadBucketObjectList() } }) {
                            Icon(painter = painterResource(Res.drawable.refresh), contentDescription = null)
                        }
                        IconButton(onClick = { isShowSettingBottomSheet.value = true }) {
                            Icon(painter = painterResource(Res.drawable.settings), contentDescription = null)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text(text = "画像を投稿") },
                    icon = { Icon(painter = painterResource(Res.drawable.upload), contentDescription = null) },
                    onClick = {
                        scope.launch {
                            // 設定を読み出す
                            val map = preference.load()
                            // 投稿処理
                            val (name, byteArray) = photoPicker.startPhotoPicker() ?: return@launch
                            // S3 に投げる
                            val isSuccessful = AwsS3Client.putObject(
                                bucketName = map[Preference.KEY_INPUT_BUCKET] ?: return@launch,
                                key = name,
                                byteArray = byteArray
                            )
                            snackbarHostState.showSnackbar(
                                message = if (isSuccessful) "成功しました。変換をお待ち下さい。" else "失敗しました。"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->

            // 読み込み中
            if (objectList.value == null) {
                LoadingCenterBox(
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                // 画面サイズに合わせてセル数調整
                BoxWithConstraints(modifier = Modifier.padding(horizontal = 5.dp)) {
                    LazyVerticalGrid(
                        contentPadding = innerPadding,
                        columns = GridCells.Fixed((this.maxWidth / 200.dp).toInt()),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // 一覧画面
                        items(
                            items = objectList.value!!,
                            key = { it.key }
                        ) { obj ->
                            // 各写真
                            PhotoContainer(
                                listObject = obj,
                                baseUrl = baseUrl.value,
                                onCopy = {
                                    scope.launch {
                                        clipboard.setText(AnnotatedString(text = "${baseUrl.value}/${it.key}"))
                                        snackbarHostState.showSnackbar("コピーしました")
                                    }
                                },
                                onOpenBrowser = {
                                    uriHandler.openUri(uri = "${baseUrl.value}/${it.key}")
                                },
                                onDelete = {
                                    scope.launch {
                                        // 消す
                                        val map = preference.load()
                                        val isSuccessful = AwsS3Client.deleteObject(
                                            bucketName = map[Preference.KEY_OUTPUT_BUCKET] ?: return@launch,
                                            key = it.key
                                        )
                                        // 再読み込み
                                        loadBucketObjectList()
                                        snackbarHostState.showSnackbar(
                                            message = if (isSuccessful) "削除しました" else "問題が発生しました"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 真ん中に読み込み中のグルグル出す */
@Composable
private fun LoadingCenterBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(100.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * AWS の認証情報を入力するためのボトムシート
 *
 * @param onDismissRequest 消す要求が来た
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingBottomSheet(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settingMap = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(key1 = Unit) {
        preference.load().forEach { (key, value) -> settingMap[key] = value }
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row {
                Text(
                    text = "初期設定",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    scope.launch { preference.update(settingMap) }
                    onDismissRequest()
                }) { Text(text = "保存") }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_ACCESS_KEY] ?: "",
                onValueChange = { settingMap[Preference.KEY_ACCESS_KEY] = it },
                label = { Text(text = "アクセスキー") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_SECRET_ACCESS_LEY] ?: "",
                onValueChange = { settingMap[Preference.KEY_SECRET_ACCESS_LEY] = it },
                label = { Text(text = "シークレットアクセスキー") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_REGION] ?: "",
                onValueChange = { settingMap[Preference.KEY_REGION] = it },
                label = { Text(text = "リージョン") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_INPUT_BUCKET] ?: "",
                onValueChange = { settingMap[Preference.KEY_INPUT_BUCKET] = it },
                label = { Text(text = "画像受け付け用バケット名") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_OUTPUT_BUCKET] ?: "",
                onValueChange = { settingMap[Preference.KEY_OUTPUT_BUCKET] = it },
                label = { Text(text = "画像配信用バケット名") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = settingMap[Preference.KEY_OEKAKITYOU_BASE_URL] ?: "",
                onValueChange = { settingMap[Preference.KEY_OEKAKITYOU_BASE_URL] = it },
                label = { Text(text = "おえかきちょう ベース URL") }
            )
        }
    }
}

/** グリッドの写真コンテナ */
@Composable
private fun PhotoContainer(
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