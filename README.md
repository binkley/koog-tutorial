<a href="./LICENSE.md">
<img src="./images/cc0.svg" alt="Creative Commons Public Domain Dedication"
align="right" width="10%" height="auto"/>
</a>

# Koog Tutorial

Kick the tires on Koog.
Koog is a Kotlin library for calling out to various public LLM APIs.

## Setup

### Gemini

Ensure a `GEMINI_API_KEY` from Google is exported in your shell environment.

You API key reflects the rights your Google account has when you generate the
key. For example, this project uses the "Gemini Flash" 2.5 model as a default.

### Ollama

Install and run the "llama3.2:latest" model in Ollama.

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
> Just a friendly reminder, I'm KoogBot, your helpful assistant. How can I help you today? 😊

Knock yourself out with prompts.
