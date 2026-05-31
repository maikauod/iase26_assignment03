package de.seuhd.ktcodingagent.tools

import de.seuhd.ktcodingagent.io.Workspace
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import kotlin.collections.sortWith

/**
 * Sub-exercise (a): implement [execute].
 *
 * - Read the optional "path" arg (default ".") and resolve it via workspace.resolveSandboxed(...).
 * - If not a directory, return an error ToolResult.
 * - List entries (directories first, then files; alphabetic). Hide IGNORED_PATH_NAMES.
 * - Format each entry as "[D] relpath" or "[F] relpath". Return "(empty)" if none.
 *
 * See ToolsTest for the contract.
 */
private val IGNORED_PATH_NAMES = setOf(".git", ".kt-coding-agent", "build", ".gradle", ".idea")

class ListFilesTool(private val workspace: Workspace) : Tool {
    override val name: String = "list_files"
    override val description: String = "List files in the workspace."
    override val schema: Map<String, String> = mapOf("path" to "str='.'")
    override val risky: Boolean = false

    override fun execute(args: JsonObject): ToolResult { // input json probs in form of {path:src}
       // ("Implement list_files (sub-exercise (a)).")
        val path = (args["path"] as? JsonPrimitive)?.content ?: "."
        val resolved = workspace.resolveSandboxed(path)
        if (!Files.isDirectory(resolved)) {
            return ToolResult.error("invalid path: $path")}
        val entries = Files.list(resolved).toList()
        val filtered = entries.filter {
            it.fileName.toString() !in IGNORED_PATH_NAMES
            }
        val sortAlpha = filtered.sortedWith(
            compareBy<java.nio.file.Path>
            { !Files.isDirectory(it) }
                .thenBy { it.fileName.toString() }
        )

        val output = sortAlpha.map {
            entry ->  val prefix = if (Files.isDirectory(entry)) "[D]" else "[F]"
            val relative = workspace.root.relativize(entry)
            "$prefix $relative"
        }

        if (output.isEmpty()){
            return ToolResult("empty")
        }

        return ToolResult(output.joinToString("\n"))

    }
    //+2 tests Pass
}
