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

private val DEFAULT_SYSTEM_PROMPT = """
        You are Kai, a helpful AI assistant built by a team at Rice University.
        When asked who you are, introduce yourself as "Kai" and mention that you
        are powered by a model the user choose from the command line.
        Your personality is friendly and helpful.
    """.trimIndent()

// TODO: ADD COMMAND LINE OPTIONS
// TODO: Consider custom help so we can print the 1-line summary
// private const val DESCRIPTION = "A simple chat bot powered by Koog and Gemini."
// TODO: How to deduplicate listing/branching on each model "nickname"?

object Kai : CliktCommand("kai") {
    init {
        // This block customizes the help output
        context {
            helpFormatter = {
                MordantHelpFormatter(
                    it,
                    showDefaultValues = true,
                )
            }
        }
    }

    val model by option(
        "-M", "--model",
        help = "Pick a model",
    ).choice("gemini-flash", "gemini-pro", "ollama")
        .default("gemini-flash")

    val systemPrompt by option(
        "-S", "--system-prompt",
        help = "Set the system prompt",
    ).default(DEFAULT_SYSTEM_PROMPT)

    override fun run() = runBlocking { repl(agentFor(model)(systemPrompt)) }
}

fun main(args: Array<String>) {
    // Koog and Gemini like to be loquacious -- just show concerns and errors
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN")

    Kai.main(args)
}

private suspend fun repl(agent: AIAgent<String, String>) {
    val inputTerminal = TerminalBuilder.builder()
        .system(true)
        .build()
    val outputTerminal = Terminal()
    // TODO: Figure out how to use Mordant themes correctly -- this code doesn't
    //   colorize the whole output
//    val outputTerminal = Terminal(theme = Theme {
//        styles["info"] = TextStyle(color = TextColors.yellow)
//    })

    // TODO: Nicer code. Refactor scope function.
    inputTerminal.use {
        inputTerminal.writer().run {
            println("🤖 Kai is ready. Enter 'exit' to quit.".iSay)

            val reader = LineReaderBuilder.builder()
                .terminal(inputTerminal)
                .build()

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

private fun agentFor(model: String) = when (model.lowercase()) {
    "gemini-flash" -> googleAgentFor(Gemini2_5Flash)
    "gemini-pro" -> googleAgentFor(Gemini2_5Pro)
    "ollama" -> ollamaAgentFor(LLAMA_3_2)
    // TODO: Turn from exception to helpful error message with Clikt
    else -> throw RuntimeException("BUG: Unknown model: $model")
}

private fun googleAgentFor(llmModel: LLModel): (String) -> AIAgent<String, String> {
    // TODO: Trying out different models (which will have diff env vars)
    //   See SimplePromptExecutors in Koog, and map option name to executor
    // TODO: How to check the API key is valid before saying we are ready?
    val apiKey = apiKeyFromEnvironment("GEMINI_API_KEY")

    return { systemPrompt ->
        AIAgent(
            executor = simpleGoogleAIExecutor(apiKey),
            llmModel = llmModel,
            systemPrompt = systemPrompt
        )
    }
}

private fun ollamaAgentFor(llmModel: LLModel): (String) -> AIAgent<String, String> {
    // TODO: Nicer error message with Clikt
    if (!isOllamaRunning()) throw RuntimeException("Ollama is not running")

    return { systemPrompt ->
        AIAgent(
            executor = simpleOllamaAIExecutor(),
            llmModel = llmModel,
            systemPrompt = systemPrompt
        )
    }
}

private fun apiKeyFromEnvironment(envVar: String) = (System.getenv(envVar)
    ?: throw PrintMessage(
        "kai: Missing $envVar environment variable".error,
        2,
        true
    ))

private fun isOllamaRunning(): Boolean {
    val client = newHttpClient()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:11434"))
        .GET()
        .build()

    return try {
        val response = client.send(request, BodyHandlers.ofString())
        // Check for a successful status code
        200 == response.statusCode()
    } catch (_: Exception) {
        // Any exception (e.g., ConnectException) means the server is not running
        false
    }
}

val String.error
    get() = ansi().fgBrightRed().bold().a(this).reset().toString()

val String.iSay
    get() = ansi().fgBrightGreen().bold().a(this).reset().toString()

val String.aiSays
    get() = ansi().fgYellow().a(this).reset().toString()
