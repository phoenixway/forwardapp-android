package com.romankozak.forwardappmobile.core.data.interfaces

interface SystemContextEnsurer {
    suspend fun ensureAllSystemContextsExist()
}