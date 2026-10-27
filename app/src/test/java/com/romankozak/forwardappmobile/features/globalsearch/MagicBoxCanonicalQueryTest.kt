package com.romankozak.forwardappmobile.features.globalsearch

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import org.junit.Test

class MagicBoxCanonicalQueryTest {
    private val dimension = CanonicalScoreDimension.IMPORTANCE
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("same"), "Health",
        tags = setOf("health", "medical"), scores = mapOf(dimension to 7.0))
    private val aspect = CanonicalEntitySummary(CanonicalEntityRef.Aspect("same"), "Family", tags = setOf("health"))
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("same"), "Home", tags = setOf("home", "health"))
    private val catalog = listOf(orientation, aspect, workspace)
    private fun resolve(text: String) = resolveMagicBoxQuery(text, SavedEntityQueries())
    private fun results(text: String) = executeCanonicalEntityQuery(catalog, requireNotNull(resolve(text).query)).map { it.ref }
    private fun saved(id: String, name: String, query: CanonicalEntityQuery = CanonicalEntityQuery(tags = setOf("health"))) =
        SavedEntityQuery(id, name, CanonicalEntityQueryCodec.encode(query), 1, 1)

    @Test fun `plain text uses shared canonical search`() { assertThat(results("Health")).containsExactly(orientation.ref) }
    @Test fun `tag sees all direct typed owners`() { assertThat(results("#health")).containsExactlyElementsIn(catalog.map { it.ref }) }
    @Test fun `multiple tags AND`() { assertThat(results("#health #medical")).containsExactly(orientation.ref) }
    @Test fun `orientation type`() { assertThat(results("type:orientation")).containsExactly(orientation.ref) }
    @Test fun `aspect type`() { assertThat(results("type:aspect")).containsExactly(aspect.ref) }
    @Test fun `workspace type`() { assertThat(results("type:workspace")).containsExactly(workspace.ref) }
    @Test fun `semantic tag and type`() { assertThat(results("#health type:aspect")).containsExactly(aspect.ref) }
    @Test fun `operational tag and type`() { assertThat(results("#home type:workspace")).containsExactly(workspace.ref) }
    @Test fun `score minimum`() { assertThat(results("score:importance>=7")).containsExactly(orientation.ref) }
    @Test fun `score maximum excludes missing scores`() { assertThat(results("score:importance<=7")).containsExactly(orientation.ref) }
    @Test fun `tag score composition`() { assertThat(results("#health score:importance>=7")).containsExactly(orientation.ref) }
    @Test fun `type tag score composition equals Library engine`() {
        val library = CanonicalEntityQuery(entityTypes = setOf(CanonicalEntityClass.ORIENTATION), tags = setOf("health"),
            scoreFilters = mapOf(dimension to CanonicalScoreRange(min = 7.0)))
        assertThat(results("type:orientation #health score:importance>=7"))
            .containsExactlyElementsIn(executeCanonicalEntityQuery(catalog, library).map { it.ref })
    }
    @Test fun `title sort asc and desc`() {
        assertThat(results("sort:title")).containsExactly(aspect.ref, orientation.ref, workspace.ref).inOrder()
        assertThat(results("sort:title:desc")).containsExactly(workspace.ref, orientation.ref, aspect.ref).inOrder()
    }
    @Test fun `score sort missing last both directions`() {
        assertThat(results("sort:importance").first()).isEqualTo(orientation.ref)
        assertThat(results("sort:importance:desc").first()).isEqualTo(orientation.ref)
    }
    @Test fun `free text operators compose`() { assertThat(results("Health #health type:orientation score:importance>=7")).containsExactly(orientation.ref) }
    @Test fun `unknown operator fails closed`() {
        assertThat(resolve("banana:4").query).isNull()
        assertThat(parseCanonicalEntityQuery("banana:4").errorKinds).contains(CanonicalQueryError.UNKNOWN_OPERATOR)
    }
    @Test fun `unknown type distinct error`() { assertThat(parseCanonicalEntityQuery("type:context").errorKinds).contains(CanonicalQueryError.UNKNOWN_ENTITY_TYPE) }
    @Test fun `malformed score distinct error`() { assertThat(parseCanonicalEntityQuery("score:importance>=oops").errorKinds).contains(CanonicalQueryError.MALFORMED_NUMBER) }
    @Test fun `unknown scoring dimension distinct error`() { assertThat(parseCanonicalEntityQuery("score:banana>=4").errorKinds).contains(CanonicalQueryError.UNKNOWN_SCORE_DIMENSION) }
    @Test fun `typed ID collisions remain distinct`() { assertThat(results("#health")).hasSize(3) }
    @Test fun `exact semantic orientation navigation`() { assertThat(magicBoxCanonicalDestination(orientation.ref)).isEqualTo(NavTarget.SemanticEntityEditor("same")) }
    @Test fun `exact semantic aspect navigation`() { assertThat(magicBoxCanonicalDestination(aspect.ref)).isEqualTo(NavTarget.SemanticEntityEditor("same")) }
    @Test fun `exact workspace navigation without Context`() { assertThat(magicBoxCanonicalDestination(workspace.ref)).isEqualTo(NavTarget.Workspace("same")) }
    @Test fun `saved exact full structured query resolves`() {
        val query = CanonicalEntityQuery(text = "Health", tags = setOf("health"), unplaced = true,
            scoreFilters = mapOf(dimension to CanonicalScoreRange(max = 8.0)), sort = CanonicalEntitySort(CanonicalEntitySortKey.TITLE))
        assertThat(resolveMagicBoxQuery("saved:Important Health", SavedEntityQueries(listOf(saved("1", "Important Health", query)))).query).isEqualTo(query)
    }
    @Test fun `missing saved safe`() { assertThat(resolve("saved:no").issue).isEqualTo(MagicBoxQueryIssue.SAVED_MISSING) }
    @Test fun `duplicate name requires stable choice`() {
        val queries = SavedEntityQueries(listOf(saved("1", "Foo"), saved("2", "Foo", CanonicalEntityQuery(text = "Home"))))
        assertThat(resolveMagicBoxQuery("saved:Foo", queries).issue).isEqualTo(MagicBoxQueryIssue.SAVED_AMBIGUOUS)
        assertThat(resolveMagicBoxQuery("saved:Foo", queries, "2").query?.text).isEqualTo("Home")
    }
    @Test fun `deleted selected saved ID cannot switch to remaining duplicate`() {
        assertThat(resolveMagicBoxQuery("saved:Foo", SavedEntityQueries(listOf(saved("2", "Foo"))), "1").query).isNull()
    }
    @Test fun `saved prefix suggests and selects stable IDs`() {
        val queries = SavedEntityQueries(listOf(saved("1", "Foo")))
        assertThat(resolveMagicBoxQuery("saved:", queries).choices.map { it.id }).containsExactly("1")
        assertThat(resolveMagicBoxQuery("saved:", queries, "1").query).isNotNull()
    }
    @Test fun `saved plus predicates is not implicit composition`() {
        assertThat(resolveMagicBoxQuery("saved:Foo type:aspect", SavedEntityQueries(listOf(saved("1", "Foo")))).query).isNull()
    }
    @Test fun `future saved query fails safely`() {
        val invalid = saved("1", "Foo").copy(specification = "{\"version\":999}")
        assertThat(resolveMagicBoxQuery("saved:Foo", SavedEntityQueries(listOf(invalid))).issue).isEqualTo(MagicBoxQueryIssue.SAVED_UNAVAILABLE)
    }
    @Test fun `commands stay outside canonical operators and structured input detected`() {
        assertThat(hasCanonicalQueryOperators("health")).isFalse()
        assertThat(hasCanonicalQueryOperators("health #medical")).isTrue()
        assertThat(hasCanonicalQueryOperators("unknown:value")).isTrue()
    }
}
