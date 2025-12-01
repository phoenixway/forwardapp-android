package com.romankozak.forwardappmobile.data.sync

import java.nio.charset.StandardCharsets

object FixtureLoader {
    fun loadJson(name: String): String {
        val path = "sync-fixtures/$name"
        val stream = this::class.java.classLoader?.getResourceAsStream(path)
            ?: this::class.java.getResourceAsStream("/$path")
            ?: error("Fixture not found on classpath: $path")
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}
