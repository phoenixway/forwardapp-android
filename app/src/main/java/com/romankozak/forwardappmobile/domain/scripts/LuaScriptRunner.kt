package com.romankozak.forwardappmobile.domain.scripts

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LuaScriptRunner @Inject constructor() {
    fun runScript(scriptContent: String, context: Map<String, String>): Result<LuaValue> =
        runCatching {
            val globals: Globals = JsePlatform.standardGlobals()
            context.forEach { (key, value) ->
                globals.set(LuaValue.valueOf(key), LuaValue.valueOf(value))
            }
            globals.load(scriptContent, "script").call()
        }
}
