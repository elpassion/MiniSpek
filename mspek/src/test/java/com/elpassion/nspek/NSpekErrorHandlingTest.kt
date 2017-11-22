package com.elpassion.nspek

import org.junit.Test

class NSpekErrorHandlingTest {

    @Test
    fun shouldNotCrashWhenThrowingExceptionFromRootOfTestMethod() {
        runClassTests(ExampleTest::class.java)
    }

    class ExampleTest {
        fun NSpekMethodContext.test(){
            throw NullPointerException()
        }
    }
}