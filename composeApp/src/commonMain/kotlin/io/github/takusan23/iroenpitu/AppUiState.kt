package io.github.takusan23.iroenpitu

/** App() の UiState */
data class AppUiState(
    val loadState: LoadState = LoadState.Init,
    val photoList: List<AwsS3Client.ListObject> = emptyList(),
    val snackbarMessage: String? = null,
    val baseUrl: String? = null
) {

    /** 読み込み状態 */
    enum class LoadState {
        Init,
        Reload,
        Loaded
    }

}