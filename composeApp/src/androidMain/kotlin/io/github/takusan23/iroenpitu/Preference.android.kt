package io.github.takusan23.iroenpitu

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val dataStoreFlow = MutableStateFlow<Preferences?>(null)
private val updatePreferenceChannel = Channel<Map<String, String>>()

/** Android 側 Key-Value ストア */
actual val preference = object : Preference {
    override suspend fun load(): Map<String, String> {
        val preference = dataStoreFlow.filterNotNull().first()
        return preference.asMap()
            .map { it.key.name to it.value.toString() }
            .associate { it }
    }

    override suspend fun update(after: Map<String, String>) {
        updatePreferenceChannel.send(after)
    }
}

/** Preference.kt を使うために一回これを呼び出してください */
@Composable
fun PreferenceInitEffect() {
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        // 値更新を通知
        launch {
            context.dataStore.data.collect {
                dataStoreFlow.value = it
            }
        }
        // 受け取ったら値を更新する
        launch {
            for (preferenceMap in updatePreferenceChannel) {
                context.dataStore.edit {
                    preferenceMap.forEach { (key, value) ->
                        it[stringPreferencesKey(key)] = value
                    }
                }
            }
        }
    }
}