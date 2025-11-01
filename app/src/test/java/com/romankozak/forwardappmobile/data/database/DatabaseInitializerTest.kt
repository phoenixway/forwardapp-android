import android.content.Context
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.DatabaseInitializer
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectType
import com.romankozak.forwardappmobile.data.database.models.ReservedGroup
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor

class DatabaseInitializerTest {

    private lateinit var projectDao: ProjectDao
    private lateinit var context: Context
    private lateinit var databaseInitializer: DatabaseInitializer

    @Before
    fun setup() {
        projectDao = mock()
        context = mock()
        databaseInitializer = DatabaseInitializer(projectDao, context)

        // Mock string resources
        whenever(context.getString(R.string.special_project_name)).thenReturn("special")
        whenever(context.getString(R.string.strategic_group_name)).thenReturn("strategic")
        whenever(context.getString(R.string.mission_project_name)).thenReturn("mission")
        whenever(context.getString(R.string.long_term_strategy_project_name)).thenReturn("long-term-strategy")
        whenever(context.getString(R.string.medium_term_program_project_name)).thenReturn("medium-term-program")
        whenever(context.getString(R.string.active_quests_project_name)).thenReturn("active-quests")
        whenever(context.getString(R.string.strategic_goals_project_name)).thenReturn("strategic-inbox")
        whenever(context.getString(R.string.strategic_review_project_name)).thenReturn("strategic-review")
        whenever(context.getString(R.string.inbox_project_name)).thenReturn("inbox")
    }

    @Test
    fun `prePopulate inserts projects when special project does not exist`() = runTest {
        // Given
        whenever(projectDao.getProjectsByType(ProjectType.SYSTEM.name)).thenReturn(emptyList())

        // When
        databaseInitializer.prePopulate()

        // Then
        argumentCaptor<List<Project>>().apply {
            verify(projectDao).insertProjects(capture())
            assert(allValues.first().size == 9) // 1 special + 1 strategic group + 6 strategic projects + 1 inbox
            assert(allValues.first().any { it.name == "special" && it.projectType == ProjectType.SYSTEM })
            assert(allValues.first().any { it.name == "strategic" && it.projectType == ProjectType.RESERVED && it.reservedGroup == ReservedGroup.StrategicGroup })
            assert(allValues.first().any { it.name == "mission" && it.projectType == ProjectType.RESERVED && it.reservedGroup == ReservedGroup.Strategic })
            assert(allValues.first().any { it.name == "inbox" && it.projectType == ProjectType.RESERVED && it.reservedGroup == ReservedGroup.Inbox })
        }
    }

    @Test
    fun `prePopulate does not insert projects when special project exists`() = runTest {
        // Given
        val existingSpecialProject = Project(
            id = "special-id",
            name = "special",
            projectType = ProjectType.SYSTEM,
            parentId = null,
            createdAt = System.currentTimeMillis(),
            description = null,
            updatedAt = null
        )
        whenever(projectDao.getProjectsByType(ProjectType.SYSTEM.name)).thenReturn(listOf(existingSpecialProject))

        // When
        databaseInitializer.prePopulate()

        // Then
        verify(projectDao, never()).insertProjects(any())
    }
}
