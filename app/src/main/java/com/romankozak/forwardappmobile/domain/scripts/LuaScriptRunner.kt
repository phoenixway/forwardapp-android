package com.romankozak.forwardappmobile.domain.scripts

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LuaScriptRunner @Inject constructor() {
    companion object {
        private const val ERROR_TAG = "LUA_SCRIPT_ERROR"
    }

    fun runScript(
        scriptContent: String,
        context: Map<String, String>,
        helpers: Map<String, LuaValue> = emptyMap(),
    ): Result<LuaValue> =
        runCatching {
            val globals: Globals = JsePlatform.standardGlobals()
            context.forEach { (key, value) ->
                globals.set(LuaValue.valueOf(key), LuaValue.valueOf(value))
            }
            helpers.forEach { (name, function) ->
                globals.set(name, function)
            }
            globals.load(scriptContent, "script").call()
        }.onFailure { e ->
            android.util.Log.e(ERROR_TAG, "Lua script failed: ${e.message}", e)
        }
}
