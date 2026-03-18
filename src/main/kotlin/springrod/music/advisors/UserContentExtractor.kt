package springrod.music.advisors

import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.messages.UserMessage

/**
 * Returns the user content we need to analyze
 * e.g. recent user messages
 */
typealias UserContentExtractor = (a: ChatClientRequest) -> String

val lastMessageUserContentExtractor: UserContentExtractor = {
    // Remove any context added by Spring AI QuestionAnswerAdvisor
    // to user-authored text
    val userText = it.prompt().getUserMessage()?.text ?: ""
    userText.takeBefore("\n\n")
}

fun String.takeBefore(what: String): String {
    return this.substringBefore(what)
}
