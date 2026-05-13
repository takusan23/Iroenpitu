package io.github.takusan23.iroenpitu

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform