package com.romankozak.forwardappmobile.domain.aichat

sealed interface RoleItem {
    val name: String
    val path: String
    val isFolder: Boolean
}

data class RoleFolder(
    override val name: String,
    override val path: String,
    val children: List<RoleItem> = emptyList(),
) : RoleItem {
    override val isFolder: Boolean = true
}

data class RoleFile(
    override val name: String,
    override val path: String,
    val prompt: String,
) : RoleItem {
    override val isFolder: Boolean = false
}
