package io.github.takusan23.iroenpitu

/**
 * Android の DataStore
 * Web の LocalStorage
 */
interface Preference {

    suspend fun load(): Map<String, String>

    suspend fun update(after: Map<String, String>)

    /** 予約済みキー */
    companion object {
        const val KEY_ACCESS_KEY = "access_key"
        const val KEY_SECRET_ACCESS_LEY = "secret_access_key"
        const val KEY_REGION = "region"
        const val KEY_INPUT_BUCKET = "bucket_input"
        const val KEY_OUTPUT_BUCKET = "bucket_output"
    }
}

expect val preference: Preference