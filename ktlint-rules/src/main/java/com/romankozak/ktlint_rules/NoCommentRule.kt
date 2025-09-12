package com.romankozak.ktlint_rules

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens

class NoCommentRule(about: About) : Rule(
    ruleId = RuleId("custom:no-comment"), about
) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        when (node.elementType) {
            KtTokens.EOL_COMMENT -> {
                emit(node.startOffset, "Single-line comments are not allowed", false)
            }
            KtTokens.BLOCK_COMMENT -> {
                emit(node.startOffset, "Block comments are not allowed", false)
            }
            KtTokens.DOC_COMMENT -> {
                emit(node.startOffset, "Documentation comments are not allowed", false)
            }
        }
    }
}