package de.seuhd.ktcodingagent.parse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Sub-exercise (c): the response parser.
 *
 * Implement [parse] to return one of:
 *   - Parsed.Tool(name, args)   when the raw text contains <tool>{json}</tool>
 *   - Parsed.Final(text)        when the raw text contains <final>...</final>,
 *                               or contains neither tag but is non-empty
 *   - Parsed.Retry(notice)      on empty input, empty <final>, malformed JSON
 *                               inside <tool>, missing "name", or non-object "args"
 *
 * Retry notices follow the form:
 *   "Runtime notice: <problem>. Reply with a valid <tool> call or a non-empty <final> answer."
 *
 * See ResponseParserTest for the contract.
 */
object ResponseParser {
    fun parse(raw: String): Parsed {
        //("Implement the parser (sub-exercise (d)).")
        val text = raw.trim()

        fun retry(problem: String) = Parsed.Retry(
            "Runtime notice: $problem. Reply with a valid <tool> call or a non-empty <final> answer."
        )

        if (text.isEmpty()) {
            return retry("empty response")
        }

        val toolMatch = Regex("<tool>(.*?)</tool>", RegexOption.DOT_MATCHES_ALL)
            .find(text)

        if (toolMatch != null) {
            val jsonText = toolMatch.groupValues[1]

            return try {
                val obj = Json.parseToJsonElement(jsonText).jsonObject

                val name = try {
                    obj["name"]?.jsonPrimitive?.contentOrNull }
                catch (_: Exception) { null } ?: return retry("missing tool name")

                val argsElement =
                    obj["args"]
                        ?: return retry("non-object args")

                if (argsElement !is JsonObject) {
                    return retry("non-object args")
                }

                Parsed.Tool(name, argsElement)
            } catch (_: Exception) {
                retry("malformed tool JSON")
            }
        }

        val finalMatch = Regex("<final>(.*?)</final>", RegexOption.DOT_MATCHES_ALL)
            .find(text)

        if (finalMatch != null) {
            val answer = finalMatch.groupValues[1].trim()

            if (answer.isEmpty()) {
                return retry("empty <final>")
            }

            return Parsed.Final(answer)
        }

        return Parsed.Final(text)
    }
}
