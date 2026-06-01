package de.seuhd.ktcodingagent.context

import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.nio.file.Files

/**
 * Snapshot of stable facts about the workspace, captured once at agent startup.
 *
 * Embedded in the stable prefix of every prompt so the model has consistent situational
 * awareness without us having to re-discover it each turn: branch and default branch, the
 * current `git status --short`, the last few commits, and excerpts of well-known project
 * documents.
 */
@Serializable
data class WorkspaceContext(
    val cwd: String,
    val repoRoot: String,
    val branch: String,
    val defaultBranch: String,
    val status: String,
    val recentCommits: List<String>,
    val projectDocs: Map<String, String>
) {
    fun render(): String {
        val commits = if (recentCommits.isEmpty()) "- none" else recentCommits.joinToString("\n") { "- $it" }
        val docs = if (projectDocs.isEmpty()) {
            "- none"
        } else {
            projectDocs.entries.joinToString("\n") { (path, body) -> "- $path\n$body" }
        }
        return buildString {
            appendLine("Workspace:")
            appendLine("- cwd: $cwd")
            appendLine("- repo_root: $repoRoot")
            appendLine("- branch: $branch")
            appendLine("- default_branch: $defaultBranch")
            appendLine("- status:")
            appendLine(status)
            appendLine("- recent_commits:")
            appendLine(commits)
            appendLine("- project_docs:")
            append(docs)
        }
    }
}

/**
 * Sub-exercise (b): implement [load].
 *
 * Build a WorkspaceContext from the directory at [cwd].
 * - Use ProcessBuilder to run "git rev-parse --show-toplevel" (fall back to cwd if it fails).
 * - "git branch --show-current" (fall back to "-").
 * - "git symbolic-ref --short refs/remotes/origin/HEAD" (fall back to "origin/main"); strip "origin/".
 * - "git status --short" (fall back to "clean"); clip to 1500 chars.
 * - "git log --oneline -5"; split on newlines and drop blanks.
 * - Read AGENTS.md, README.md, build.gradle.kts:
 *   when [walkToRepoRoot] is true (default), from both repo root and cwd (skip duplicates);
 *   when false, from cwd only. Clip each to 1200 chars.
 *
 * All git calls must degrade gracefully (no exceptions if git is missing or this is not a repo).
 *
 * See WorkspaceContextTest for the contract.
 */
object WorkspaceContextLoader {
    fun load(cwd: Path, walkToRepoRoot: Boolean = true): WorkspaceContext {
        //("Implement WorkspaceContext.load (sub-exercise (b)).")
        fun sRunCommand(cwd: Path, command: List<String>): String? {
            return try {
                //from runCommand test:
                val process = ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true).start()
                process.waitFor()
                if (process.exitValue() != 0) {
                    return null
                }

                process.inputStream.bufferedReader().readText().trim()
            } catch(_: Exception){
                null
                }
            }

        val root = sRunCommand(cwd, listOf("git", "rev-parse", "--show-toplevel"))?: cwd.toString()
        val branch = sRunCommand(cwd, listOf("git", "branch", "--show-current"))?: "-"
        val branchpath = (sRunCommand(cwd, listOf("git", "symbolic-ref",  "--short", "refs/remotes/origin/HEAD")) ?: "origin/main").removePrefix("origin/")
        val status = (sRunCommand(cwd, listOf("git", "status", "--short"))?: "clean").take(1500)
        val log = (sRunCommand(cwd, listOf("git", "log", "--oneline" ,"-5"))?: "").lines().filter { it.isNotBlank() }

        val repofiles = mutableMapOf<String, String>()
        val files = listOf("AGENTS.md", "README.md", "build.gradle.kts")
        val searchdir = mutableListOf<Path>()

        if (walkToRepoRoot) {
            searchdir.add( Path.of(root))
        }
        searchdir.add(cwd)

        for (d in searchdir.distinct()){
            for (name in files){
                val file = d.resolve(name)

                if (!Files.isRegularFile(file)){
                    continue
                }

                val key = file.toAbsolutePath().normalize().toString()

                if (repofiles.containsKey(key)){ continue}

                try {
                    val content = Files.readString(file)
                    if (content.length > 1200){
                    repofiles[key] = content.take(1200) + "...[truncated]"}
                    else {repofiles[key] = content}}
                    catch(_: Exception){
                        //ignore
                    }
                }
            }



        return WorkspaceContext(
            cwd = cwd.toString(),
            repoRoot = root,
            branch = branch,
            defaultBranch = branchpath,
            status = status,
            recentCommits = log,
            projectDocs = repofiles
        )

        }
    }

