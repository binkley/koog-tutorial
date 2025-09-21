import SupportedAgent.Companion.agentFor
import SupportedAgent.Companion.agentNicknames
import UserInteraction.Companion.pickUserInteraction
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.utils.use
import ai.koog.prompt.executor.clients.google.GoogleModels.Gemini2_5Flash
import ai.koog.prompt.executor.clients.google.GoogleModels.Gemini2_5Pro
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaModels.Meta.LLAMA_3_2
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.markdown.Markdown
import kotlinx.coroutines.runBlocking
import org.fusesource.jansi.Ansi.ansi
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader.HISTORY_FILE
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import java.io.Closeable
import java.io.File
import java.net.URI
import java.net.http.HttpClient.newHttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Paths
import kotlin.system.exitProcess
import com.github.ajalt.mordant.terminal.Terminal as OutputTerminal

const val DEFAULT_SYSTEM_PROMPT =
    "You are Kai, an AI assistant. Your personality is friendly and helpful."

val DEFAULT_HISTORY_FILE: File =
    Paths.get(System.getProperty("user.home"), ".kai_history")
        .toFile()

private const val DEFAULT_OLLAMA_ENDPOINT = "http://localhost:11434"

private const val INTERRUPTED_EXIT_CODE = 130 // Following POSIX/Linux standards

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

    override fun help(context: Context) = """
        A rich command-line chat bot powered by Koog and JLine.
        """.trimIndent()

    val agentNickname by option(
        "--agent-nickname", "-M",
        help = "Pick a model agent"
    ).choice(*agentNicknames, ignoreCase = true)
        .default("gemini-flash")

    val historyFile by option(
        "--history-file", "-H",
        help = "Set the history file for previous user-entered prompts"
    ).file()
        .default(DEFAULT_HISTORY_FILE)

    val systemPrompt by option(
        "--system-prompt", "-S",
        help = "Set the system prompt"
    )
        .default(DEFAULT_SYSTEM_PROMPT)

    override fun run() = runBlocking {
        repl(
            agent = agentFor(agentNickname.lowercase())(systemPrompt),
            userOptions = UserOptions(historyFile, systemPrompt)
        )
    }
}

data class UserOptions(
    val historyFile: File,
    val systemPrompt: String
)

fun main(args: Array<String>) {
    // Koog likes to be loquacious -- just show concerns and errors.
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN")

    Kai.main(args)
}

private suspend fun repl(
    agent: AIAgent<String, String>,
    userOptions: UserOptions
) {
    agent.use {
        pickUserInteraction(userOptions).use {
            it.printFromKai("🤖 Kai is ready. Enter 'exit' to quit.")

            while (true) {
                val userInput = try {
                    it.readFromUser("> ") ?: break
                } catch (_: UserInterruptException) {
                    // User or script is terminating NOW -- do not be friendly
                    exitProcess(INTERRUPTED_EXIT_CODE)
                }.trim()

                if (userInput.equals("exit", ignoreCase = true)) {
                    it.printFromKai("Goodbye! 👋")
                    break
                }

                it.printFromLlm(agent.run(userInput))
            }
        }
    }
}

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
        fun agentFor(nickname: String): (String) -> AIAgent<String, String> {
            val agent = models[nickname] ?: throw PrintMessage(
                message = "Unknown agent nickname: $nickname".error,
                statusCode = 2,
                printError = true
            )

            return { systemPrompt ->
                agent.using(systemPrompt)
            }
        }
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
        .uri(URI.create(DEFAULT_OLLAMA_ENDPOINT))
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

sealed interface UserInteraction : Closeable {
    fun raiseError(message: String, exitCode: Int): Nothing
    fun printFromKai(message: String)
    fun readFromUser(prompt: String): String?
    fun printFromLlm(message: String)

    companion object {
        fun pickUserInteraction(userOptions: UserOptions): UserInteraction {
            val hasTerminal = null != System.console()
            return when (hasTerminal) {
                true -> ConsoleInteraction(userOptions)
                else -> StreamInteraction()
            }
        }
    }
}

class ConsoleInteraction(userOptions: UserOptions) : UserInteraction {
    private val inputTerminal = TerminalBuilder.builder()
        .system(true)
        .build()

    private val reader = LineReaderBuilder.builder()
        .variable(HISTORY_FILE, userOptions.historyFile)
        .terminal(inputTerminal)
        .build()

    // TODO: Figure out how to use Mordant themes correctly -- this code doesn't
    //   colorize the whole output
    //    val outputTerminal = OutputTerminal(theme = Theme {
    //        styles["info"] = TextStyle(color = TextColors.yellow)
    //    })
    private val outputTerminal = OutputTerminal()

    override fun raiseError(message: String, exitCode: Int) =
        throw PrintMessage(
            message = message.error,
            statusCode = exitCode,
            printError = 0 == exitCode
        )

    override fun printFromKai(message: String) =
        outputTerminal.println(message.iSay)

    override fun readFromUser(prompt: String) = try {
        reader.readLine(prompt.iSay)
    } catch (_: EndOfFileException) {
        null
    }

    override fun printFromLlm(message: String) =
        outputTerminal.println(Markdown("🤖: $message".aiSays))

    override fun close() = inputTerminal.close()
}

class StreamInteraction : UserInteraction {
    override fun raiseError(message: String, exitCode: Int): Nothing {
        System.err.println("kai: $message")
        exitProcess(exitCode)
    }

    override fun printFromKai(message: String) = Unit

    override fun readFromUser(prompt: String) =
        // TODO: Return `null` and not empty string on empty input (/dev/null)
        generateSequence(::readlnOrNull).joinToString("\n")

    override fun printFromLlm(message: String) = println(message)

    override fun close() = Unit
}

val String.error
    get() = ansi().fgBrightRed().bold().a(this).reset().toString()

val String.iSay
    get() = ansi().fgBrightGreen().bold().a(this).reset().toString()

val String.aiSays
    get() = ansi().fgYellow().a(this).reset().toString()

