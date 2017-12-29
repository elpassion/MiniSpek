package com.elpassion.nspek

import org.junit.Test

class NSpekErrorHandlingTest {

    @Test
    fun shouldNotCrashWhenThrowingExceptionFromRootOfTestMethod() {
        runClassTests(ExampleTest::class.java)
    }

    class ExampleTest {
        @com.elpassion.nspek.Test
        fun NSpekMethodContext.test() {
            throw NullPointerException()
        }
    }
}