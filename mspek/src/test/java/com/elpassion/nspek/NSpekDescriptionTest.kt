package com.elpassion.nspek

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import java.lang.reflect.Method

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
        assertEquals("nested-suite", NSpekRunner(ExampleTestClass::class.java).description.children.first().children[1].displayName)
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


class NSpekRunner(testClass: Class<*>) : Runner() {

    private val rootDescription = Description.createSuiteDescription(testClass).apply {
        testClass.declaredMethods.forEach { method ->
            addChild(Description.createSuiteDescription(method.name).apply {
                val children = runMethod(method, testClass.newInstance())
                children.forEach {
                    addChild(it)
                }
            })
        }
    }

    override fun getDescription(): Description = rootDescription

    override fun run(notifier: RunNotifier?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class NSpekMethodContext(private val parentName: String) {
    val children = mutableListOf<Description>()

    infix fun String.o(code: NSpekMethodContext.() -> Unit) {
        runCode(this, code)
    }

    private fun runCode(currentName: String, code: (NSpekMethodContext) -> Unit) {
        children.add(createDescription(currentName, code))
    }

    private fun createDescription(currentName: String, code: (NSpekMethodContext) -> Unit): Description {
        val children = runCodeWithContext(currentName, code)
        return if (children.isEmpty()) {
            Description.createTestDescription(parentName, currentName)
        } else {
            Description.createSuiteDescription(currentName).apply {
                children.forEach {
                    addChild(it)
                }
            }
        }
    }
}

private fun runMethod(method: Method, instance: Any?): List<Description> {
    val nSpekContext = NSpekMethodContext(method.name)
    method.invoke(instance, nSpekContext)
    return nSpekContext.children
}

private fun runCodeWithContext(currentName: String, code: (NSpekMethodContext) -> Unit): List<Description> {
    val nSpekContext = NSpekMethodContext(currentName)
    code(nSpekContext)
    return nSpekContext.children
}
