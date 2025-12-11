package com.romankozak.forwardappmobile.domain.lifecontext

interface LifeContextProcessor {
    fun process(signal: LifeContextSignal)
}
