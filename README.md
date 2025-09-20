<a href="./LICENSE.md">
<img src="./images/cc0.svg" alt="Creative Commons Public Domain Dedication"
align="right" width="10%" height="auto"/>
</a>

# Koog Tutorial

Kick the tires on Koog.
Koog is a Kotlin library for calling out to various public LLM APIs.

## Setup

### Gemini

Set up your environment to call Google Gemini.
Before running with `-M gemini-flash` or `-M gemini-pro` flags, export
`GEMINI_API_KEY` to your enivornment.

### Ollama

Install and run the "llama3.2:latest" model in Ollama.
An example user session:

1. In terminal A run `ollama serve`.
2. In terminal B run `ollama run llama3.2:latest`.
3. In terminal C run this tutorial with `-M ollama`.

## Try it

From the project root, run:

```shell
./gradlew installDist
./build/install/koog-tutorial/bin/koog-tutorial
```

A sample conversation on the command line:

> 🤖 Kai agent is ready. Type 'exit' to quit.<br>
> \> Hi, Bob!<br>
> 🤖: Hi there! It's great to hear from you!<br>
> <br>
> Just a friendly reminder, I'm KoogBot, your helpful assistant. How can I help
> you today? 😊

Knock yourself out with prompts.
