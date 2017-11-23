package com.elpassion.nspek

import com.elpassion.mspek.CodeLocation
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
    val descriptions = runMethodsTests(testClass)
    val rootDescription = Description.createSuiteDescription(testClass)
    val toTree = descriptions.map { it.names }.toTree(rootDescription)
    rootDescription.addAllChildren(toTree.getDescriptions())
    return rootDescription to notifications
}

private fun runMethodsTests(testClass: Class<*>): List<TestBranch> {
    return testClass.declaredMethods.flatMap { method ->
        runMethodTests(method, testClass).map { testBranch ->
            testBranch.copy(names = listOf(method.name) + testBranch.names)
        }
    }
}

private fun runMethodTests(method: Method, testClass: Class<*>): List<TestBranch> {
    val descriptionsNames = mutableListOf<TestBranch>()
    val nSpekContext = NSpekMethodContext()
    while (true) {
        try {
            nSpekContext.names.clear()
            method.invoke(testClass.newInstance(), nSpekContext)
            break
        } catch (e: InvocationTargetException) {
            if (e.cause is TestEnd) {
                descriptionsNames.add(TestBranch(ArrayList(nSpekContext.names), e.cause?.cause))
            } else {
                break
            }
        }
    }
    return descriptionsNames
}

data class TestBranch(val names: List<String>, val throwable: Throwable? = null)

data class InfiniteMap(val throwable: Throwable? = null, val description: Description) : MutableMap<String, InfiniteMap> by mutableMapOf()

private fun List<List<String>>.toTree(description: Description): InfiniteMap {
    val map = InfiniteMap(description = description)
    forEach { names ->
        val lastName = names.last()
        names.dropLast(1).fold(map, { acc, name ->
            acc.getOrPut(name, { InfiniteMap(description = Description.createSuiteDescription(name)) })
        }).getOrPut(lastName, { InfiniteMap(description = Description.createTestDescription(names.dropLast(1).last(), lastName)) })
    }
    return map
}

private fun InfiniteMap.getDescriptions(): List<Description> {
    return values.map { map ->
        if (map.isNotEmpty()) {
            map.description.addAllChildren(map.getDescriptions())
        } else {
            map.description
        }
    }
}

private fun Description.addAllChildren(descriptions: List<Description>) = apply {
    descriptions.forEach {
        addChild(it)
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