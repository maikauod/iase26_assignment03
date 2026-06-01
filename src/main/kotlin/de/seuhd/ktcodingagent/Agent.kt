package de.seuhd.ktcodingagent

import de.seuhd.ktcodingagent.context.PromptBuilder
import de.seuhd.ktcodingagent.model.ModelClient
import de.seuhd.ktcodingagent.parse.Parsed
import de.seuhd.ktcodingagent.parse.ResponseParser
import de.seuhd.ktcodingagent.session.HistoryEntry
import de.seuhd.ktcodingagent.session.Session
import de.seuhd.ktcodingagent.session.SessionStore
import de.seuhd.ktcodingagent.tools.ToolRegistry
import kotlinx.serialization.json.JsonObject

/**
 * Sub-exercise (c): the agent loop.
 *
 * Implement [ask] per the loop in the assignment sheet:
 *   - record the user message and persist the session
 *   - on each iteration, build the prompt, call modelClient.complete,
 *     parse the response, and act on the three cases (Tool, Final, Retry)
 *   - bound by maxSteps (tool calls) and maxAttempts = 3 * maxSteps
 *   - on tool calls, call session.memory.recordToolCall(...)
 *   - distinguish the two stop conditions in the final message
 *
 * See AgentTest for the contract.
 */
class Agent(
    private val modelClient: ModelClient,
    private val registry: ToolRegistry,
    private val promptBuilder: PromptBuilder,
    val session: Session,
    private val sessionStore: SessionStore,
    private val maxSteps: Int = 16,
    private val maxNewTokens: Int = 1024,
    private val onToolCall: (name: String, args: JsonObject, content: String, isError: Boolean) -> Unit = { _, _, _, _ -> }
) {
    fun ask(userMessage: String): String {
        //("Implement the agent loop (sub-exercise (d)).")
        session.memory.setInitialTask(userMessage)
        session.record(HistoryEntry.UserEntry(userMessage, java.time.OffsetDateTime.now().toString()))
        sessionStore.save(session)

        var steps = 0
        var tries = 0
        val max = maxSteps * 3

        while (steps < maxSteps && tries < max) {
            tries++

            val p = promptBuilder.build(session, userMessage)
            val raw = modelClient.complete(p, maxNewTokens)

            when (val parsed = ResponseParser.parse(raw)) {
                is Parsed.Final -> {
                    session.record(HistoryEntry.AssistantEntry(parsed.text,java.time.OffsetDateTime.now().toString()))
                    session.memory.recordFinal(parsed.text)
                    sessionStore.save(session)
                    return parsed.text }

                is Parsed.Retry -> {
                    session.record(HistoryEntry.AssistantEntry(parsed.notice, java.time.OffsetDateTime.now().toString()))
                    sessionStore.save(session)
                }

                is Parsed.Tool -> {
                    val result = registry.dispatch(parsed.name, parsed.args, session.history)

                    session.record(HistoryEntry.ToolEntry(
                        parsed.name,
                        parsed.args,
                        result.content,
                        result.isError,
                        java.time.OffsetDateTime.now().toString())
                    )

                    session.memory.recordToolCall(
                        parsed.name,
                        parsed.args,
                        result.content
                    )
                    sessionStore.save(session)
                    if (result.isError && result.content.contains("repeated identical tool call")) { break }
                    steps++
                }
            }
        }

        if (steps >= maxSteps) {
            return "Stopped after reaching the step limit without a final answer."
        }
        if(tries >= max){
            return "Stopped after too many malformed model responses without a valid tool call or final answer."
        }
        val lastTool = session.history.filterIsInstance<HistoryEntry.ToolEntry>().lastOrNull { !it.isError }

        return lastTool?.content ?: ""
    }


    fun reset() {
        session.history.clear()
        session.memory.clear()
        sessionStore.save(session)
    }
}
