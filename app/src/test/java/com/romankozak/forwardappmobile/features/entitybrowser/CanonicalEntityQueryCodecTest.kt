package com.romankozak.forwardappmobile.features.entitybrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanonicalEntityQueryCodecTest {
    @Test fun `all predicates roundtrip with exact typed sort and bounds`() {
        val query = CanonicalEntityQuery("Лікування", setOf(CanonicalEntityClass.ORIENTATION, CanonicalEntityClass.WORKSPACE), setOf("health", "finance"),
            mapOf(CanonicalScoreDimension.IMPORTANCE to CanonicalScoreRange(7.1, 12.0), CanonicalScoreDimension.RISK to CanonicalScoreRange(max = 5.0)),
            CanonicalEntitySort(CanonicalEntitySortKey.SCORE, CanonicalSortDirection.DESC, CanonicalScoreDimension.IMPORTANCE), true)
        assertThat(CanonicalEntityQueryCodec.decode(CanonicalEntityQueryCodec.encode(query)).query).isEqualTo(query)
    }
    @Test fun `every current class dimension and sort direction remains typed`() {
        for (dimension in CanonicalScoreDimension.entries) for (direction in CanonicalSortDirection.entries) {
            val query = CanonicalEntityQuery(sort = CanonicalEntitySort(CanonicalEntitySortKey.SCORE, direction, dimension),
                scoreFilters = mapOf(dimension to CanonicalScoreRange(min = 2.0)))
            assertThat(CanonicalEntityQueryCodec.decode(CanonicalEntityQueryCodec.encode(query)).query).isEqualTo(query)
        }
    }
    @Test fun `empty type set is retained rather than broadened`() {
        val query = CanonicalEntityQuery(entityTypes = emptySet())
        assertThat(CanonicalEntityQueryCodec.decode(CanonicalEntityQueryCodec.encode(query)).query).isEqualTo(query)
    }
    @Test fun `future version type and numeric dimension fail closed`() {
        val encoded = CanonicalEntityQueryCodec.encode(CanonicalEntityQuery(scoreFilters = mapOf(CanonicalScoreDimension.RISK to CanonicalScoreRange(min = 1.0))))
        for (raw in listOf(encoded.replace("\"version\":1", "\"version\":99"), encoded.replace("ORIENTATION", "PLANNING"),
            encoded.replace("risk", "urgency"), encoded.replace("\"min\":1.0", "\"min\":\"NaN\""), "broken")) {
            assertThat(CanonicalEntityQueryCodec.decode(raw).query).isNull()
            assertThat(CanonicalEntityQueryCodec.decode(raw).issue).isNotNull()
        }
    }
    @Test fun `missing or additional predicate is not silently dropped`() {
        val encoded = CanonicalEntityQueryCodec.encode(CanonicalEntityQuery())
        assertThat(CanonicalEntityQueryCodec.decode(encoded.replace("\"unplaced\":false", "\"future\":false")).query).isNull()
        assertThat(CanonicalEntityQueryCodec.decode(encoded.dropLast(1) + ",\"future\":true}").query).isNull()
    }
    @Test fun `tags normalized consistently with existing authority`() {
        val decoded = CanonicalEntityQueryCodec.decode(CanonicalEntityQueryCodec.encode(CanonicalEntityQuery(tags = setOf("#HEALTH", " health "))))
        assertThat(decoded.query!!.tags).containsExactly("health")
    }
}
