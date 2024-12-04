package springrod.music.advisors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.client.entity

import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.document.Document
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.io.ClassPathResource
import org.springframework.retry.support.RetryTemplate
import org.springframework.retry.support.RetryTemplateBuilder
import java.util.concurrent.Executor

/**
 * Returns what we need to extract memories from,
 * e.g. recent messages
 */
typealias MemoryBasisExtractor = (a: AdvisedRequest) -> List<Message>

val lastMessageMemoryBasisExtractor: MemoryBasisExtractor = {
    listOf(UserMessage(it.userText.takeBefore("\n\n")))
}

fun String.takeBefore(what: String): String {
    return this.substringBefore(what)
}

/**
 * Capture a memory, similar to OpenAI memory feature
 */
class CaptureMemoryAdvisor(
    private val vectorStore: VectorStore,
    chatModel: OllamaChatModel,
    private val executor: Executor,
    private val memoryBasisExtractor: MemoryBasisExtractor = lastMessageMemoryBasisExtractor,
    private val retryTemplate: RetryTemplate =
        RetryTemplateBuilder().maxAttempts(3).fixedBackoff(1000).build()
) : CallAroundAdvisor {

    private val logger: Logger = LoggerFactory.getLogger(CaptureMemoryAdvisor::class.java)

    private val chatClient = ChatClient
        .builder(chatModel)
        .defaultSystem(ClassPathResource("prompts/capture_memory.md"))
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
                    extractMemoryIfPossible(memoryBasisExtractor.invoke(advisedRequest))
                }
            } catch (t: Throwable) {
                logger.error("We tried really hard but the model kept failing. Don't fail the advisor chain", t)
            }
        }
        executor.execute(backgroundTask)
        return chain.nextAroundCall(advisedRequest)
    }

    override fun getName(): String = CaptureMemoryAdvisor::class.java.simpleName

    override fun getOrder(): Int = 0

    private fun extractMemoryIfPossible(messages:List<Message>): Boolean {
        val memoryLlmResponse = chatClient
            .prompt()
            .messages(messages)
            .call()
            .entity<MemoryLlmResponse>()
        if (memoryLlmResponse.worthKeeping() == true) {
            logger.info("Adding memory: {}", memoryLlmResponse)
            vectorStore.add(
                listOf(
                    Document(
                        """
                    Remember this about the user:
                    ${memoryLlmResponse.content}
                """.trimIndent()
                    )
                )
            )
            return true
        }
        logger.info("Ignoring useless potential memory: {}", memoryLlmResponse)
        return false
    }

    // Lots of little private classes can be handy in your LLM interaction code
    private data class MemoryLlmResponse(
        val content: String? = null,
        val useful: Boolean,
    ) {
        fun worthKeeping() = useful && content != null
    }

}