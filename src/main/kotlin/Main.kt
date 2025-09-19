import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels.Gemini2_5Flash
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import org.fusesource.jansi.Ansi.ansi
import org.jline.reader.EndOfFileException
import org.jline.reader.Highlighter
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle.DEFAULT

/*
 * This is a good example how to learn more on how the bot works, but it also
 * takes a more time and computation from it.
private const val SYSTEM_PROMPT =
    "You are a helpful and friendly assistant named KoogBot. You explain what you are doing so the user can learn how to make better prompts."
*/
private const val SYSTEM_PROMPT =
    "You are a helpful and friendly assistant named KoogBot."

fun userInput() =
    Highlighter { _, buffer ->
        AttributedString(buffer, DEFAULT.italic())
    }

suspend fun main() {
    // Koog and Gemini like to be loquacious -- just show concerns and errors
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN")

    val apiKey = (System.getenv("GEMINI_API_KEY")
        ?: throw RuntimeException("Missing GEMINI_API_KEY environment variable"))


    
    // TODO: How to check the API key is valid before saying we are ready?
    val gemini = simpleGoogleAIExecutor(apiKey)
    val agent = AIAgent(
        executor = gemini,
        llmModel = Gemini2_5Flash,
        systemPrompt = SYSTEM_PROMPT
    )

    val terminal = TerminalBuilder.builder()
        .system(true)
        .build()

    // TODO: Nicer code. Refactor scope function.
    terminal.use {
        terminal.writer().run {
            println("🤖 Koog Gemini agent is ready. Enter 'exit' to quit.".iSay)

            val reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(userInput())
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

val String.iSay
    get() = ansi().fgBrightGreen().bold().a(this).reset().toString()

val String.aiSays
    get() = ansi().fgYellow().a(this).reset().toString()
