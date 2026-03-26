package com.romankozak.forwardappmobile.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchRepositoryTest {
    @Test
    fun `buildSafeActivityFtsQuery strips leading minus and keeps searchable token`() {
        val result = buildSafeActivityFtsQuery("%-model%")

        assertThat(result).isEqualTo("\"model\"")
    }

    @Test
    fun `buildSafeActivityFtsQuery returns null for punctuation only query`() {
        val result = buildSafeActivityFtsQuery("%---%")

        assertThat(result).isNull()
    }

    @Test
    fun `buildSafeActivityFtsQuery keeps multiple tokens as quoted terms`() {
        val result = buildSafeActivityFtsQuery("%foo-bar baz%")

        assertThat(result).isEqualTo("\"foo\" \"bar\" \"baz\"")
    }
}
