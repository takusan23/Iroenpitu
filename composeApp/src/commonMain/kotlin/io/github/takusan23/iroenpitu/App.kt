package io.github.takusan23.iroenpitu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iroenpitu.composeapp.generated.resources.KosugiMaru_Regular
import iroenpitu.composeapp.generated.resources.Res
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // API を叩く
    val objectList = remember { mutableStateOf<List<AwsS3Client.ListObject>?>(null) }
    suspend fun loadBucketObjectList() {
        // 設定を読み出す
        val map = preference.load()
        objectList.value = AwsS3Client.getObjectList(
            bucketName = map[Preference.KEY_OUTPUT_BUCKET] ?: return
        )
    }
    // 初回読み込み
    LaunchedEffect(key1 = Unit) {
        loadBucketObjectList()
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
                    title = { Text(text = "AWS S3 オブジェクト一覧") },
                    actions = {
                        IconButton(onClick = { isShowSettingBottomSheet.value = true }) {
                            Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text(text = "画像を投稿") },
                    icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
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

            LazyColumn(contentPadding = innerPadding) {
                if (objectList.value == null) {
                    // 読み込み中
                    item {
                        LoadingCenterBox()
                    }
                } else {
                    // 一覧画面
                    items(
                        items = objectList.value!!,
                        key = { it.key }
                    ) { obj ->
                        Column(
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxWidth()
                        ) {
                            Text(text = obj.key, fontSize = 16.sp)
                            Text(text = obj.lastModified)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** 真ん中に読み込み中のグルグル出す */
@Composable
private fun LoadingCenterBox() {
    Box(
        modifier = Modifier
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
        }
    }
}

/** 初期値を受け入れて、あとは */
@Composable
private fun RememberOutlinedTextField(
    modifier: Modifier = Modifier,
    initValue: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    val currentText = remember { mutableStateOf(initValue) }
    OutlinedTextField(
        modifier = modifier,
        value = currentText.value,
        label = { Text(text = label) },
        onValueChange = {
            currentText.value = it
            onValueChange(it)
        }
    )
}
