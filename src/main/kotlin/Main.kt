import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels.Gemini2_5Flash
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.llm.LLModel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.fusesource.jansi.Ansi.ansi
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder

private val DEFAULT_SYSTEM_PROMPT = """
        You are Kai, a helpful AI assistant built by a team at Rice University.
        When asked who you are, introduce yourself as "Kai" and mention that you
        are powered by Google's Gemini models. Your personality is friendly and helpful.
    """.trimIndent()

// TODO: ADD COMMAND LINE OPTIONS
// TODO: Consider custom help so we can print the 1-line summary
// private const val DESCRIPTION = "A simple chat bot powered by Koog and Gemini."
// TODO: Show option default values in help

object Kai : CliktCommand("kai") {
    val systemPrompt by option(
        help = "Set the system prompt",
        names = arrayOf("-S", "--system-prompt")
    ).default(DEFAULT_SYSTEM_PROMPT)

    override fun run() = runBlocking {
        run(
            model = Gemini2_5Flash,
            systemPrompt = systemPrompt
        )
    }
}

fun main(args: Array<String>) = Kai.main(args)
suspend fun run(model: LLModel, systemPrompt: String) {
    // Koog and Gemini like to be loquacious -- just show concerns and errors
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN")

    // TODO: Trying out different models (which will have diff env vars)
    //   See SimplePromptExecutors in Koog, and map option name to executor
    // TODO: How to check the API key is valid before saying we are ready?
    val apiKey = (System.getenv("GEMINI_API_KEY")
        ?: throw PrintMessage(
            "kai: Missing GEMINI_API_KEY environment variable".error,
            2,
            true
        ))

    val agent = AIAgent(
        executor = simpleGoogleAIExecutor(apiKey),
        llmModel = model,
        systemPrompt = systemPrompt
    )

    val inputTerminal = TerminalBuilder.builder()
        .system(true)
        .build()
    val outputTerminal = Terminal(theme = Theme {
        styles["info"] = TextStyle(color = TextColors.yellow)
    })

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

val String.error
    get() = ansi().fgBrightRed().bold().a(this).reset().toString()

val String.iSay
    get() = ansi().fgBrightGreen().bold().a(this).reset().toString()

val String.aiSays
    get() = ansi().fgYellow().a(this).reset().toString()
