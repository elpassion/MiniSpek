package com.elpassion.nspek

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier

class NSpekDescriptionTest {


    @Test
    fun shouldRegisterClassRootDescription() {
        assertEquals(RegisterRootDescription::class.java.name, NSpekRunner(RegisterRootDescription::class.java).description.displayName)
    }

    @Test
    fun shouldRegisterMethodNameDescription() {
        assertEquals("test", NSpekRunner(RegisterRootDescription::class.java).description.children.first().displayName)
    }

    @Test
    fun shouldRegisterNestedTestDescription() {
        assertEquals("subtest(test)", NSpekRunner(RegisterRootDescription::class.java).description.children.first().children.first().displayName)
    }

    @Test
    fun shouldRegisterNestedSuiteDescription() {
        assertEquals("nested-suite", NSpekRunner(RegisterRootDescription::class.java).description.children.first().children[1].displayName)
    }

    @Test
    fun shouldRegisterNestedTestDescriptionInNestedSuite() {
        assertEquals("nested-test(nested-suite)", NSpekRunner(RegisterRootDescription::class.java).description.children.first().children[1].children.first().displayName)
    }

    @Test
    fun shouldRegisterAllNestedTestDescriptionInNestedSuite() {
        assertEquals("nested-test-2(nested-suite)", NSpekRunner(RegisterRootDescription::class.java).description.children.first().children[1].children[1].displayName)
    }

    class RegisterRootDescription {
        fun NSpekContext.test() {
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
}


class NSpekRunner(testClass: Class<*>) : Runner() {

    private val rootDescription = Description.createSuiteDescription(testClass).apply {
        addChild(Description.createSuiteDescription(testClass.declaredMethods.first().name).apply {
            addChild(Description.createTestDescription(this.displayName, "subtest"))
            addChild(Description.createSuiteDescription("nested-suite").apply {
                addChild(Description.createTestDescription(this.displayName, "nested-test"))
                addChild(Description.createTestDescription(this.displayName, "nested-test-2"))
            })
        })
    }

    override fun getDescription(): Description = rootDescription

    override fun run(notifier: RunNotifier?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class NSpekContext {
    infix fun String.o(code: () -> Unit) {

    }
}
