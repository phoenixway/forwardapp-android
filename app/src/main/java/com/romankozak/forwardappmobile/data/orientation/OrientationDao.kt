package com.romankozak.forwardappmobile.data.orientation

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationScoringOverrideEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.SavedOrientationViewEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.data.database.OrientationBootstrapIssueEntity
import com.romankozak.forwardappmobile.data.database.OrientationBootstrapStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrientationDao {
    @Query("SELECT * FROM managed_subject_conversions")
    suspend fun getSubjectConversions(): List<com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectConversionEntity>

    @Query("DELETE FROM aspects WHERE subjectId=:id")
    suspend fun retireAspectSubtypeForConversion(id: String)
    @Query("SELECT * FROM managed_subjects ORDER BY updatedAt DESC")
    fun observeManagedSubjects(): Flow<List<ManagedSubjectEntity>>

    @Query("SELECT * FROM managed_subjects")
    suspend fun getAllManagedSubjects(): List<ManagedSubjectEntity>

    @Query("SELECT * FROM managed_subjects WHERE id = :id LIMIT 1")
    suspend fun getManagedSubject(id: String): ManagedSubjectEntity?

    @Query("SELECT * FROM orientations")
    suspend fun getAllOrientations(): List<OrientationEntity>

    @Query("SELECT * FROM orientations WHERE subjectId = :id LIMIT 1")
    suspend fun getOrientation(id: String): OrientationEntity?

    @Query("SELECT * FROM orientations")
    fun observeOrientations(): Flow<List<OrientationEntity>>

    @Query("SELECT * FROM aspects")
    suspend fun getAllAspects(): List<AspectEntity>

    @Query("SELECT * FROM aspects ORDER BY parentAspectId, aspectOrder")
    fun observeAspects(): Flow<List<AspectEntity>>

    @Query("SELECT * FROM aspects WHERE subjectId = :id LIMIT 1")
    suspend fun getAspect(id: String): AspectEntity?

    @Query("SELECT * FROM orientation_assessments")
    suspend fun getAllAssessments(): List<OrientationAssessmentEntity>

    @Query("SELECT * FROM orientation_assessments WHERE orientationId IN (:ids)")
    suspend fun getAssessmentsFor(ids: List<String>): List<OrientationAssessmentEntity>

    @Query("SELECT * FROM orientation_assessment_revisions WHERE id IN (:ids)")
    suspend fun getAssessmentRevisionsByIds(ids: List<String>): List<OrientationAssessmentRevisionEntity>

    @Query("SELECT * FROM orientation_assessments ORDER BY orientationId")
    fun observeAssessments(): Flow<List<OrientationAssessmentEntity>>

    @Query("SELECT * FROM orientation_assessment_revisions")
    suspend fun getAllAssessmentRevisions(): List<OrientationAssessmentRevisionEntity>

    @Query("SELECT * FROM orientation_assessment_revisions ORDER BY orientationId, effectiveFrom, id")
    fun observeAssessmentRevisions(): Flow<List<OrientationAssessmentRevisionEntity>>

    @Query("SELECT * FROM orientation_scoring_overrides")
    suspend fun getAllScoringOverrides(): List<OrientationScoringOverrideEntity>

    @Query("SELECT * FROM orientation_scoring_overrides ORDER BY orientationId")
    fun observeScoringOverrides(): Flow<List<OrientationScoringOverrideEntity>>

    @Query("SELECT * FROM orientation_scoring_overrides WHERE orientationId = :orientationId LIMIT 1")
    suspend fun getScoringOverride(orientationId: String): OrientationScoringOverrideEntity?

    @Query("SELECT * FROM legacy_subject_mappings")
    suspend fun getAllLegacyMappings(): List<LegacySubjectMappingEntity>

    @Query("SELECT * FROM legacy_subject_mappings")
    fun observeLegacyMappings(): Flow<List<LegacySubjectMappingEntity>>

    @Query("SELECT * FROM legacy_subject_mappings WHERE sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun getLegacyMapping(sourceType: String, sourceId: String): LegacySubjectMappingEntity?

    @Query("SELECT * FROM orientation_relations")
    suspend fun getAllOrientationRelations(): List<OrientationRelationEntity>

    @Query(
        """
        SELECT * FROM orientation_relations
        WHERE isDeleted = 0
          AND (fromOrientationId = :orientationId OR toOrientationId = :orientationId)
        ORDER BY relationType, fromOrientationId, relationOrder, toOrientationId, id
        """,
    )
    fun observeLiveOrientationRelationsForOrientation(
        orientationId: String,
    ): Flow<List<OrientationRelationEntity>>

    @Query(
        """
        SELECT * FROM orientation_relations
        ORDER BY relationType, fromOrientationId, relationOrder, toOrientationId, id
        """,
    )
    fun observeOrientationRelations(): Flow<List<OrientationRelationEntity>>

    @Query("SELECT * FROM aspect_orientation_refs")
    suspend fun getAllAspectOrientationRefs(): List<AspectOrientationRefEntity>

    @Query("SELECT * FROM aspect_orientation_refs ORDER BY aspectId, refOrder")
    fun observeAspectOrientationRefs(): Flow<List<AspectOrientationRefEntity>>

    @Query("SELECT * FROM aspect_orientation_refs WHERE aspectId=:aspectId AND isDeleted=0 ORDER BY refOrder, id")
    fun observeLiveAspectRefsForAspect(aspectId: String): Flow<List<AspectOrientationRefEntity>>

    @Query(
        """
        SELECT * FROM aspect_orientation_refs
        WHERE orientationId = :orientationId AND isDeleted = 0
        ORDER BY refOrder, id
        """,
    )
    fun observeLiveAspectRefsForOrientation(orientationId: String): Flow<List<AspectOrientationRefEntity>>

    @Query("SELECT * FROM workspace_bindings")
    suspend fun getAllWorkspaceBindings(): List<WorkspaceBindingEntity>

    @Query("SELECT * FROM workspace_bindings ORDER BY workspaceId, bindingOrder")
    fun observeWorkspaceBindings(): Flow<List<WorkspaceBindingEntity>>

    /** Narrow inverse presentation read; never infers embodiment from Context or H1. */
    @Query("""
        SELECT b.* FROM workspace_bindings b
        INNER JOIN workspaces w ON w.id = b.workspaceId
        WHERE b.isDeleted = 0 AND w.isDeleted = 0 AND b.bindingType = 'EMBODIES'
    """)
    suspend fun getLiveEmbodimentBindings(): List<WorkspaceBindingEntity>

    @Query(
        """
        SELECT * FROM workspace_bindings
        WHERE subjectId = :subjectId AND isDeleted = 0
        ORDER BY bindingOrder, id
        """,
    )
    fun observeLiveWorkspaceBindingsForSubject(subjectId: String): Flow<List<WorkspaceBindingEntity>>

    @Query(
        """
        SELECT * FROM workspace_bindings
        WHERE workspaceId = :workspaceId AND isDeleted = 0
        ORDER BY bindingOrder, id
        """,
    )
    fun observeLiveWorkspaceBindingsForWorkspace(workspaceId: String): Flow<List<WorkspaceBindingEntity>>

    @Query("SELECT * FROM workspace_capability_instances")
    suspend fun getAllWorkspaceCapabilities(): List<WorkspaceCapabilityInstanceEntity>

    @Query("SELECT * FROM workspace_capability_instances WHERE workspaceId IN (:workspaceIds)")
    suspend fun getWorkspaceCapabilitiesForWorkspaces(
        workspaceIds: List<String>,
    ): List<WorkspaceCapabilityInstanceEntity>

    @Query("SELECT * FROM workspace_capability_instances WHERE workspaceId = :workspaceId")
    fun observeWorkspaceCapabilities(workspaceId: String): Flow<List<WorkspaceCapabilityInstanceEntity>>

    @Query("SELECT * FROM saved_orientation_views")
    suspend fun getAllSavedViews(): List<SavedOrientationViewEntity>

    @Upsert
    suspend fun upsertManagedSubjects(items: List<ManagedSubjectEntity>)

    @Upsert
    suspend fun upsertOrientations(items: List<OrientationEntity>)

    @Upsert
    suspend fun upsertAspects(items: List<AspectEntity>)

    @Upsert
    suspend fun upsertAssessments(items: List<OrientationAssessmentEntity>)

    @Upsert
    suspend fun upsertAssessmentRevisions(items: List<OrientationAssessmentRevisionEntity>)

    @Upsert
    suspend fun upsertScoringOverrides(items: List<OrientationScoringOverrideEntity>)

    @Upsert
    suspend fun upsertLegacyMappings(items: List<LegacySubjectMappingEntity>)

    @Upsert
    suspend fun upsertOrientationRelations(items: List<OrientationRelationEntity>)

    @Upsert
    suspend fun upsertAspectOrientationRefs(items: List<AspectOrientationRefEntity>)

    @Upsert
    suspend fun upsertWorkspaceBindings(items: List<WorkspaceBindingEntity>)

    @Upsert
    suspend fun upsertWorkspaceCapabilities(items: List<WorkspaceCapabilityInstanceEntity>)

    @Upsert
    suspend fun upsertSavedViews(items: List<SavedOrientationViewEntity>)

    @Query("SELECT * FROM orientation_bootstrap_state WHERE id = 1 LIMIT 1")
    suspend fun getBootstrapState(): OrientationBootstrapStateEntity?

    @Upsert
    suspend fun upsertBootstrapState(state: OrientationBootstrapStateEntity)

    @Query("SELECT * FROM orientation_bootstrap_issues WHERE resolvedAt IS NULL")
    suspend fun getOpenBootstrapIssues(): List<OrientationBootstrapIssueEntity>

    @Upsert
    suspend fun upsertBootstrapIssues(items: List<OrientationBootstrapIssueEntity>)

    @Query("UPDATE orientation_bootstrap_issues SET resolvedAt = :resolvedAt WHERE resolvedAt IS NULL")
    suspend fun resolveOpenBootstrapIssues(resolvedAt: Long)

    @Query("UPDATE managed_subjects SET syncedAt = :syncedAt WHERE id = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markSubjectSynced(id: String, version: Long, syncedAt: Long): Int

    @Query("UPDATE orientation_assessments SET syncedAt = :syncedAt WHERE orientationId = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markAssessmentSynced(id: String, version: Long, syncedAt: Long): Int

    @Query("UPDATE orientation_assessment_revisions SET syncedAt = :syncedAt WHERE id = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markAssessmentRevisionSynced(id: String, version: Long, syncedAt: Long): Int

    @Query("UPDATE orientation_scoring_overrides SET syncedAt = :syncedAt WHERE orientationId = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markScoringOverrideSynced(id: String, version: Long, syncedAt: Long): Int

    @Query("UPDATE legacy_subject_mappings SET syncedAt = :syncedAt WHERE id = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markLegacyMappingSynced(id: String, version: Long, syncedAt: Long): Int

    @Query("UPDATE orientation_relations SET syncedAt = :syncedAt WHERE id = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markRelationSynced(id: String, version: Long, syncedAt: Long): Int

    @Query("UPDATE aspect_orientation_refs SET syncedAt = :syncedAt WHERE id = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markAspectRefSynced(id: String, version: Long, syncedAt: Long): Int

    @Query("UPDATE workspace_bindings SET syncedAt = :syncedAt WHERE id = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markWorkspaceBindingSynced(id: String, version: Long, syncedAt: Long): Int

    @Query("UPDATE workspace_capability_instances SET syncedAt = :syncedAt WHERE id = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markWorkspaceCapabilitySynced(id: String, version: Long, syncedAt: Long): Int

    @Query("UPDATE saved_orientation_views SET syncedAt = :syncedAt WHERE id = :id AND version = :version AND syncedAt IS NULL")
    suspend fun markSavedViewSynced(id: String, version: Long, syncedAt: Long): Int
}
