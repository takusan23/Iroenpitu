package io.github.takusan23.iroenpitu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.takusan23.iroenpitu.ui.ContentReceiveBox
import io.github.takusan23.iroenpitu.ui.PhotoContainer
import io.github.takusan23.iroenpitu.ui.SettingBottomSheet
import iroenpitu.composeapp.generated.resources.KosugiMaru_Regular
import iroenpitu.composeapp.generated.resources.Res
import iroenpitu.composeapp.generated.resources.refresh
import iroenpitu.composeapp.generated.resources.settings
import iroenpitu.composeapp.generated.resources.upload
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
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
    val viewModel = remember { AppViewModel(scope) }
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState = viewModel.uiState.collectAsState()

    // Snackbar 監視
    LaunchedEffect(key1 = Unit) {
        viewModel.uiState
            .mapNotNull { it.snackbarMessage }
            .collectLatest { message ->
                snackbarHostState.showSnackbar(message)
                viewModel.deleteSnackbar()
            }
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
                        IconButton(onClick = { scope.launch { viewModel.reloadBucketObjectList() } }) {
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
                            // 投稿処理
                            val (name, byteArray) = photoPicker.startPhotoPicker() ?: return@launch
                            // S3 に投げる
                            viewModel.putObject(name, byteArray)
                        }
                    }
                )
            }
        ) { innerPadding ->

            if (uiState.value.loadState == AppUiState.LoadState.Init) {
                LoadingCenterBox(
                    modifier = Modifier.padding(innerPadding)
                )
            } else {

                // ドラッグアンドドロップを受け入れる
                ContentReceiveBox(
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                    onReceive = { (name, byteArray) -> viewModel.putObject(name, byteArray) }
                ) {

                    // 引っ張って更新
                    PullToRefreshBox(
                        isRefreshing = uiState.value.loadState == AppUiState.LoadState.Reload,
                        onRefresh = { viewModel.reloadBucketObjectList() }
                    ) {

                        // 画面サイズに合わせてセル数調整
                        BoxWithConstraints {

                            // グリッド表示
                            LazyVerticalGrid(
                                contentPadding = PaddingValues(
                                    top = 0.dp,
                                    bottom = innerPadding.calculateBottomPadding(),
                                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp),
                                columns = GridCells.Fixed((this.maxWidth / 200.dp).toInt()),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                // 一覧画面
                                items(
                                    items = uiState.value.photoList,
                                    key = { it.key }
                                ) { obj ->
                                    // 各写真
                                    val baseUrl = uiState.value.baseUrl ?: ""
                                    PhotoContainer(
                                        listObject = obj,
                                        baseUrl = baseUrl,
                                        onCopy = {
                                            scope.launch {
                                                clipboard.setText(AnnotatedString(text = "$baseUrl/${it.key}"))
                                                snackbarHostState.showSnackbar("コピーしました")
                                            }
                                        },
                                        onOpenBrowser = {
                                            uriHandler.openUri(uri = "$baseUrl/${it.key}")
                                        },
                                        onDelete = {
                                            viewModel.deleteObject(it.key)
                                        }
                                    )
                                }
                            }
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
