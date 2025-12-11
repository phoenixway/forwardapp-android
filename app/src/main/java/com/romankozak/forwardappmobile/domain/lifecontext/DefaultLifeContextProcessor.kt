package com.romankozak.forwardappmobile.domain.lifecontext

class DefaultLifeContextProcessor(
    private val rules: List<LifeContextRule>,
) : LifeContextProcessor {

    override fun process(signal: LifeContextSignal) {
        rules.forEach { it.evaluate(signal) }
    }
}
