package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state

import com.romankozak.forwardappmobile.data.repository.ContextRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Управляє тегами та контекстами для автодоповнення
 */
class TagManager(
    private val contextRepository: ContextRepository,
    private val scope: CoroutineScope
) {
    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

    private val _allContexts = MutableStateFlow<List<String>>(emptyList())
    val allContexts: StateFlow<List<String>> = _allContexts.asStateFlow()

    /**
     * Завантажує всі теги з бази даних
     */
    fun loadTags() {
        scope.launch {
            contextRepository.getAllContextsFlow().collect { contexts ->
                // Збираємо унікальні теги з усіх контекстів
                val tags = contexts
                    .flatMap { it.tags }
                    .distinct()
                    .sorted()
                _allTags.value = tags

                // Збираємо назви контекстів
                val contextNames = contexts
                    .map { it.name }
                    .distinct()
                    .sorted()
                _allContexts.value = contextNames
            }
        }
    }

    /**
     * Додає новий тег до списку
     */
    fun addTag(tag: String) {
        if (tag.isNotBlank() && !_allTags.value.contains(tag)) {
            _allTags.value = (_allTags.value + tag).sorted()
        }
    }

    /**
     * Фільтрує теги за запитом
     */
    fun filterTags(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return _allTags.value.filter { 
            it.contains(query, ignoreCase = true) 
        }
    }

    /**
     * Фільтрує контексти за запитом
     */
    fun filterContexts(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return _allContexts.value.filter { 
            it.contains(query, ignoreCase = true) 
        }
    }
}
