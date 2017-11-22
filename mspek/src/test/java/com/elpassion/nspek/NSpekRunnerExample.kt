package com.elpassion.nspek

import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(NSpekRunner::class)
class NSpekRunnerExample {

    fun NSpekContext.test() {
        "subtest" o {
            assertTrue(true)
        }
        assertTrue(true)
    }
}