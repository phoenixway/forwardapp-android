package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import com.romankozak.forwardappmobile.data.dao.BacklogOrderDao
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogOrder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BacklogOrderRepository @Inject constructor(
    private val backlogOrderDao: BacklogOrderDao,
) {

    private val TAG = "BacklogOrderRepo"

    suspend fun getOrdersForList(listId: String): List<BacklogOrder> =
        backlogOrderDao.getOrdersForList(listId)

    fun observeAll(): Flow<List<BacklogOrder>> = backlogOrderDao.observeAll()

    suspend fun upsertOrders(orders: List<BacklogOrder>) {
        if (orders.isEmpty()) return
        Log.d(TAG, "[upsertOrders] count=${orders.size} sample=${orders.take(3)}")
        backlogOrderDao.insertOrders(orders)
    }
}
