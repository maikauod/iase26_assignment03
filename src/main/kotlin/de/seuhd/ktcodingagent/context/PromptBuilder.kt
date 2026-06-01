package de.seuhd.ktcodingagent.context

import de.seuhd.ktcodingagent.session.HistoryEntry
import de.seuhd.ktcodingagent.session.Session
import de.seuhd.ktcodingagent.tools.Tool

/**
 * Sub-exercise (b): implement [build].
 *
 * The full prompt is:
 *   prefix + "\n\n" + memoryText + "\n\nTranscript:\n" + historyText + "\n\nCurrent user request:\n" + userMessage
 *
 * The stable prefix (built once at construction; byte-identical across calls):
 *   <promptPreamble>
 *
 *   Tools:
 *   - <name>(<schema fields joined by ", ">) [safe|approval required] <description>
 *   ... (one line per tool)
 *
 *   Valid response examples:
 *   <tool>{"name":"list_files","args":{"path":"."}}</tool>
 *   <tool>{"name":"read_file","args":{"path":"README.md","start":1,"end":80}}</tool>
 *   <tool>{"name":"write_file","args":{"path":"hello.txt","content":"hi\n"}}</tool>
 *   <final>Done.</final>
 *
 *   <workspace.render()>
 *
 * The memory text:
 *   Memory:
 *   - task: <task or "->">
 *   - files: <comma-separated paths or "->">
 *   - notes:
 *   - <note>
 *   ...
 *
 * The transcript text:
 *   - "- empty" when history is empty
 *   - otherwise, one or two lines per entry:
 *     ToolEntry      -> "[tool:<name>] <args as compact JSON>" then clipped content
 *     UserEntry      -> "[user] <clipped content>"
 *     AssistantEntry -> "[assistant] <clipped content>"
 *   - recent window: entries with index >= max(0, history.size - 6) use limit 900;
 *     older entries use 180 (tool) or 220 (user/assistant)
 *   - final transcript clipped to MAX_HISTORY = 12000 chars
 *
 * See PromptBuilderTest for the contract.
 */
class PromptBuilder(
    private val promptPreamble: String,
    private val tools: List<Tool>,
    private val workspace: WorkspaceContext
) {
    fun build(session: Session, userMessage: String): String {
        //("Implement PromptBuilder.build (sub-exercise (b)).")
        val memo = session.memory

        val files = if (memo.files.isEmpty()) {
            "->"
        } else {
            memo.files.joinToString(", ")
        }

        val notes = if (memo.notes.isEmpty()) {"" }
        else {memo.notes.joinToString("\n") { "- $it" }}

        val memoText = buildString {
            appendLine("Memory:")
            appendLine("- task: ${memo.task.ifBlank { "->" }}")
            appendLine("- files: $files")
            appendLine("- notes:")
            if (notes.isNotEmpty()) append(notes)
        }.trimEnd()

        val transcript = if (session.history.isEmpty()) {"- empty"}
            else{session.history.joinToString("\n"){
                entry ->
                when (entry){
                is HistoryEntry.ToolEntry ->"[tool: ${entry.name}] ${entry.args}"
                is HistoryEntry.UserEntry -> "[user] ${entry.content}"
                is HistoryEntry.AssistantEntry -> "[assistant] ${entry.content}"
                }
                }}

            return buildString {
                append(prefix())
                append("\n\n")
                append(memoText)
                append("\n\nTranscript:\n")
                append(transcript)
                append("\n\nCurrent user request:\n")
                append(userMessage)
            }
        }


    fun prefix(): String {
        //("Implement PromptBuilder.prefix (sub-exercise (b)). ")
        val toolLines = tools.joinToString("\n") { tool ->
            val schema = tool.schema.entries.joinToString(", ") { "${it.key}:${it.value}" }
            val safety = if (tool.risky) "[approval required]" else "[safe]"
            "- ${tool.name}($schema) $safety ${tool.description}"
        }

        return buildString {
            appendLine(promptPreamble)
            appendLine()
            appendLine("Tools:")
            appendLine(toolLines)
            appendLine()
            appendLine("Valid response examples:")
            appendLine("""<tool>{"name":"list_files","args":{"path":"."}}</tool>""")
            appendLine("""<tool>{"name":"read_file","args":{"path":"README.md","start":1,"end":80}}</tool>""")
            appendLine("""<tool>{"name":"write_file","args":{"path":"hello.txt","content":"hi\n"}}</tool>""")
            appendLine("""<final>Done.</final>""")
            appendLine()
            append(workspace.render())
        }
    }
}
