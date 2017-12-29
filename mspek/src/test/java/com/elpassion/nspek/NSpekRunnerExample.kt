package com.elpassion.nspek

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(NSpekRunner::class)
class NSpekRunnerExample {

    @Test
    fun NSpekMethodContext.test() {
        "subtest" o {
            assertTrue(true)
            ASa()
        }
        assertTrue(true)
    }

    private fun NSpekMethodContext.ASa() {
        "nested-subtest" o {
            assertTrue(true)
        }
    }
}