package com.romankozak.forwardappmobile.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Provider

class DatabaseCallback(
    private val projectDaoProvider: Provider<ProjectDao>,
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch(Dispatchers.IO) {
            val projectDao = projectDaoProvider.get()
            prePopulateProjects(projectDao)
        }
    }

    private suspend fun prePopulateProjects(projectDao: ProjectDao) {
        val projects = listOf(
            Project(id = UUID.randomUUID().toString(), name = "Місія", description = null, parentId = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null, projectType = ProjectType.BEACON),
            Project(id = UUID.randomUUID().toString(), name = "Довгострокова стратегія", description = null, parentId = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null, projectType = ProjectType.BEACON),
            Project(id = UUID.randomUUID().toString(), name = "Середньострокова програма", description = null, parentId = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null, projectType = ProjectType.BEACON),
            Project(id = UUID.randomUUID().toString(), name = "Активні квести", description = null, parentId = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null, projectType = ProjectType.BEACON),
            Project(id = UUID.randomUUID().toString(), name = "Стратегічні цілі", description = null, parentId = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null, projectType = ProjectType.BEACON),
            Project(id = UUID.randomUUID().toString(), name = "Стратегічний огляд", description = null, parentId = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null, projectType = ProjectType.BEACON),
            Project(id = UUID.randomUUID().toString(), name = "Inbox", description = null, parentId = null, createdAt = System.currentTimeMillis(), updatedAt = null, tags = null, projectType = ProjectType.BEACON)
        )
        projectDao.insertProjects(projects)
    }
}
