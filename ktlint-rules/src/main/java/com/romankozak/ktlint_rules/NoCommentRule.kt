package com.romankozak.ktlint_rules

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens

class NoCommentRule : Rule(
    ruleId = RuleId("custom:no-comment"),
    about = About(
        maintainer = "Roman Kozak",
        repositoryUrl = "...", // Вкажіть ваше репо
        issueTrackerUrl = "..." // Вкажіть трекер
    )
) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        when (node.elementType) {
            KtTokens.EOL_COMMENT, KtTokens.BLOCK_COMMENT, KtTokens.DOC_COMMENT -> {
                val message = when (node.elementType) {
                    KtTokens.EOL_COMMENT -> "Single-line comments are not allowed"
                    KtTokens.BLOCK_COMMENT -> "Block comments are not allowed"
                    else -> "Documentation comments are not allowed"
                }
                emit(node.startOffset, message, true)
                if (autoCorrect) {
                    removeCommentNode(node)
                }
            }
            else -> Unit
        }
    }

    private fun removeCommentNode(node: ASTNode) {
        val parent = node.treeParent
        if (parent != null) {
            val prevSibling = node.treePrev

            // Видаляємо сам вузол коментаря
            parent.removeChild(node)

            // Якщо безпосередньо перед коментарем на тому ж рядку був пробіл,
            // видаляємо його також (щоб уникнути зайвих пробілів в кінці рядка).
            // Це не зачепить перенос рядка.
            if (prevSibling != null && prevSibling.elementType == KtTokens.WHITE_SPACE) {
                if (!prevSibling.text.contains('\n')) {
                    parent.removeChild(prevSibling)
                }
            }
        }
    }
}
