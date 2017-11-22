package com.elpassion.nspek

import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class NSpekRunNotifierTest {

    @Test
    fun shouldStartClassTest() {
        val second = runClassTests(ExampleTestClass::class.java).second
        Assert.assertTrue(second.any { it is Notification.Start && it.description.displayName == ExampleTestClass::class.java.name })
        Assert.assertTrue(second.any { it is Notification.End && it.description.displayName == ExampleTestClass::class.java.name })
    }

    @Test
    fun shouldStartMethodTest() {
        val second = runClassTests(ExampleTestClass::class.java).second
        Assert.assertTrue(second.any { it is Notification.Start && it.description.displayName == "test" })
        Assert.assertTrue(second.any { it is Notification.End && it.description.displayName == "test" })
    }

    @Test
    fun shouldStartSubTest() {
        val second = runClassTests(ExampleTestClass::class.java).second
        Assert.assertTrue(second.any { it is Notification.Start && it.description.displayName == "sub-test(test)" })
        Assert.assertTrue(second.any { it is Notification.End && it.description.displayName == "sub-test(test)" })
    }

    @Test
    fun shouldStartNestedSubTest() {
        val second = runClassTests(ExampleTestClass::class.java).second
        Assert.assertTrue(second.any { it is Notification.Start && it.description.displayName == "sub-suite" })
        Assert.assertTrue(second.any { it is Notification.End && it.description.displayName == "sub-suite" })

        Assert.assertTrue(second.any { it is Notification.Start && it.description.displayName == "nested-subtest(sub-suite)" })
        Assert.assertTrue(second.any { it is Notification.End && it.description.displayName == "nested-subtest(sub-suite)" })
    }

    @Test
    @Ignore
    fun shouldHaveFailingNestedTest() {
        val second = runClassTests(ExampleTestClass::class.java).second
        Assert.assertTrue(second.any { it is Notification.Start && it.description.displayName == "nested-failing-subtest(sub-suite)" })
        Assert.assertTrue(second.any { it is Notification.Failure && it.description.displayName == "nested-failing-subtest(sub-suite)" && it.cause is AssertionError })
    }

    class ExampleTestClass {

        fun NSpekMethodContext.test() {
            "sub-test" o {
                assertTrue(true)
            }
            "sub-suite" o {
                assertTrue(true)
                "nested-subtest" o {
                    assertTrue(true)
                }
                "nested-failing-subtest" o {
//                    assertTrue(false)
                }
            }
        }
    }
}