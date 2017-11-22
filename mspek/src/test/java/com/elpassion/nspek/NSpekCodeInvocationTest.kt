package com.elpassion.nspek

import com.nhaarman.mockito_kotlin.*
import org.junit.After
import org.junit.Before
import org.junit.Test

private val codeFragment = mock<NSpekCodeInvocationTest.CodeFragment>()

class NSpekCodeInvocationTest {

    @Before
    fun setUp() {
        runClassTests(ExampleTestClass::class.java)
    }

    @Test
    fun shouldInvokeCodeWitchIsInside() {
        verify(codeFragment, times(5)).first()
    }

    @Test
    fun shouldInvokeCodeFromNamedNesting() {
        verify(codeFragment).second()
    }

    @Test
    fun shouldInvokeCodeOtherNamedNesting() {
        verify(codeFragment, times(3)).third()
    }

    @Test
    fun shouldInvokeCodeFromNamedNestedNesting() {
        verify(codeFragment).fourth()
    }

    @Test
    fun shouldInvokeCodeFromSecondNamedNestedNesting() {
        verify(codeFragment).fifth()
    }

    @Test
    fun shouldInvokeCodeAfterAllNamedNestedCases() {
        verify(codeFragment).sixth()
    }

    @Test
    fun shouldInvokeAllCodeInCorrectOrder() {
        val orderVerification = inOrder(codeFragment)
        orderVerification.verify(codeFragment).first()
        orderVerification.verify(codeFragment).second()

        orderVerification.verify(codeFragment).first()
        orderVerification.verify(codeFragment).third()
        orderVerification.verify(codeFragment).fourth()

        orderVerification.verify(codeFragment).first()
        orderVerification.verify(codeFragment).third()
        orderVerification.verify(codeFragment).fifth()

        orderVerification.verify(codeFragment).first()
        orderVerification.verify(codeFragment).third()

        orderVerification.verify(codeFragment).first()
        orderVerification.verify(codeFragment).sixth()

    }

    class ExampleTestClass {
        fun NSpekMethodContext.test() {
            codeFragment.first()
            "sub-test" o {
                codeFragment.second()
            }
            "sub-suite" o {
                codeFragment.third()
                "nested-subtest" o {
                    codeFragment.fourth()
                }
                "nested-failing-subtest" o {
                    codeFragment.fifth()
                }
            }
            codeFragment.sixth()
        }
    }

    @After
    fun tearDown() {
        reset(codeFragment)
    }

    interface CodeFragment{
        fun first()
        fun second()
        fun third()
        fun fourth()
        fun fifth()
        fun sixth()
    }
}