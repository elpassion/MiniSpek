package com.elpassion.nspek

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NSpekDescriptionTest {


    @Test
    fun shouldRegisterClassRootDescription() {
        assertEquals(ExampleTestClass::class.java.name, NSpekRunner(ExampleTestClass::class.java).description.displayName)
    }

    @Test
    fun shouldRegisterMethodNameDescription() {
        assertEquals("test", NSpekRunner(ExampleTestClass::class.java).description.children.first().displayName)
    }

    @Test
    fun shouldRegisterNestedTestDescription() {
        assertEquals("subtest(test)", NSpekRunner(ExampleTestClass::class.java).description.children.first().children.first().displayName)
    }

    @Test
    fun shouldRegisterNestedSuiteDescription() {
        val description = NSpekRunner(ExampleTestClass::class.java).description
        assertEquals("nested-suite", description.children.first().children[1].displayName)
    }

    @Test
    fun shouldRegisterNestedTestDescriptionInNestedSuite() {
        val description = NSpekRunner(ExampleTestClass::class.java).description
        assertEquals("nested-test(nested-suite)", description.children.first().children[1].children.first().displayName)
    }

    @Test
    fun shouldRegisterAllNestedTestDescriptionInNestedSuite() {
        assertEquals("nested-test-2(nested-suite)", NSpekRunner(ExampleTestClass::class.java).description.children.first().children[1].children[1].displayName)
    }

    @Test
    fun shouldRegisterAllDescriptionsForDifferentClass() {
        val testClass = OtherExampleTestClass::class.java
        assertEquals(testClass.name, NSpekRunner(testClass).description.displayName)
        assertEquals("notatest", NSpekRunner(testClass).description.children.first().displayName)
        assertEquals("not-a-subtest(notatest)", NSpekRunner(testClass).description.children.first().children.first().displayName)
        assertEquals("not-a-nested-suite", NSpekRunner(testClass).description.children.first().children[1].displayName)
        assertEquals("not-a-nested-test(not-a-nested-suite)", NSpekRunner(testClass).description.children.first().children[1].children.first().displayName)
        assertEquals("not-a-nested-test-2(not-a-nested-suite)", NSpekRunner(testClass).description.children.first().children[1].children[1].displayName)
    }

    class ExampleTestClass {
        fun NSpekMethodContext.test() {
            assertTrue(true)
            "subtest" o {
                assertTrue(true)
            }
            "nested-suite" o {
                "nested-test" o {
                    assertTrue(true)
                }
                "nested-test-2" o {
                    assertTrue(true)
                }
            }
        }
    }

    class OtherExampleTestClass {
        fun NSpekMethodContext.notatest() {
            assertTrue(true)
            "not-a-subtest" o {
                assertTrue(true)
            }
            "not-a-nested-suite" o {
                "not-a-nested-test" o {
                    assertTrue(true)
                }
                "not-a-nested-test-2" o {
                    assertTrue(true)
                }
            }
        }
    }
}

