package com.elpassion.nspek

import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(NSpekRunner::class)
class NSpekRunnerExample {

    fun NSpekMethodContext.test() {
        "subtest" o {
            assertTrue(true)
            "nested-subtest" o {
                assertTrue(false)
            }
        }
        assertTrue(true)
    }
}