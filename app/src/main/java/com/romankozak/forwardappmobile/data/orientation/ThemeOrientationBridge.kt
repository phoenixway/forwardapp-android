package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.ThemeDefinitionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.data.dao.CanonicalDayThemeDao
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Semantic authority boundary for reusable Themes. ThemeDefinition remains a
 * typed extension and compatibility projection; it no longer resolves common
 * field conflicts after its mapping is CUT_OVER.
 */
@Singleton
class ThemeOrientationBridge
    @Inject
    constructor(
        private val orientationDao: OrientationDao,
        private val dayThemeDao: CanonicalDayThemeDao,
        private val bootstrapper: CanonicalOrientationBootstrapper,
    ) {
        private val gson = Gson()

        suspend fun ensureCutOver() {
            val report = bootstrapper.ensureBootstrapped()
            check(report.issues.isEmpty()) {
                "Theme Orientation authority cutover is blocked by ${report.issues.size} bootstrap issue(s)"
            }
        }

        fun observeCanonicalDefinitions(
            definitions: Flow<List<ThemeDefinitionEntity>>,
        ): Flow<List<ThemeDefinitionEntity>> =
            combine(
                definitions,
                orientationDao.observeManagedSubjects(),
                orientationDao.observeLegacyMappings(),
                orientationDao.observeOrientations(),
            ) { rows, subjects, mappings, orientations ->
                projectCanonicalThemeDefinitions(rows, subjects, mappings, orientations)
            }

        suspend fun projectCanonicalDefinitions(
            definitions: List<ThemeDefinitionEntity>,
        ): List<ThemeDefinitionEntity> =
            projectCanonicalThemeDefinitions(
                definitions = definitions,
                subjects = orientationDao.getAllManagedSubjects(),
                mappings = orientationDao.getAllLegacyMappings(),
                orientations = orientationDao.getAllOrientations(),
            )

        suspend fun requireLiveThemeSubjectId(themeDefinitionId: String): String {
            val mapping =
                requireNotNull(
                    orientationDao.getLegacyMapping(
                        LegacyOrientationSourceType.THEME_DEFINITION.name,
                        themeDefinitionId,
                    ),
                ) { "Theme $themeDefinitionId has no canonical mapping" }
            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name && !mapping.isDeleted) {
                "Theme $themeDefinitionId mapping is not live CUT_OVER"
            }
            val subject = requireNotNull(orientationDao.getManagedSubject(mapping.subjectId)) {
                "Theme $themeDefinitionId canonical subject is missing"
            }
            require(!subject.isDeleted && subject.subjectType == ManagedSubjectType.ORIENTATION.name) {
                "Theme $themeDefinitionId canonical subject is not a live Orientation"
            }
            val orientation = orientationDao.getAllOrientations().singleOrNull { it.subjectId == mapping.subjectId }
            require(orientation?.kind == OrientationKind.THEME.name) {
                "Theme $themeDefinitionId canonical kind is not THEME"
            }
            return mapping.subjectId
        }

        /** Must be invoked inside the caller's Room transaction. */
        suspend fun writeCommon(
            definition: ThemeDefinitionEntity,
            title: String,
            description: String,
            now: Long,
        ) {
            val mapping =
                orientationDao.getLegacyMapping(
                    LegacyOrientationSourceType.THEME_DEFINITION.name,
                    definition.id,
                )
            if (mapping == null) {
                createCanonicalTheme(definition.copy(title = title, description = description))
                return
            }

            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name && !mapping.isDeleted) {
                "Theme ${definition.id} mapping is not a live CUT_OVER mapping"
            }
            val subject =
                requireNotNull(orientationDao.getManagedSubject(mapping.subjectId)) {
                    "CUT_OVER Theme ${definition.id} is missing canonical subject ${mapping.subjectId}"
                }
            require(subject.subjectType == ManagedSubjectType.ORIENTATION.name && !subject.isDeleted) {
                "CUT_OVER Theme ${definition.id} canonical subject is not a live Orientation"
            }
            val orientation =
                orientationDao.getAllOrientations().singleOrNull { it.subjectId == mapping.subjectId }
            require(orientation?.kind == OrientationKind.THEME.name) {
                "CUT_OVER Theme ${definition.id} canonical kind is not THEME"
            }

            if (subject.title != title || subject.description.orEmpty() != description) {
                orientationDao.upsertManagedSubjects(
                    listOf(
                        subject.copy(
                            title = title,
                            description = description,
                            updatedAt = now,
                            syncedAt = null,
                            version = subject.version + 1L,
                        ),
                    ),
                )
            }
            dayThemeDao.projectThemeCommonFields(definition.id, title, description, isDeleted = false)
        }

        private suspend fun createCanonicalTheme(definition: ThemeDefinitionEntity) {
            val rows =
                definition.toCutOverCanonicalThemeRows(
                    gson = gson,
                    migrationVersion = CanonicalOrientationBootstrapper.CURRENT_BOOTSTRAP_VERSION,
                )
            val projectionSubjectId = rows.subject.id
            require(orientationDao.getManagedSubject(projectionSubjectId) == null) {
                "New Theme ${definition.id} collides with canonical subject $projectionSubjectId"
            }
            require(orientationDao.getAllLegacyMappings().none { it.subjectId == projectionSubjectId }) {
                "New Theme ${definition.id} canonical subject already has a source mapping"
            }
            orientationDao.upsertManagedSubjects(listOf(rows.subject))
            orientationDao.upsertOrientations(listOf(rows.orientation))
            orientationDao.upsertAssessmentRevisions(listOf(rows.revision))
            orientationDao.upsertAssessments(listOf(rows.assessment))
            orientationDao.upsertLegacyMappings(
                listOf(
                    rows.mapping.copy(
                        state = LegacySubjectMappingState.CUT_OVER.name,
                    ),
                ),
            )
        }
    }

internal fun ThemeDefinitionEntity.toCutOverCanonicalThemeRows(
    gson: Gson,
    migrationVersion: Int,
): CanonicalOrientationRows {
    val projection =
        toEffectiveOrientation(LegacySubjectUuid).let { legacy ->
            legacy.copy(
                orientation = legacy.orientation.copy(kind = OrientationKind.THEME),
            )
        }
    require(validateOrientationAssessment(OrientationKind.THEME, projection.orientation.assessment).isEmpty()) {
        "New Theme $id has an invalid canonical assessment"
    }
    val rows = projection.toCanonicalRows(gson, migrationVersion)
    return rows.copy(
        mapping = rows.mapping.copy(state = LegacySubjectMappingState.CUT_OVER.name),
    )
}

internal fun projectCanonicalThemeDefinitions(
    definitions: List<ThemeDefinitionEntity>,
    subjects: List<ManagedSubjectEntity>,
    mappings: List<LegacySubjectMappingEntity>,
    orientations: List<OrientationEntity>,
): List<ThemeDefinitionEntity> {
    val subjectsById = subjects.associateBy { it.id }
    val orientationsById = orientations.associateBy { it.subjectId }
    val mappingsBySource =
        mappings.groupBy { it.sourceType to it.sourceId }
    return definitions.map { definition ->
        val sourceMappings =
            mappingsBySource[
                LegacyOrientationSourceType.THEME_DEFINITION.name to definition.id
            ].orEmpty()
        require(sourceMappings.size == 1) {
            "Theme ${definition.id} must have exactly one canonical source mapping"
        }
        val mapping = sourceMappings.single()
        require(mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
            "Theme ${definition.id} mapping is not CUT_OVER"
        }
        val expectedSubjectId =
            LegacySubjectUuid.resolve(
                com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef(
                    LegacyOrientationSourceType.THEME_DEFINITION,
                    definition.id,
                ),
            )
        require(mapping.subjectId == expectedSubjectId) {
            "Theme ${definition.id} mapping does not preserve deterministic semantic identity"
        }
        val subject =
            requireNotNull(subjectsById[mapping.subjectId]) {
                "CUT_OVER Theme ${definition.id} is missing canonical subject ${mapping.subjectId}"
            }
        require(subject.subjectType == ManagedSubjectType.ORIENTATION.name) {
            "CUT_OVER Theme ${definition.id} target is not an Orientation"
        }
        require(orientationsById[mapping.subjectId]?.kind == OrientationKind.THEME.name) {
            "CUT_OVER Theme ${definition.id} canonical kind is not THEME"
        }
        definition.copy(
            title = subject.title,
            description = subject.description.orEmpty(),
            isDeleted = subject.isDeleted,
        )
    }
}
