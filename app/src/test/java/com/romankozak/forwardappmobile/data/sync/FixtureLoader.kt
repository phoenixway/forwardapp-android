package com.romankozak.forwardappmobile.data.sync

import java.nio.charset.StandardCharsets

object FixtureLoader {
    fun loadJson(name: String): String {
        // Prefer external shared fixtures if available
        val externalPath = java.nio.file.Paths.get("..", "test-data", "common", "sync-fixtures", name).toFile()
        if (externalPath.exists()) {
            return externalPath.readText()
        }

        val path = "sync-fixtures/$name"
        val stream = this::class.java.classLoader?.getResourceAsStream(path)
            ?: this::class.java.getResourceAsStream("/$path")
            ?: error("Fixture not found: $path (also tried ../test-data/common/sync-fixtures)")
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}
