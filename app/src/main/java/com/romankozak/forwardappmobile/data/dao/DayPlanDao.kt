
package com.romankozak.forwardappmobile.data.dao

import androidx.room.*
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DayPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: DayPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<DayPlan>)

    @Update
    suspend fun update(plan: DayPlan)

    @Delete
    suspend fun delete(plan: DayPlan)

    @Query("SELECT * FROM day_plans WHERE id = :planId LIMIT 1")
    suspend fun getPlanById(planId: String): DayPlan?

    @Query("SELECT * FROM day_plans WHERE date = :dayStartMillis LIMIT 1")
    suspend fun getPlanForDateSync(dayStartMillis: Long): DayPlan?

    @Query("SELECT * FROM day_plans WHERE date = :dayStartMillis LIMIT 1")
    fun getPlanForDate(dayStartMillis: Long): Flow<DayPlan?>

    @Query("SELECT * FROM day_plans WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getPlansForDateRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<DayPlan>>

    @Query("SELECT * FROM day_plans ORDER BY date DESC LIMIT :limit")
    fun getRecentPlans(limit: Int = 30): Flow<List<DayPlan>>

    @Query("SELECT * FROM day_plans WHERE status = :status ORDER BY date ASC")
    fun getPlansByStatus(status: DayStatus): Flow<List<DayPlan>>

    @Query("SELECT * FROM day_plans ORDER BY date ASC")
    fun getAllPlans(): Flow<List<DayPlan>>

    @Query("UPDATE day_plans SET status = :status, updatedAt = :updatedAt, version = version + 1, syncedAt = NULL WHERE id = :planId")
    suspend fun updatePlanStatus(
        planId: String,
        status: DayStatus,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE day_plans 
        SET totalCompletedMinutes = :minutes, 
            completionPercentage = :percentage, 
            updatedAt = :updatedAt,
            version = version + 1,
            syncedAt = NULL
        WHERE id = :planId
        """,
    )
    suspend fun updatePlanProgress(
        planId: String,
        minutes: Long,
        percentage: Float,
        updatedAt: Long,
    )

    @Query("SELECT id FROM day_plans WHERE date >= :date")
    suspend fun getFutureDayPlanIds(date: Long): List<String>

    @Query("SELECT * FROM day_plans WHERE id = :planId LIMIT 1")
    fun getPlanByIdStream(planId: String): Flow<DayPlan?>
}
