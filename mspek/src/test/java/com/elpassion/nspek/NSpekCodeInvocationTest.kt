package com.elpassion.nspek

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

private val firstCodeFragment = mock<() -> Unit>()
private val secondCodeFragment = mock<() -> Unit>()
private val thirdCodeFragment = mock<() -> Unit>()
private val fourthCodeFragment = mock<() -> Unit>()
private val fifthCodeFragment = mock<() -> Unit>()
private val sixthCodeFragment = mock<() -> Unit>()

class NSpekCodeInvocationTest {

    @Before
    fun setUp() {
        runClassTests(ExampleTestClass::class.java)
    }

    @Test
    fun shouldInvokeCodeWitchIsInside() {
        verify(firstCodeFragment, times(5)).invoke()
    }

    @Test
    fun shouldInvokeCodeFromNamedNesting() {
        verify(secondCodeFragment).invoke()
    }

    @Test
    fun shouldInvokeCodeOtherNamedNesting() {
        verify(thirdCodeFragment, times(3)).invoke()
    }

    @Test
    fun shouldInvokeCodeFromNamedNestedNesting() {
        verify(fourthCodeFragment).invoke()
    }

    @Test
    fun shouldInvokeCodeFromSecondNamedNestedNesting() {
        verify(fifthCodeFragment).invoke()
    }

    @Test
    fun shouldInvokeCodeAfterAllNamedNestedCases() {
        verify(sixthCodeFragment).invoke()
    }

    class ExampleTestClass {
        fun NSpekMethodContext.test() {
            firstCodeFragment.invoke()
            "sub-test" o {
                secondCodeFragment.invoke()
            }
            "sub-suite" o {
                thirdCodeFragment.invoke()
                "nested-subtest" o {
                    fourthCodeFragment.invoke()
                }
                "nested-failing-subtest" o {
                    fifthCodeFragment.invoke()
                }
            }
            sixthCodeFragment.invoke()
        }
    }

    @After
    fun tearDown() {
        reset(firstCodeFragment, secondCodeFragment, thirdCodeFragment,
                fourthCodeFragment, fifthCodeFragment, sixthCodeFragment)
    }
}