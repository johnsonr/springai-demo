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
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.stereotype.Service
import springrod.music.advisors.SavePerformanceAdvisor
import springrod.music.advisors.CountMentionsAdvisor
import springrod.music.advisors.Topic
import springrod.music.advisors.TopicGuardAdvisor
import java.util.concurrent.Executor

@Service
class ChatService(
    private val chatModel: ChatModel,
    private val localChatModel: OllamaChatModel,
    private val vectorStore: VectorStore,
    private val neo4jTemplate: Neo4jTemplate,
    private val executor: Executor,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val contextFunctions: List<FunctionCallback>,
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
                TopicGuardAdvisor(
                    chatModel = localChatModel,
                    bannedTopics = setOf(Topic.POLITICS, Topic.RELIGION, Topic.SPORT),
                ),
                // Out of the box advisor, handles RAG
                QuestionAnswerAdvisor(
                    vectorStore,
                    SearchRequest.defaults()
                        .withSimilarityThreshold(.2)
                        .withTopK(6)
                ),
            )
            .defaultFunctions(
                *contextFunctions.toTypedArray()
            )
            .defaultSystem(conversationSession.promptResource())
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