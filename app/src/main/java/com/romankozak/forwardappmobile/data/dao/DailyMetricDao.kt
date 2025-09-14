// DailyMetricDao.kt
package com.romankozak.forwardappmobile.data.dao

import androidx.room.*
import com.romankozak.forwardappmobile.data.database.models.DailyMetric
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyMetricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: DailyMetric)

    @Update
    suspend fun update(metric: DailyMetric)

    @Delete
    suspend fun delete(metric: DailyMetric)

    @Query("SELECT * FROM daily_metrics WHERE dayPlanId = :dayPlanId LIMIT 1")
    suspend fun getMetricForDay(dayPlanId: String): DailyMetric?

    @Query("SELECT * FROM daily_metrics WHERE date = :date LIMIT 1")
    suspend fun getMetricByDate(date: Long): DailyMetric?

    @Query("SELECT * FROM daily_metrics WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getMetricsForDateRange(startDate: Long, endDate: Long): Flow<List<DailyMetric>>

    @Query("SELECT * FROM daily_metrics ORDER BY date DESC LIMIT :limit")
    fun getRecentMetrics(limit: Int = 30): Flow<List<DailyMetric>>

    @Query("SELECT * FROM daily_metrics ORDER BY date ASC")
    fun getAllMetrics(): Flow<List<DailyMetric>>

    @Query("SELECT AVG(completionRate) FROM daily_metrics WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAverageCompletionRate(startDate: Long, endDate: Long): Float?

    @Query("SELECT AVG(morningEnergyLevel) FROM daily_metrics WHERE date BETWEEN :startDate AND :endDate AND morningEnergyLevel IS NOT NULL")
    suspend fun getAverageEnergyLevel(startDate: Long, endDate: Long): Float?

    @Query("SELECT * FROM daily_metrics WHERE dayPlanId = :dayPlanId LIMIT 1")
    fun getMetricForDayStream(dayPlanId: String): Flow<DailyMetric?>

}
