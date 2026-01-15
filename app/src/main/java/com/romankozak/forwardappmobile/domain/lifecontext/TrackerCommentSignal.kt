package com.romankozak.forwardappmobile.domain.lifecontext

data class TrackerCommentSignal(
    val text: String,
    override val timestamp: Long,
) : LifeContextSignal
