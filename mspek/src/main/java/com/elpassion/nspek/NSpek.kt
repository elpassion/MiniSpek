package com.elpassion.nspek

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
        val notifiers = customNotifiers + JunitNotifierWrapper(notifier)
        notifications.forEach { notification ->
            notifiers.forEach { notifier ->
                notifier.invoke(notification)
            }
        }
    }

    companion object {
        var customNotifiers = listOf<(Notification) -> Unit>(LoggingNotifier())
    }
}

private class LoggingNotifier : (Notification) -> Unit {
    override fun invoke(it: Notification) {
        when (it) {
            is Notification.Start -> println(it.description.displayName)
            is Notification.End -> println("SUCCESS.(${it.location})\n")
            is Notification.Failure -> {
                println("FAILURE.(${it.location})")
                println("BECAUSE.(${it.cause.causeLocation})")
                println("${it.cause}\n")
            }
        }
    }
}

private class JunitNotifierWrapper(private val notifier: RunNotifier) : (Notification) -> Unit {
    override fun invoke(notification: Notification) {
        when (notification) {
            is Notification.Start -> notifier.fireTestStarted(notification.description)
            is Notification.Failure -> {
                notifier.fireTestFailure(Failure(notification.description, notification.cause))
                notifier.fireTestFinished(notification.description)
            }
            is Notification.End -> notifier.fireTestFinished(notification.description)
        }
    }
}

fun runClassTests(testClass: Class<*>): Pair<Description, List<Notification>> {
    require(testClass.methods.filter { it.getAnnotation(Test::class.java) != null }.isNotEmpty()) { "At least one method should be annotated with com.elpassion.nspek.Test" }
    val descriptions = runMethodsTests(testClass).map { testBranch ->
        testBranch.copy(names = listOf(testClass.name) + testBranch.names)
    }
    val descriptionTree = descriptions.toTree()
    return descriptionTree.getDescriptions().first() to descriptionTree.getNotifications()
}

private fun runMethodsTests(testClass: Class<*>): List<TestBranch> {
    return testClass.declaredMethods.filter { it.getAnnotation(Test::class.java) != null }.flatMap { method ->
        try {
            val results = runMethodTests(method, testClass).map { testBranch ->
                testBranch.copy(names = listOf(method.name) + testBranch.names)
            }
            if (results.isNotEmpty()) {
                results
            } else {
                listOf(TestBranch(names = listOf(method.name), location = currentUserCodeLocation))
            }
        } catch (t: Throwable) {
            listOf(TestBranch(names = listOf(method.name), throwable = t, location = currentUserCodeLocation))
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
            val cause = e.cause
            if (cause is TestEnd) {
                descriptionsNames.add(TestBranch(ArrayList(nSpekContext.names), cause.cause, location = cause.codeLocation))
            } else {
                throw cause!!
            }
        }
    }
    return descriptionsNames
}

data class TestBranch(val names: List<String>, val throwable: Throwable? = null, val location: CodeLocation)

sealed class InfiniteMap : MutableMap<String, InfiniteMap> by mutableMapOf() {
    data class Branch(val throwable: Throwable? = null, val description: Description, val location: CodeLocation) : InfiniteMap()
    class Root : InfiniteMap()
}

private fun List<TestBranch>.toTree(): InfiniteMap {
    val map: InfiniteMap = InfiniteMap.Root()
    forEach { (names, throwable, location) ->
        names.foldIndexed(map, { index, acc, name ->
            acc.getOrPut(name, {
                val description = if (index != names.lastIndex) {
                    Description.createSuiteDescription(name)
                } else {
                    Description.createTestDescription(names[index - 1], name)
                }
                InfiniteMap.Branch(description = description, throwable = throwable, location = location)
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
    return if (this is InfiniteMap.Branch && isEmpty()) {
        val startNotification = listOf(Notification.Start(description, location))
        val endNotification = if (throwable != null) {
            Notification.Failure(description, location, throwable)
        } else {
            Notification.End(description, location)
        }
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
    abstract val description: Description
    abstract val location: CodeLocation

    data class Start(override val description: Description, override val location: CodeLocation) : Notification()
    data class End(override val description: Description, override val location: CodeLocation) : Notification()
    data class Failure(override val description: Description, override val location: CodeLocation, val cause: Throwable) : Notification()
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
                throw TestEnd(codeLocation = currentUserCodeLocation)
            } catch (ex: TestEnd) {
                throw ex
            } catch (ex: Throwable) {
                finishedTests.add(currentUserCodeLocation)
                throw TestEnd(ex, codeLocation = currentUserCodeLocation)
            }
        }
    }
}

class TestEnd(cause: Throwable? = null, val codeLocation: CodeLocation) : RuntimeException(cause)