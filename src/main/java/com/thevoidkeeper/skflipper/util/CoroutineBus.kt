package com.thevoidkeeper.skflipper.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object CoroutineBus {
    lateinit var io: CoroutineScope
        private set

    fun init() {
        io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
