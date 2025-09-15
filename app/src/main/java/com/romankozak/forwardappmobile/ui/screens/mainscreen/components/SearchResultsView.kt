package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.mainscreen.SearchResult

@Composable
fun SearchResultsView(
    results: List<SearchResult>,
    onResultClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(results, key = { it.list.id }) { result ->
            Column(
                modifier = Modifier
                    .clickable { onResultClick(result.list.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val annotatedString = buildAnnotatedString {
                    result.path.forEachIndexed { index, breadcrumb ->
                        // Додаємо анотацію з ID для кожного елемента шляху
                        pushStringAnnotation(tag = "listId", annotation = breadcrumb.id)

                        // Стилізуємо текст, щоб він виглядав як посилання
                        val style = if (index == result.path.size - 1) {
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        } else {
                            SpanStyle(color = MaterialTheme.colorScheme.tertiary)
                        }
                        withStyle(style = style) {
                            append(breadcrumb.name)
                        }
                        pop() // Завершуємо анотацію

                        if (index < result.path.size - 1) {
                            append(" / ")
                        }
                    }
                }

                // ClickableText дозволяє обробляти натискання на різні частини тексту
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "listId", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                onResultClick(annotation.item)
                            }
                    }
                )
            }
            HorizontalDivider()
        }
    }
}