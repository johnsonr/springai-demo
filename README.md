# Spring AI Demo

Updated for YOW! Australia, December 2024.

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)
![Neo4J](https://img.shields.io/badge/Neo4j-008CC1?style=for-the-badge&logo=neo4j&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Apache Tomcat](https://img.shields.io/badge/apache%20tomcat-%23F8DC75.svg?style=for-the-badge&logo=apache-tomcat&logoColor=black)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)

Kotlin demo project with a simple [HTMX](https://htmx.org/) UI, demonstrating Spring AI with Ollama, Open AI and Neo4j.
Shows:

- RAG using Spring AI's out of the box `QuestionAnswerAdvisor`
- Mixing LLMs in a single application. **Use the right LLM for each requirement.**
- The power of [Spring AI](https://github.com/spring-projects/spring-ai) advisors to instrument chats in a reusable way
- The power of integrating LLM calls within a Spring application by exposing Spring beans as LLM-accessible _functions_.

This project features the following custom advisors:

- `CountMentionsAdvisor`: Detects when a topic is mentioned in a chat and increments an entity counter and raises an
  application event
- `SavePerformanceAdvisor`: Remembers mentions of upcoming performances and saves them to the database. Extraction runs
  asynchronously, so it doesn't slow responding
  to the user.

This project illustrates the following best practices:

- _Externalize your prompts_. Prompts should not be in Kotlin, Java, Python/whatever programming language. They should
  be externalized so they can be edited easily and potentially shared.
- _Mix multiple models_, using the best (or cheapest) LLM for each task.
- Enable reuse via advisors, analogous to aspects in AOP
- Return entities from the LLM
- Use structured persistent data as well as vector search
- _Write in Kotlin_!

## Setup

This is a standard Spring Boot project, built with Maven
and written in Kotlin.

Set the `OPEN_AI_API_KEY` environment variable
to your Open AI token, or edit `ChatConfiguration.kt`
to switch to a different premium chat model.

Use the Docker Compose file in this project to run Neo,
or otherwise change the Neo credentials in `application.properties`
to use your own database.

Run [Ollama](https://ollama.com/) on your machine.
Make sure you've pulled the `gemma2:2b` model as follows:

```bash
docker pull ollama/gemma2:2b
```

Edit `ChatConfiguration.kt` to use a different Ollama model if you prefer.

## Running

- Start the server, either in your IDE or with `mvn spring-boot:run`
- Go to `http://localhost:8080` to see the simple chat interface

## Limitations

This is a demo to illustrate the power of Spring AI advisors,
so it's simplistic.

In particular:

- The `SavePerformanceAdvisor` works off the latest user message only (although this is extracted into a strategy
  function)
- The `CountMentionsAdvisor` looks for a literal string. This could easily be improved to work with a local model and
  exhibit deeper understanding (e.g. "the user is talking about auto service"). It's also inefficient as it scans all
  `Mention` entities on every request.
- The UI is very basic

Contributions welcome.