package springrod.music.advisors

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest

/**
 * Returns the user content we need to analyze
 * e.g. recent messages
 */
typealias UserContentExtractor = (a: AdvisedRequest) -> String

val lastMessageUserContentExtractor: UserContentExtractor = {
    // Remove any context inserted by Spring AI QuestionAnswerAdvisor
    it.userText.takeBefore("\n\n")
}

fun String.takeBefore(what: String): String {
    return this.substringBefore(what)
}