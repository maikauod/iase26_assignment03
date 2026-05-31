package de.seuhd.ktcodingagent.tools

import de.seuhd.ktcodingagent.io.Workspace
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files

/**
 * Sub-exercise (a): implement [execute].
 *
 * - Read required "path" and "content" args.
 * - Resolve via workspace.resolveSandboxed(...). If the target is an existing directory, return an error.
 * - Create parent directories as needed; write the content as UTF-8.
 * - Return ToolResult("wrote <relpath> (<n> chars)").
 *
 * See ToolsTest for the contract.
 */
class WriteFileTool(private val workspace: Workspace) : Tool {
    override val name: String = "write_file"
    override val description: String = "Write a text file."
    override val schema: Map<String, String> = mapOf(
        "path" to "str",
        "content" to "str"
    )
    override val risky: Boolean = true

    override fun execute(args: JsonObject): ToolResult {
        //("Implement write_file (sub-exercise (a)).")
        val path = (args["path"] as? JsonPrimitive)?.content ?: "."
        val content = (args["content"] as? JsonPrimitive)?.content !!
        val resolved = workspace.resolveSandboxed(path)
        if (Files.isDirectory(resolved)) {
            return ToolResult.error("directory already exists")
        }
        Files.createDirectories(resolved.parent)
        Files.writeString(resolved, content)
        return ToolResult("wrote $path (${content.length} chars)")
    }
}
