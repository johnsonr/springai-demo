package springrod.music.advisors

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain
import org.springframework.ai.chat.client.entity

import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.core.io.ClassPathResource
import org.springframework.data.annotation.Id
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.support.UUIDStringGenerator
import org.springframework.retry.support.RetryTemplate
import org.springframework.retry.support.RetryTemplateBuilder
import java.util.Date
import java.util.concurrent.Executor

/**
 * Returns what we need to extract memories from,
 * e.g. recent messages
 */
typealias UserContentExtractor = (a: AdvisedRequest) -> String

val lastMessageUserContentExtractor: UserContentExtractor = {
    it.userText.takeBefore("\n\n")
}

fun String.takeBefore(what: String): String {
    return this.substringBefore(what)
}

/**
 * Save an upcoming performance mentioned by the user
 */
class SavePerformanceAdvisor(
    private val neo4jTemplate: Neo4jTemplate,
    chatModel: OllamaChatModel,
    private val executor: Executor,
    private val userContentExtractor: UserContentExtractor = lastMessageUserContentExtractor,
    private val retryTemplate: RetryTemplate =
        RetryTemplateBuilder().maxAttempts(3).fixedBackoff(1000).build()
) : CallAroundAdvisor {

    private val logger: Logger = LoggerFactory.getLogger(SavePerformanceAdvisor::class.java)

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
        // Allow for flaky model
        val backgroundTask = Runnable {
            try {
                retryTemplate.execute<Boolean, Throwable> {
                    extractMemoryIfPossible(userContentExtractor.invoke(advisedRequest))
                }
            } catch (t: Throwable) {
                logger.error("We tried really hard but the model kept failing. Don't fail the advisor chain", t)
            }
        }
        executor.execute(backgroundTask)
        return chain.nextAroundCall(advisedRequest)
    }

    override fun getName(): String = SavePerformanceAdvisor::class.java.simpleName

    override fun getOrder() = 0

    private fun extractMemoryIfPossible(userContent: String): Boolean {
        // Independent LLM call
        val performanceResponse = chatClient
            .prompt()
            .user(ClassPathResource("prompts/save_performance.md"))
            // Prompt that go into template rendering
            .user { it.param("content", userContent) }
            .user { it.param("now", Date()) }
            .call()
            .entity<PerformanceResponse>()
        performanceResponse.performance?.let {
            logger.info("Adding performance: {}", it)
            neo4jTemplate.save(it)
            return true
        }
        return false
    }

    // Lots of little private classes can be handy in your LLM interaction code
    private data class PerformanceResponse(
        val work: String? = null,
        val date: Date? = null,
        val composer: String? = null,
    ) {
        val performance: Performance? =
            if (work != null && composer != null && date != null) Performance(
                work = work,
                composer = composer,
                date = date,
            ) else null
    }

}

@Node
data class Performance(
    val work: String,
    val composer: String,
    val date: Date,
    @Id @GeneratedValue(UUIDStringGenerator::class)
    private val id: String? = null,
)