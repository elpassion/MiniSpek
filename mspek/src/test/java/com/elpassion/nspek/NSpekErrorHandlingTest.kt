package com.elpassion.nspek

import org.junit.Assert
import org.junit.Test

class NSpekErrorHandlingTest {

    @Test
    fun shouldNotCrashWhenThrowingExceptionFromRootOfTestMethod() {
        runClassTests(ExampleTest::class.java)
    }

    @Test
    fun shouldThrowIllegalArgumentExceptionWhenEmptyClassGiven() {
        try {
            runClassTests(EmptyTestClass::class.java)
            Assert.assertTrue(false)
        } catch (ex: Exception) {
            Assert.assertEquals(IllegalArgumentException::class.java, ex.javaClass)
            Assert.assertEquals("At least one method should be annotated with com.elpassion.nspek.Test", ex.message)
        }
    }

    class ExampleTest {
        @com.elpassion.nspek.Test
        fun NSpekMethodContext.test() {
            throw NullPointerException()
        }
    }

    class EmptyTestClass
}