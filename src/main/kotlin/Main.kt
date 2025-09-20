import SupportedAgent.Companion.agentFor
import SupportedAgent.Companion.agentNicknames
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels.Gemini2_5Flash
import ai.koog.prompt.executor.clients.google.GoogleModels.Gemini2_5Pro
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaModels.Meta.LLAMA_3_2
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.fusesource.jansi.Ansi.ansi
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import java.net.URI
import java.net.http.HttpClient.newHttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

private const val DEFAULT_SYSTEM_PROMPT =
    "You are Kai, an AI assistant. Your personality is friendly and helpful."

// TODO: Consider custom help so we can print the 1-line summary
// private const val DESCRIPTION = "A simple chat bot powered by Koog and Gemini."
// TODO: How to deduplicate listing/branching on each model "nickname"?

object Kai : CliktCommand("kai") {
    init {
        context {
            helpFormatter = {
                MordantHelpFormatter(
                    it,
                    showDefaultValues = true,
                )
            }
        }
    }

    val nickname by option(
        "-M", "--model",
        help = "Pick a model agent",
    ).choice(*agentNicknames, ignoreCase = true)
        .default("gemini-flash")

    val systemPrompt by option(
        "-S", "--system-prompt",
        help = "Set the system prompt",
    ).default(DEFAULT_SYSTEM_PROMPT)

    override fun run() = runBlocking {
        repl(agentFor(nickname.lowercase(), systemPrompt))
    }
}

fun main(args: Array<String>) {
    // Koog likes to be loquacious -- just show concerns and errors.
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN")

    Kai.main(args)
}

private suspend fun repl(agent: AIAgent<String, String>) {
    val inputTerminal = TerminalBuilder.builder().system(true).build()
    val outputTerminal = Terminal()
    // TODO: Figure out how to use Mordant themes correctly -- this code doesn't
    //   colorize the whole output
    //    val outputTerminal = Terminal(theme = Theme {
    //        styles["info"] = TextStyle(color = TextColors.yellow)
    //    })

    // TODO: Nicer code. Refactor scope function.
    // TODO: Can we combine terminal handling into one more readable place?
    inputTerminal.use {
        inputTerminal.writer().run {
            val reader = LineReaderBuilder.builder()
                .terminal(inputTerminal)
                .build()

            println("🤖 Kai is ready. Enter 'exit' to quit.".iSay)

            while (true) {
                val userInput = try {
                    reader.readLine("> ".iSay) ?: break
                } catch (_: EndOfFileException) {
                    "exit"
                }.trim()

                if (userInput.equals("exit", ignoreCase = true)) {
                    println("Goodbye! 👋".iSay)
                    break
                }

                val response = agent.run(userInput)
                outputTerminal.println(Markdown("🤖: $response".aiSays))
            }
        }
    }

    agent.close()
}

val String.error
    get() = ansi().fgBrightRed().bold().a(this).reset().toString()

val String.iSay
    get() = ansi().fgBrightGreen().bold().a(this).reset().toString()

val String.aiSays
    get() = ansi().fgYellow().a(this).reset().toString()

sealed class SupportedAgent(
    val llmModel: LLModel
) {
    abstract fun using(systemPrompt: String): AIAgent<String, String>

    companion object {
        private val models = mapOf(
            "gemini-flash" to GeminiFlashAgent(),
            "gemini-pro" to GeminiProAgent(),
            "ollama" to OllamaAgent()
        )

        val agentNicknames = models.keys.sorted().toTypedArray()

        @Throws(PrintMessage::class)
        fun agentFor(nickname: String, systemPrompt: String) =
            models[nickname]?.using(systemPrompt)
                ?: throw PrintMessage(
                    message = "Unknown agent nickname: $nickname".error,
                    statusCode = 2,
                    printError = true
                )
    }
}

abstract class GoogleAgent(llModel: LLModel) : SupportedAgent(llModel) {
    override fun using(systemPrompt: String): AIAgent<String, String> {
        val apiKey = requiredFromEnvironment("GEMINI_API_KEY")

        return AIAgent(
            executor = simpleGoogleAIExecutor(apiKey),
            llmModel = llmModel,
            systemPrompt = systemPrompt
        )
    }
}

class GeminiFlashAgent : GoogleAgent(Gemini2_5Flash)

class GeminiProAgent : GoogleAgent(Gemini2_5Pro)

class OllamaAgent : SupportedAgent(LLAMA_3_2) {
    override fun using(systemPrompt: String): AIAgent<String, String> {
        requireOllamaRunningLocally()

        return AIAgent(
            executor = simpleOllamaAIExecutor(),
            llmModel = llmModel,
            systemPrompt = systemPrompt
        )
    }
}

@Throws(PrintMessage::class)
private fun requiredFromEnvironment(envVar: String) =
    System.getenv(envVar) ?: throw PrintMessage(
        message = "kai: Missing $envVar environment variable".error,
        statusCode = 2,
        printError = true
    )

@Throws(PrintMessage::class)
private fun requireOllamaRunningLocally() {
    val client = newHttpClient()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:11434"))
        .GET()
        .build()

    try {
        val response = client.send(request, BodyHandlers.ofString())
        if (200 == response.statusCode()) return
        throw PrintMessage(
            message = "kai: Ollama is sad: $response".error,
            statusCode = 1,
            printError = true
        )
    } catch (_: Exception) {
        throw PrintMessage(
            message = "kai: Is Ollama running locally?".error,
            statusCode = 2,
            printError = true
        )
    }
}
