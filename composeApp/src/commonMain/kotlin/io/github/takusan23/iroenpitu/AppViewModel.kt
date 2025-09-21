package io.github.takusan23.iroenpitu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ロジックを切り出す */
class AppViewModel(private val scope: CoroutineScope) {
    private val _uiState = MutableStateFlow(AppUiState())

    // 設定から読み出した値
    private var accessKey: String? = null
    private var secretAccessKey: String? = null
    private var region: String? = null
    private var inputBucket: String? = null
    private var outputBucket: String? = null
    private var oekakityouBaseUrl: String? = null

    /** [AppUiState] */
    val uiState = _uiState.asStateFlow()

    init {
        scope.launch {
            // 設定を読み出す
            val settingMap = preference.load()
            accessKey = settingMap[Preference.KEY_ACCESS_KEY] ?: return@launch
            secretAccessKey = settingMap[Preference.KEY_SECRET_ACCESS_LEY] ?: return@launch
            region = settingMap[Preference.KEY_REGION] ?: return@launch
            inputBucket = settingMap[Preference.KEY_INPUT_BUCKET] ?: return@launch
            outputBucket = settingMap[Preference.KEY_OUTPUT_BUCKET] ?: return@launch
            oekakityouBaseUrl = settingMap[Preference.KEY_OEKAKITYOU_BASE_URL] ?: return@launch

            // 初回読み込み
            _uiState.update { before ->
                before.copy(
                    loadState = AppUiState.LoadState.Init,
                    baseUrl = oekakityouBaseUrl
                )
            }
            loadBucketObjectList()
        }
    }

    /** バケット内のオブジェクト一覧を取得 */
    fun loadBucketObjectList() {
        scope.launch {
            // API
            val objectList = AwsS3Client.getObjectList(
                bucketName = outputBucket ?: return@launch
            )
            // 更新
            _uiState.update { before ->
                before.copy(
                    loadState = AppUiState.LoadState.Loaded,
                    photoList = objectList
                )
            }
        }
    }

    /** バケットにオブジェクトを投稿する */
    fun putObject(key: String, byteArray: ByteArray) {
        scope.launch {
            // ぐるぐる
            _uiState.update { before -> before.copy(isObjectUploading = true) }
            // 投稿
            val isSuccessful = AwsS3Client.putObject(
                bucketName = inputBucket ?: return@launch,
                key = key,
                byteArray = byteArray
            )
            // 更新
            _uiState.update { before ->
                before.copy(
                    isObjectUploading = false,
                    snackbarMessage = if (isSuccessful) "投稿しました。変換が終わるまでお待ち下さい。" else "失敗しました。"
                )
            }
        }
    }

    /** バケットにまとめて投稿する */
    fun putObjectInList(list: List<PhotoPickerResult>) {

        // 一個だけの場合は inList じゃない方を
        list.singleOrNull()?.also { (key, byteArray) ->
            putObject(key, byteArray)
            return
        }

        scope.launch {
            // ぐるぐる
            _uiState.update { before -> before.copy(isObjectUploading = true) }
            // 投稿
            // todo バルクアップロードの API があるかも
            val successList = list.map { (name, byteArray) ->
                AwsS3Client.putObject(
                    bucketName = inputBucket ?: return@launch,
                    key = name,
                    byteArray = byteArray
                )
            }
            // 更新
            _uiState.update { before ->
                before.copy(
                    isObjectUploading = false,
                    snackbarMessage = "投稿数: ${list.size} / 成功した数: ${successList.size}"
                )
            }
        }
    }

    /** オブジェクトを削除する */
    fun deleteObject(key: String) {
        scope.launch {
            // 投稿
            val isSuccessful = AwsS3Client.deleteObject(
                bucketName = outputBucket ?: return@launch,
                key = key
            )
            // 更新
            _uiState.update { before ->
                before.copy(
                    snackbarMessage = if (isSuccessful) "削除しました。" else "問題が発生しました。"
                )
            }
            // 更新
            loadBucketObjectList()
        }
    }

    /** 再読み込みする */
    fun reloadBucketObjectList() {
        _uiState.update { before -> before.copy(loadState = AppUiState.LoadState.Reload) }
        loadBucketObjectList()
    }

    /** Snackbar を消す */
    fun deleteSnackbar() {
        _uiState.update { before -> before.copy(snackbarMessage = null) }
    }
}