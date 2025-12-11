package com.romankozak.forwardappmobile.domain.lifecontext

interface LifeContextRule {
    fun evaluate(signal: LifeContextSignal)
}
