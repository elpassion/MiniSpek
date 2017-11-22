package com.elpassion.nspek

import com.elpassion.mspek.CodeLocation
import com.elpassion.mspek.TestEnd
import com.elpassion.mspek.currentUserCodeLocation
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import java.lang.reflect.InvocationTargetException
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
    val nSpekContext = NSpekMethodContext()
    while (true) {
        try {
            nSpekContext.names.clear()
            method.invoke(testClass.newInstance(), nSpekContext)
            break
        } catch (e: InvocationTargetException) {
            descriptions.addFromNames(nSpekContext.names, method.name)
        }
    }
    return descriptions to notifications
}

private fun MutableList<Description>.addFromNames(names: List<String>, name: String) {
    if (names.isEmpty()) {
        return
    } else if (names.size == 1) {
        add(Description.createTestDescription(name, names.first()))
    } else {
        val suites = names.dropLast(1)
        suites.forEach {
            val suiteDescription = Description.createSuiteDescription(it)
            if (!contains(suiteDescription)) {
                add(suiteDescription)
            }
        }
        add(Description.createTestDescription(last().displayName, names.last()))
    }
}

sealed class Notification {
    data class Start(val description: Description) : Notification()
    data class End(val description: Description) : Notification()
    data class Failure(val description: Description, val cause: Throwable) : Notification()
}

class NSpekMethodContext {
    val finishedTests = mutableSetOf<CodeLocation>()
    val names = mutableListOf<String>()

    infix fun String.o(code: NSpekMethodContext.() -> Unit) {
        if (!finishedTests.contains(currentUserCodeLocation)) {
            names.add(this)
            try {
                code()
                finishedTests.add(currentUserCodeLocation)
                throw TestEnd()
            } catch (ex: TestEnd) {
                throw ex
            } catch (ex: Throwable) {
                finishedTests.add(currentUserCodeLocation)
                throw TestEnd(ex)
            }
        }
    }
}

class TestEnd(cause: Throwable? = null) : RuntimeException(cause)