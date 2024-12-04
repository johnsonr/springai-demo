package springrod.music

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.model.function.FunctionCallback
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.stereotype.Service
import springrod.music.advisors.CaptureMemoryAdvisor
import springrod.music.advisors.CountMentionsAdvisor
import java.util.concurrent.Executor

@Service
class ChatService(
    private val chatModel: ChatModel,
    private val localChatModel: OllamaChatModel,
    private val vectorStore: VectorStore,
    private val neo4jTemplate: Neo4jTemplate,
    private val executor: Executor,
    private val applicationEventPublisher: ApplicationEventPublisher,
    @Qualifier("listPopularThings") private val listPopularThings: FunctionCallback,
) {

    /**
     * Some advisors depend on session state
     */
    private fun chatClientForSession(conversationSession: ConversationSession): ChatClient {
        return ChatClient
            .builder(chatModel)
            .defaultAdvisors(
                // Out of the box advisor, adds conversation memory
                MessageChatMemoryAdvisor(conversationSession.chatMemory),
                CountMentionsAdvisor(
                    applicationEventPublisher = applicationEventPublisher,
                    neo4jTemplate = neo4jTemplate,
                ),
                CaptureMemoryAdvisor(
                    chatModel = localChatModel,
                    vectorStore = vectorStore,
                    executor = executor,
                ),
                // Out of the box advisor, handles RAG
                QuestionAnswerAdvisor(
                    vectorStore,
                    SearchRequest.defaults().withSimilarityThreshold(.8)
                ),
               // Edit application.properties to show log messages from this advisor
                SimpleLoggerAdvisor(),
            )
            .defaultSystem(conversationSession.promptResource())
            .defaultFunctions(
                listPopularThings
            )
            .build()
    }

    fun respondToUserMessage(
        conversationSession: ConversationSession,
        userMessage: String,
    ): ChatResponse {
        val chatResponse = chatClientForSession(conversationSession)
            .prompt()
            .advisors { it.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationSession.conversationId) }
            .advisors { it.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50) }
            .user(userMessage)
            .call()
            .chatResponse()!!
        return chatResponse
    }
}