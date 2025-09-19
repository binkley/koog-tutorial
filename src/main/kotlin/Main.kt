import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels.Gemini2_5Flash
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.llm.LLModel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.fusesource.jansi.Ansi.ansi
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder

/*
 * This is a good example how to learn more on how the bot works, but it also
 * takes a more time and computation from it.
private const val SYSTEM_PROMPT =
    "You are a helpful and friendly assistant named Kai. You explain what you are doing so the user can learn how to make better prompts."
*/
private const val DEFAULT_SYSTEM_PROMPT =
    "You are a helpful and friendly assistant named Kai."

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

    val terminal = TerminalBuilder.builder()
        .system(true)
        .build()

    // TODO: Nicer code. Refactor scope function.
    terminal.use {
        terminal.writer().run {
            println("🤖 Kai is ready. Enter 'exit' to quit.".iSay)

            val reader = LineReaderBuilder.builder()
                .terminal(terminal)
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

                println("🤖: $response".aiSays)
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
