package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import org.junit.Test

class DayTaskPropertiesPresentationTest {
    @Test fun `all existing property tabs retain labels and stable order`() {
        assertThat(EditTaskTab.entries.map { it.title })
            .containsExactly("Основне", "Деталі", "Повтор", "Нотатки", "Реалізує").inOrder()
    }

    @Test fun `priority labels use product language rather than persisted enum names`() {
        assertThat(TaskPriority.CRITICAL.taskPropertiesLabel()).isEqualTo("Критичний")
        assertThat(TaskPriority.NONE.taskPropertiesLabel()).isEqualTo("Без пріоритету")
        assertThat(TaskPriority.entries.map { it.taskPropertiesLabel() }.distinct()).hasSize(TaskPriority.entries.size)
        TaskPriority.entries.forEach { assertThat(it.taskPropertiesLabel()).isNotEqualTo(it.name) }
    }
}
