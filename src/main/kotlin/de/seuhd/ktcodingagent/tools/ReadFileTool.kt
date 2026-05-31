package de.seuhd.ktcodingagent.tools

import de.seuhd.ktcodingagent.io.Workspace
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.nio.file.Files

/**
 * Sub-exercise (a): implement [execute].
 *
 * - Read required "path" arg and optional "start" (default 1) and "end" (default 200).
 * - Resolve via workspace.resolveSandboxed(...). If not a regular file, return an error.
 * - Validate the range; return an error on invalid range.
 * - Read the file, slice lines [start..end], and format each line as
 *   "%4d: %s" prefixed by a "# <relpath>" header.
 *
 * See ToolsTest for the contract.
 */
class ReadFileTool(private val workspace: Workspace) : Tool {
    override val name: String = "read_file"
    override val description: String = "Read a UTF-8 file by line range."
    override val schema: Map<String, String> = mapOf(
        "path" to "str",
        "start" to "int=1",
        "end" to "int=200"
    )
    override val risky: Boolean = false

    override fun execute(args: JsonObject): ToolResult {
        //("Implement read_file (sub-exercise (a)).")
        val path = (args["path"] as? JsonPrimitive)?.content ?: "."
        val start = (args["start"] as? JsonPrimitive)?.intOrNull ?: 1
        val end = (args["end"] as? JsonPrimitive)?.intOrNull ?: 200
        val resolved = workspace.resolveSandboxed(path)
        if (!Files.isRegularFile(resolved)) {
            return ToolResult.error("invalid path: $path")
        }
        if (!(start >= 1 && start <= end)){
            return ToolResult.error("invalid range")
        }
        val read = Files.readAllLines(resolved)
        val newstart = start -1
        val newend = minOf((end -1), read.size-1)
        val sliced = read.slice(newstart..newend)
        val format = sliced.mapIndexed { index, line -> "%4d: %s". format(start + index, line)}
        val header = "# $path"
        return ToolResult((listOf(header) + format).joinToString("\n"))
        //4 tests pass
    }
}
