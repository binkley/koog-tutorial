import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels.Gemini2_5Flash
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import org.fusesource.jansi.Ansi.ansi
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder

// 'suspend' is needed because AI calls are asynchronous network requests
suspend fun main() {
    println("🤖 Koog Gemini agent is ready. Type 'exit' to quit.")

    // 1. Initialize the Gemini client.
    // Koog automatically looks for the GEMINI_API_KEY environment variable.

    val gemini = simpleGoogleAIExecutor(
        System.getenv("GEMINI_API_KEY")
            ?: throw RuntimeException("Missing GEMINI_API_KEY environment variable")
    )

    // 2. Create an agent with a system prompt.
    // This gives the AI its core instructions or personality.

    val agent = AIAgent(
        executor = gemini,
        llmModel = Gemini2_5Flash,
        systemPrompt = "You are a helpful and friendly assistant named KoogBot."
    )

    // 3. Use a _nice_ command line prompt.

    val terminal = TerminalBuilder.builder()
        .system(true)
        .build()

    // 4. Start an interactive chat loop.

    terminal.use {
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
                terminal.writer().println("Goodbye! 👋".iSay)
                break
            }

            // 4. Send the user's message to the agent and get a response.
            val response = agent.run(userInput)

            // 5. Print the agent's response content.
            terminal.writer().println("🤖: $response".aiSays)
        }
    }

    // Clean up the client's resources before exiting
    agent.close()
}

val String.iSay
    get() = ansi().fgBrightDefault().bold().a(this).reset().toString()

val String.aiSays
    get() = ansi().fgYellow().a(this).reset().toString()
