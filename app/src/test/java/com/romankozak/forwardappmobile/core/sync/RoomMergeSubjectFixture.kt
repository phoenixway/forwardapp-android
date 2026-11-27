package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.data.dao.CanonicalRecurringSeriesDao
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.database.AppDatabase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkClass
import kotlin.reflect.KClass

internal fun createRoomMergeSubject(db: AppDatabase): MergeLocalDataSourceImpl {
    val constructor =
        MergeLocalDataSourceImpl::class.java.declaredConstructors.single { candidate ->
            !candidate.isSynthetic &&
                candidate.parameterTypes.none {
                    it.name == "kotlin.jvm.internal.DefaultConstructorMarker"
                }
        }
    constructor.isAccessible = true

    val arguments =
        constructor.parameterTypes.map { parameterType ->
            when (parameterType) {
                AppDatabase::class.java -> db
                DayPlanDao::class.java -> db.dayPlanDao()
                DayTaskDao::class.java -> db.dayTaskDao()
                DayFocusItemDao::class.java -> db.dayFocusItemDao()
                CanonicalRecurringSeriesDao::class.java -> db.canonicalRecurringSeriesDao()
                ContextWorkspaceWriteThrough::class.java -> contextWorkspaceWriteThrough()
                else -> relaxedMock(parameterType)
            }
        }.toTypedArray()

    return constructor.newInstance(*arguments) as MergeLocalDataSourceImpl
}

private fun contextWorkspaceWriteThrough(): ContextWorkspaceWriteThrough {
    val writeThrough = mockk<ContextWorkspaceWriteThrough>(relaxed = true)
    coEvery {
        writeThrough.mutateAndAfterWorkspaceRefresh<Any?>(
            any(),
            any(),
            any(),
        )
    } coAnswers {
        val mutation = secondArg<suspend () -> Any?>()
        val afterRefresh = thirdArg<suspend (Any?) -> Unit>()
        val result = mutation()
        afterRefresh(result)
        result
    }
    return writeThrough
}

@Suppress("UNCHECKED_CAST")
private fun relaxedMock(type: Class<*>): Any =
    mockkClass(type.kotlin as KClass<Any>, relaxed = true)
