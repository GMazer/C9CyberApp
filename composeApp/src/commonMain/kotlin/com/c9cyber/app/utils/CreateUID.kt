package com.c9cyber.app.utils

import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun CreateUID(): String {
    val clock: Clock = Clock.System
    val instant = clock.now()
    val unixtime = instant.epochSeconds
    val cyberid = "C9"
    print(
        message = "$unixtime"
    )
    return "$cyberid-$unixtime"
}