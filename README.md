<a href="./LICENSE.md">
<img src="./images/cc0.svg" alt="Creative Commons Public Domain Dedication"
align="right" width="10%" height="auto"/>
</a>

# Koog Tutorial

Kick the tires on Koog.
Kook is a Kotlin library for calling out to various public LLM APIs.

## Setup

Ensure a `GEMINI_API_KEY` from Google is exported in your shell environment.

You API key reflects the rights your Google account has when you generate the
key. For example, this project uses the "Gemini Flash" 2.5 model. To use the
"Pro" model, you should generate an API key from your account if you have paid
for that model.

## Try it

From the project root, run:

```shell
./gradlew installDist
./build/install/koog-tutorial/bin/koog-tutorial
```

A sample conversation on the command line:

> 🤖 Koog Gemini agent is ready. Type 'exit' to quit.<br>
> \> Hi, Bob!<br>
> 🤖: Hi there! It's great to hear from you!<br>
> <br>
> Just a friendly reminder, I'm KoogBot, your helpful assistant. How can I help you today? 😊

Knock yourself out with prompts.
