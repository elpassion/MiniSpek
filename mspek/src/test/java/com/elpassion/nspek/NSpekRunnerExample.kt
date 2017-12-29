package com.elpassion.nspek

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(NSpekRunner::class)
class NSpekRunnerExample {

    @Test
    fun NSpekMethodContext.test() {
        "subtest" o {
            assertTrue(true)
            extractedAssertion()
        }
        assertTrue(true)
    }

    private fun NSpekMethodContext.extractedAssertion() =
            "nested-subtest" o {
                assertFalse(false)
            }
}