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
    val descriptions = runMethodsTests(testClass).map { testBranch ->
        testBranch.copy(names = listOf(testClass.name) + testBranch.names)
    }
    val descriptionTree = descriptions.toTree()
    return descriptionTree.getDescriptions().first() to descriptionTree.getNotifications()
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

sealed class InfiniteMap : MutableMap<String, InfiniteMap> by mutableMapOf() {
    data class Branch(val throwable: Throwable? = null, val description: Description) : InfiniteMap()
    class Root : InfiniteMap()
}

private fun List<TestBranch>.toTree(): InfiniteMap {
    val map: InfiniteMap = InfiniteMap.Root()
    forEach { (names, throwable) ->
        names.foldIndexed(map, { index, acc, name ->
            acc.getOrPut(name, {
                if (index != names.lastIndex) {
                    InfiniteMap.Branch(description = Description.createSuiteDescription(name), throwable = throwable)
                } else {
                    InfiniteMap.Branch(description = Description.createTestDescription(names[index - 1], name), throwable = throwable)
                }
            })
        })
    }
    return map
}

private fun InfiniteMap.getDescriptions(): List<Description> {
    return values.filterIsInstance<InfiniteMap.Branch>().map { map ->
        if (map.isNotEmpty()) {
            map.description.addAllChildren(map.getDescriptions())
        } else {
            map.description
        }
    }
}

private fun InfiniteMap.getNotifications(): List<Notification> {
    return if (this is InfiniteMap.Branch) {
        val startNotification = listOf(Notification.Start(description))
        val endNotification = listOf(if (throwable != null) Notification.Failure(description, throwable) else Notification.End(description))
        startNotification + values.flatMap { it.getNotifications() } + endNotification
    } else {
        values.flatMap { it.getNotifications() }
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