package io.github.takusan23.iroenpitu

import kotlinx.browser.localStorage

/** Web の Key-Value の永続化 */
actual val preference = object : Preference {

    override suspend fun load(): Map<String, String> {
        return (0 until localStorage.length)
            .mapNotNull { index -> localStorage.key(index) }
            .associateWith { key -> localStorage.getItem(key) ?: "" }
    }

    override suspend fun update(after: Map<String, String>) {
        after.forEach { (key, value) ->
            localStorage.setItem(key, value)
        }
    }

}