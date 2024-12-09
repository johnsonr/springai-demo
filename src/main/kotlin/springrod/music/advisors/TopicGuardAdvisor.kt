package springrod.music.advisors

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain
import org.springframework.ai.chat.client.entity
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation

import org.springframework.core.io.ClassPathResource
import org.springframework.retry.support.RetryTemplate
import org.springframework.retry.support.RetryTemplateBuilder


enum class Topic {
    SPORT,
    RELIGION,
    POLITICS,
    OTHER,
}

// Lots of little private classes can be handy in your LLM interaction code
private data class TopicClassification(
    val topic: Topic,
)

/**
 * Prevent the bot talking about some topics
 */
class TopicGuardAdvisor(
    chatModel: ChatModel,
    private val bannedTopics: Set<Topic>,
    private val userContentExtractor: UserContentExtractor = lastMessageUserContentExtractor,
    private val retryTemplate: RetryTemplate =
        RetryTemplateBuilder().maxAttempts(3).fixedBackoff(1000).build()
) : CallAroundAdvisor {

    private val logger: Logger = LoggerFactory.getLogger(TopicGuardAdvisor::class.java)

    private val chatClient = ChatClient
        .builder(chatModel)
        .build()

    /**
     * We don't change the request, we merely look at it.
     * We perform the additional model call in the background so that
     * we can reply to the user without delay.
     */
    override fun aroundCall(
        advisedRequest: AdvisedRequest,
        chain: CallAroundAdvisorChain
    ): AdvisedResponse {

        val topicIsBanned =
            try {
                // Allow for flaky model
                retryTemplate.execute<Boolean, Throwable> {
                    isBannedTopic(userContentExtractor.invoke(advisedRequest))
                }
            } catch (t: Throwable) {
                logger.error("We tried really hard but the model kept failing. Don't fail the advisor chain", t)
                false
            }

        return if (topicIsBanned) {
            AdvisedResponse.builder()
                .withAdviseContext(advisedRequest.adviseContext)
                .withResponse(
                    ChatResponse.builder().withGenerations(
                        listOf(Generation(AssistantMessage("I'm sorry, but I can only help you with Classical music.")))
                    )
                        .build()
                )
                .build()
        } else chain.nextAroundCall(advisedRequest)
    }

    override fun getName(): String = TopicGuardAdvisor::class.java.simpleName

    override fun getOrder() = 0

    private fun isBannedTopic(userContent: String): Boolean {
        // Independent LLM call, potentially using a different model
        val topicClassification = chatClient
            .prompt()
            .user(ClassPathResource("prompts/topic_guard.md"))
            .user { it.param("content", userContent) }
            .call()
            .entity<TopicClassification>()
        logger.info("User content '$userContent' classified as ${topicClassification.topic}")
        return bannedTopics.contains(topicClassification.topic)
    }

}
