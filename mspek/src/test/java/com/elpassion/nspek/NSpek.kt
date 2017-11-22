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
        val (rootDescription, runResult) = runClassTests(testClass)
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

fun runClassTests(testClass: Class<*>): Pair<Description, List<Notification>> {
    val notifications = mutableListOf<Notification>()
    val classDescription = Description.createSuiteDescription(testClass)
    notifications.add(Notification.Start(classDescription))
    runMethodsTests(testClass)
            .run {
                first.forEach {
                    classDescription.addChild(it)
                }
                notifications.addAll(second)
            }
    notifications.add(Notification.End(classDescription))
    return classDescription to notifications
}

private fun runMethodsTests(testClass: Class<*>): Pair<List<Description>, List<Notification>> {
    val notifications = mutableListOf<Notification>()
    val descriptions = mutableListOf<Description>()

    testClass.declaredMethods.map { method ->
        val methodDescription = Description.createSuiteDescription(method.name)
        descriptions.add(methodDescription)
        notifications.add(Notification.Start(methodDescription))
        runMethodTests(method, testClass).run {
            first.forEach {
                methodDescription.addChild(it)
            }
            notifications.addAll(second)
        }
        notifications.add(Notification.End(methodDescription))
    }
    return descriptions to notifications
}

private fun runMethodTests(method: Method, testClass: Class<*>): Pair<MutableList<Description>, MutableList<Notification>> {
    val notifications = mutableListOf<Notification>()
    val descriptions = mutableListOf<Description>()

    val nSpekContext = NSpekMethodContext(method.name)
    method.invoke(testClass.newInstance(), nSpekContext)
    nSpekContext.pairs().run {
        first.forEach {
            descriptions.add(it)
        }
        notifications.addAll(second)
    }
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

    fun pairs(): Pair<MutableList<Description>, MutableList<Notification>> {
        val notifications = mutableListOf<Notification>()
        val descriptions = mutableListOf<Description>()

        children.forEach {
            descriptions.add(it)
            notifications.add(Notification.Start(it))
            it.children.forEach {
                notifications.add(Notification.Start(it))

                notifications.add(Notification.End(it))
            }
            notifications.add(Notification.End(it))
        }
        return descriptions to notifications
    }
}

private fun runCodeWithContext(currentName: String, code: (NSpekMethodContext) -> Unit): List<Description> {
    val nSpekContext = NSpekMethodContext(currentName)
    try {
        code(nSpekContext)
    } catch (e: Exception) {

    }
    return nSpekContext.children
}
