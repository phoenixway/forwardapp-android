package com.romankozak.forwardappmobile.diagnostics

import android.os.SystemClock
import android.util.Log
import com.romankozak.forwardappmobile.BuildConfig
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext

/** Debug-only, in-memory correlation. Never logs titles, text, IDs, tags or dates. */
internal object BacklogAddTrace {
    private val recorder = BacklogAddTraceRecorder(SystemClock::elapsedRealtime) { Log.d("BacklogAddTiming", it) }

    fun start(workspaceId: String): BacklogAddTraceRecorder.Operation? =
        if (BuildConfig.DEBUG) recorder.start(workspaceId) else null

    suspend fun <T> measure(stage: String, block: suspend () -> T): T {
        val operation = currentCoroutineContext()[BacklogAddTraceRecorder.Operation]
        if (operation == null) return block()
        operation.mark("$stage.begin")
        val started = SystemClock.elapsedRealtime()
        return try { block() } finally { operation.mark("$stage.end", SystemClock.elapsedRealtime() - started) }
    }

    suspend fun <T> measureWorkspace(workspaceId: String, stage: String, block: suspend () -> T): T {
        val operations = if (BuildConfig.DEBUG) recorder.pending(workspaceId) else emptyList()
        if (operations.isEmpty()) return block()
        operations.forEach { it.mark("$stage.begin", once = false) }
        val started = SystemClock.elapsedRealtime()
        return try { block() } finally {
            val duration = SystemClock.elapsedRealtime() - started
            operations.forEach { it.mark("$stage.end", duration, once = false) }
        }
    }

    fun targetObserved(workspaceId: String?, stage: String, contains: (placementId: String?, goalId: String?) -> Boolean) {
        if (BuildConfig.DEBUG) recorder.targetObserved(workspaceId, stage, contains)
    }

    fun markWorkspace(workspaceId: String, stage: String) {
        if (BuildConfig.DEBUG) recorder.pending(workspaceId).forEach { it.mark(stage, once = false) }
    }
}

/** Testable monotonic clock/sink; bounded retention even if the screen closes before layout. */
internal class BacklogAddTraceRecorder(private val now: () -> Long, private val emit: (String) -> Unit) {
    private var nextId = 0L
    private val operations = ArrayDeque<Operation>()

    @Synchronized
    fun start(workspaceId: String): Operation {
        prune()
        while (operations.size >= 32) operations.removeFirst()
        return Operation(++nextId, workspaceId, now(), now, emit).also {
            operations.addLast(it)
            it.mark("submit")
        }
    }

    @Synchronized
    fun pending(workspaceId: String?): List<Operation> {
        if (operations.isEmpty()) return emptyList()
        prune()
        return operations.filter { !it.terminal && (workspaceId == null || it.workspaceId == workspaceId) }
    }

    fun targetObserved(workspaceId: String?, stage: String, contains: (String?, String?) -> Boolean) {
        pending(workspaceId).forEach { operation ->
            if ((operation.placementId != null || operation.goalId != null) && contains(operation.placementId, operation.goalId)) {
                operation.mark(stage)
            }
        }
    }

    private fun prune() {
        val cutoff = now() - 60_000
        while (operations.firstOrNull()?.startedAt?.let { it < cutoff } == true) operations.removeFirst()
    }

    class Operation internal constructor(
        private val id: Long,
        internal val workspaceId: String,
        internal val startedAt: Long,
        private val now: () -> Long,
        private val emit: (String) -> Unit,
    ) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<Operation>
        @Volatile var goalId: String? = null
        @Volatile var placementId: String? = null
        @Volatile internal var terminal = false
        private val stages = mutableSetOf<String>()

        @Synchronized
        fun mark(stage: String, durationMs: Long? = null, once: Boolean = true) {
            if (terminal || (once && !stages.add(stage))) return
            emit("op=$id +${now() - startedAt}ms $stage" + (durationMs?.let { " duration=${it}ms" } ?: ""))
            if (stage == "failed" || stage == "cancelled") terminal = true
            if ("ui.visible" in stages && "input.updated" in stages) {
                emit("op=$id +${now() - startedAt}ms complete")
                terminal = true
            }
        }
    }
}
