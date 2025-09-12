package com.romankozak.ktlint_rules

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

class CustomRuleSetProvider : RuleSetProviderV3(RuleSetId("custom")) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { NoCommentRule() },
        )
}
