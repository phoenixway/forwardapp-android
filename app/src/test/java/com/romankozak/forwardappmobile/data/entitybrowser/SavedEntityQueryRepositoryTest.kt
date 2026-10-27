package com.romankozak.forwardappmobile.data.entitybrowser

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.features.entitybrowser.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SavedEntityQueryRepositoryTest {
    @get:Rule val temp = TemporaryFolder()
    private class Fixture(file: File) : AutoCloseable {
        private val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file })
        val repo = SavedEntityQueryRepository(store)
        override fun close() { runBlocking { job.cancelAndJoin() } }
    }
    private fun file() = File(temp.newFolder(), "saved.preferences_pb")
    private fun query() = CanonicalEntityQuery("health", setOf(CanonicalEntityClass.ORIENTATION), setOf("health", "finance"),
        mapOf(CanonicalScoreDimension.IMPORTANCE to CanonicalScoreRange(7.0, 12.0), CanonicalScoreDimension.RISK to CanonicalScoreRange(max = 3.0)),
        CanonicalEntitySort(CanonicalEntitySortKey.SCORE, CanonicalSortDirection.DESC, CanonicalScoreDimension.IMPORTANCE), true)

    @Test fun `save owns structured full specification not results or DSL`() = runBlocking<Unit> {
        Fixture(file()).use { f ->
            val id = f.repo.save("  Important  ", query(), 10)
            val item = f.repo.observe().first().items.single()
            assertThat(item.id).isEqualTo(id)
            assertThat(item.name).isEqualTo("Important")
            assertThat(item.decode().query).isEqualTo(query())
            assertThat(item.specification).contains("\"scoreFilters\"")
            assertThat(item.specification).doesNotContain("resultIds")
            assertThat(item.createdAt).isEqualTo(10)
        }
    }
    @Test fun `disk reload preserves identity query and timestamps`() = runBlocking<Unit> {
        val file = file()
        val saved = Fixture(file).use { f -> f.repo.save("Test", query(), 10); f.repo.observe().first() }
        Fixture(file).use { f -> assertThat(f.repo.observe().first()).isEqualTo(saved) }
    }
    @Test fun `rename preserves identity and exact query`() = runBlocking<Unit> {
        Fixture(file()).use { f ->
            val id = f.repo.save("Before", query(), 10)
            f.repo.rename(id, "After", 20)
            val item = f.repo.observe().first().items.single()
            assertThat(item.id).isEqualTo(id)
            assertThat(item.name).isEqualTo("After")
            assertThat(item.decode().query).isEqualTo(query())
            assertThat(item.createdAt).isEqualTo(10)
            assertThat(item.updatedAt).isEqualTo(20)
        }
    }
    @Test fun `duplicate display names have independent stable identities`() = runBlocking<Unit> {
        Fixture(file()).use { f ->
            val first = f.repo.save("Same", query(), 10)
            val second = f.repo.save("Same", CanonicalEntityQuery(), 11)
            assertThat(first).isNotEqualTo(second)
            f.repo.rename(first, "Renamed", 12)
            val secondBefore = f.repo.observe().first().items.single { it.id == second }
            f.repo.delete(first)
            assertThat(f.repo.observe().first().items).containsExactly(secondBefore)
        }
    }
    @Test fun `delete touches only preference collection not active query or unrelated state`() = runBlocking<Unit> {
        Fixture(file()).use { f ->
            val key = stringPreferencesKey("unrelated")
            f.store.edit { it[key] = "unchanged" }
            val active = query()
            val id = f.repo.save("Test", active)
            f.repo.delete(id)
            assertThat(f.repo.observe().first().items).isEmpty()
            assertThat(f.store.data.first()[key]).isEqualTo("unchanged")
            assertThat(active).isEqualTo(query())
        }
    }
    @Test fun `results are recomputed from live metadata and missing tags stay empty`() = runBlocking<Unit> {
        Fixture(file()).use { f ->
            val spec = CanonicalEntityQuery(tags = setOf("health"))
            f.repo.save("Health", spec)
            val saved = f.repo.observe().first().items.single()
            val entity = CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "First")
            assertThat(executeCanonicalEntityQuery(listOf(entity), saved.decode().query!!)).isEmpty()
            val tagged = entity.copy(title = "Renamed", tags = setOf("health"))
            assertThat(executeCanonicalEntityQuery(listOf(tagged), saved.decode().query!!)).containsExactly(tagged)
            assertThat(executeCanonicalEntityQuery(emptyList(), saved.decode().query!!)).isEmpty()
            assertThat(f.repo.observe().first().items.single()).isEqualTo(saved)
        }
    }
    @Test fun `unknown dimension survives rename and other saves but cannot apply`() = runBlocking<Unit> {
        Fixture(file()).use { f ->
            val id = f.repo.save("Future", query(), 10)
            f.store.edit { it[SavedEntityQueryRepository.KEY] = it[SavedEntityQueryRepository.KEY]!!.replace("importance", "future-dimension") }
            val old = f.repo.observe().first().items.single()
            assertThat(old.decode().query).isNull()
            assertThat(old.decode().issue).isNotEmpty()
            f.repo.rename(id, "Kept", 20)
            f.repo.save("Other", CanonicalEntityQuery())
            val kept = f.repo.observe().first().items.single { it.id == id }
            assertThat(kept.specification).isEqualTo(old.specification)
            f.repo.delete(id)
            assertThat(f.repo.observe().first().items).hasSize(1)
        }
    }
    @Test fun `unknown collection format cannot be overwritten by CRUD`() = runBlocking<Unit> {
        Fixture(file()).use { f ->
            val raw = "{\"version\":99,\"items\":[]}"
            f.store.edit { it[SavedEntityQueryRepository.KEY] = raw }
            assertThat(f.repo.observe().first().issue).isNotNull()
            assertThat(runCatching { f.repo.save("No", query()) }.isFailure).isTrue()
            assertThat(runCatching { f.repo.delete("any") }.isFailure).isTrue()
            assertThat(f.store.data.first()[SavedEntityQueryRepository.KEY]).isEqualTo(raw)
        }
    }
    @Test fun `concurrent saves cannot lose collection entries`() = runBlocking<Unit> {
        Fixture(file()).use { f ->
            coroutineScope { (1..12).map { async { f.repo.save("Query $it", CanonicalEntityQuery()) } }.awaitAll() }
            assertThat(f.repo.observe().first().items).hasSize(12)
        }
    }
    @Test fun `invalid name changes nothing`() = runBlocking<Unit> {
        Fixture(file()).use { f ->
            for (name in listOf("  ", "x".repeat(121))) assertThat(runCatching { f.repo.save(name, query()) }.isFailure).isTrue()
            assertThat(f.repo.observe().first().items).isEmpty()
        }
    }
}
