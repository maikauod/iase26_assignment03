package de.seuhd.ktcodingagent

import de.seuhd.ktcodingagent.session.HistoryEntry
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.assertEquals
import kotlin.test.Test
import java.nio.file.Files

/**
 * Sub-exercise (d): add at least three scripted scenarios in this class that exercise
 * [Agent] via [StubModelClient], beyond the twelve cases provided in [AgentTest].
 *
 * Suggested scenarios:
 *   1. A sequence where the agent receives a tool-error response and surfaces it.
 *   2. A sequence where the model attempts a path-safety violation that the
 *      sandbox rejects inside the loop.
 *   3. One scenario of your own design.
 *
 * Use [buildAgentForTest] from [AgentTestSupport] to wire up an Agent with your
 * scripted StubModelClient outputs, then assert on `agent.session.history` and the
 * returned final answer.
 *
 * This class ships empty so it does not contribute to the failing-test count. JUnit
 * picks up no tests until you add @Test methods.
 */
class ScriptedAgentTest{

    //1. A sequence where the agent receives a tool-error response and surfaces it.
    @Test
    fun `tool error response`(){
        val (agent,_) = buildAgentForTest(Files.createTempDirectory("agenttest"),
            listOf("""<tool>{"name":"read_file","args":{"path":"does_not_exist.txt"}}</tool>""", "<final>finished</final>"))
        val res = agent.ask("read file")
        assertEquals("finished", res)
        val tool = agent.session.history.filterIsInstance<HistoryEntry.ToolEntry>().first()
        assertTrue(tool.isError, "error missing in logs")
    }

    //2. A sequence where the model attempts a path-safety violation that the sandbox rejects inside the loop.
    @Test
    fun `safe path rejection scenario`() {
        val (agent, _) = buildAgentForTest(
            Files.createTempDirectory("agenttest"),
            listOf("""<tool>{"name":"read_file","args":{"path":"../secret"}}</tool>""", "<final>ok</final>")
        )
        val result = agent.ask("unsafe read")

        assertEquals("ok", result)

        assertTrue(agent.session.history.any { it is HistoryEntry.ToolEntry })
    }
    //3. own scenario
    @Test
    fun `empty response triggers retry`(){
        val (agent, _) = buildAgentForTest(Files.createTempDirectory("agenttest"), listOf("", "<final>recovered</final>"))
        val result = agent.ask("sth")
        assertEquals("recovered", result)
        assertTrue(agent.session.history.any { it is HistoryEntry.AssistantEntry }, "retry logs missing")
    }

}
