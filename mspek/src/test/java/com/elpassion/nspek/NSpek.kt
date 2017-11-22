package com.elpassion.nspek

import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import java.lang.reflect.Method


class NSpekRunner(testClass: Class<*>) : Runner() {
    private val notifications: List<Notification>
    private val rootDescription: Description

    init {
        val (rootDescription, runResult) = doAll(testClass)
        this.rootDescription = rootDescription
        this.notifications = runResult
    }

    override fun getDescription(): Description = rootDescription

    override fun run(notifier: RunNotifier) {
        notifications.forEach {
            when (it) {
                is Notification.Start -> notifier.fireTestStarted(it.description)
                is Notification.Failure -> notifier.fireTestFailure(Failure(it.description, it.cause))
                is Notification.End -> notifier.fireTestFinished(it.description)
            }
        }
    }
}

fun doAll(testClass: Class<*>): Pair<Description, List<Notification>> {
    val notifications = mutableListOf<Notification>()
    val descriptions = Description.createSuiteDescription(testClass).apply {
        testClass.declaredMethods.forEach { method ->
            addChild(Description.createSuiteDescription(method.name).apply {
                notifications.add(Notification.Start(this))
                notifications.add(Notification.End(this))
                val children = runMethod(method, testClass.newInstance())
                children.forEach {
                    addChild(it)
                    notifications.add(Notification.Start(it))
                    it.children.forEach {
                        notifications.add(Notification.Start(it))
                        notifications.add(Notification.End(it))
                    }
                    notifications.add(Notification.End(it))
                }
            })
        }
    }
    notifications.add(Notification.Start(descriptions))
    notifications.add(Notification.End(descriptions))
    return descriptions to notifications
}

sealed class Notification {
    data class Start(val description: Description) : Notification()
    data class End(val description: Description) : Notification()
    data class Failure(val description: Description, val cause: Throwable) : Notification()
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
