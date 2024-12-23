package springrod.music

import org.junit.jupiter.api.Test
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemory
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.SpringBootTest

@ConditionalOnProperty("\${OPENAI_API_KEY}")
@SpringBootTest
class ChatServiceTest {

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var embeddingModel: EmbeddingModel

    @Autowired
    private lateinit var vectorStore: VectorStore

    private val chatMemory: ChatMemory = InMemoryChatMemory()

    @Test
    fun testThing() {
        chatService.respondToUserMessage(
            ConversationSession(chatMemory),
            "Hello"
        )
    }
}