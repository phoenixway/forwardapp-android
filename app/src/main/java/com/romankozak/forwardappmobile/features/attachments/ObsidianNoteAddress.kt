package com.romankozak.forwardappmobile.features.attachments

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/** External note address only; ForwardApp does not index or inspect vault files. */
data class ObsidianNoteAddress(val target: String, val vault: String?, val displayName: String, val openUrl: String? = null) {
    fun openUri(fallbackVault: String?): String? {
        if (openUrl != null) return openUrl
        val resolvedVault = vault ?: fallbackVault?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return "obsidian://open?vault=${encode(resolvedVault)}&file=${encode(target)}"
    }

    companion object {
        fun parse(input: String, vault: String? = null): ObsidianNoteAddress {
            val target = input.trim()
            require(target.isNotEmpty()) { "Вкажіть ім’я, шлях або Obsidian URL" }
            val explicitVault = vault?.trim()?.takeIf(String::isNotEmpty)
            if (!target.contains("://")) {
                require(!target.startsWith("/") && target.split('/').none { it == ".." }) {
                    "Шлях має бути від кореня vault, наприклад Folder/Note.md"
                }
                return ObsidianNoteAddress(target, explicitVault, target.substringAfterLast('/'))
            }
            val uri = runCatching { URI(target) }.getOrElse { throw IllegalArgumentException("Некоректний Obsidian URL") }
            require(uri.scheme.equals("obsidian", ignoreCase = true) && uri.host == "open") {
                "Вставте Obsidian URL нотатки: obsidian://open?..."
            }
            val parameters = uri.rawQuery.orEmpty().split('&').filter(String::isNotEmpty).map {
                val pair = it.split('=', limit = 2)
                decode(pair[0]) to decode(pair.getOrElse(1) { "" })
            }
            require(parameters.map { it.first }.distinct().size == parameters.size) { "Повторені параметри Obsidian URL" }
            val params = parameters.toMap()
            val path = params["path"]?.takeIf(String::isNotBlank)
            val file = params["file"]?.takeIf(String::isNotBlank)
            val urlVault = params["vault"]?.takeIf(String::isNotBlank)
            require(path != null || (file != null && urlVault != null)) { "Obsidian URL має містити vault і file або абсолютний path" }
            // Preserve copied URLs, including headings, block IDs and paneType.
            return ObsidianNoteAddress(target, urlVault, (path ?: file!!).substringAfterLast('/'), target)
        }

        private fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")
        private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
}
