package com.elpassion.mspek

import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import java.util.*

class MiniSpekRunner(testClass: Class<*>) : Runner() {

    private val rootDescription = Description.createSuiteDescription(testClass.simpleName, UUID.randomUUID().toString())

    private val tree = TestTree()

    init {
        tree.reset()
        MiniSpek.log = logToAll(logToTree(tree), ::logToConsole)
        val instance = testClass.newInstance()
        testClass.declaredMethods
                .filter { it.getAnnotation(Test::class.java) !== null }
                .forEach { it.invoke(instance) }
        tree.info = tree.info.copy(state = TestState.SUCCESS)
        rootDescription.addChild(createDescriptions(tree, testClass.name))
    }

    override fun getDescription(): Description = rootDescription

    override fun run(notifier: RunNotifier) = runTree(tree, tree.info.name ?: "UNKNOWN NAME", notifier)

    private fun runTree(branchTree: TestTree, name: String, notifier: RunNotifier) {
        if (branchTree.subtrees.isEmpty()) {
            val description = branchTree.info.description
            notifier.fireTestStarted(description)
            logToConsole(branchTree.info)
            when (branchTree.info.state) {
                TestState.STARTED -> throw IllegalStateException("Tree branch not finished")
                TestState.SUCCESS -> {
                    notifier.fireTestFinished(description)
                }
                TestState.FAILURE -> {
                    notifier.fireTestFailure(Failure(description, branchTree.info.failureCause))
                    notifier.fireTestFinished(description)
                }
            }
        } else {
            branchTree.subtrees.forEach { runTree(it, name + "\n" + it.info.name, notifier) }
        }
    }

    private fun createDescriptions(testBranch: TestTree, testSuite: String): Description {
        val description = createDescription(testBranch, testSuite)
        testBranch.subtrees.forEach {
            val child = createDescriptions(it, testSuite + "." + testBranch.info.name)
            description.addChild(child)
        }
        return description
    }

    private fun createDescription(testBranch: TestTree, testSuite: String): Description {
        return if (testBranch.subtrees.isNotEmpty()) {
            Description.createSuiteDescription(testBranch.info.name, UUID.randomUUID().toString())
        } else {
            Description.createTestDescription(testSuite, testBranch.info.name)
        }.apply {
            testBranch.info = testBranch.info.copy(description = this)
        }
    }
}