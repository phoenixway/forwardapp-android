package com.romankozak.forwardappmobile.features.projectscreen.components

data class Tag(val fullTag: String, val name: String)

object TagUtils {
    fun extractTags(text: String): List<Tag> {
        val regex = Regex("""#(\w+)""")
        return regex.findAll(text).map { Tag(it.value, it.groupValues[1]) }.toList()
    }
}
