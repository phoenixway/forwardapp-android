package com.romankozak.forwardappmobile.features.recent

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecentEntitiesRoomTest {
    @Test fun `existing Room table persists new types independently and reload returns last opened order`() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val dao = db.recentItemDao()
            val repository = RecentItemsRepository(dao)
            dao.logAccess(RecentItem("same", RecentItemType.PROJECT, 1, "Workspace", "same"))
            repository.logEntityAccess(RecentItemType.SEMANTIC_ENTITY, "same", "Subject")
            repository.logEntityAccess(RecentItemType.SCRIPT, "same", "Script")
            val reload = RecentItemsRepository(dao).getRecentItems(100).first()
            assertThat(reload).hasSize(3)
            assertThat(reload.map { it.type }).containsExactly(RecentItemType.PROJECT, RecentItemType.SEMANTIC_ENTITY, RecentItemType.SCRIPT)
            assertThat(reload.last().type).isEqualTo(RecentItemType.PROJECT)
            repository.logEntityAccess(RecentItemType.SEMANTIC_ENTITY, "same", "Renamed")
            assertThat(dao.getAllSync()).hasSize(3)
            assertThat(dao.getRecentItemById("SEMANTIC_ENTITY:same")!!.displayName).isEqualTo("Renamed")
        } finally { db.close() }
    }
}
