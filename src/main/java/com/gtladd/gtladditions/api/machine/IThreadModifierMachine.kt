package com.gtladd.gtladditions.api.machine

import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart

interface IThreadModifierMachine {
    fun getAdditionalThread(): Int = getThreadPartMachine()?.getThreadCount() ?: 0

    fun getThreadPartMachine(): IThreadModifierPart? = null

    fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {}
}