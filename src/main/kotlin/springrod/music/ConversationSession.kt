package springrod.music

import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.Message
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.SessionScope
import java.util.*

fun interface NameGenerator {
    fun generateName(): String
}

val MobyNameGenerator = NameGenerator {
    info.schnatterer.mobynamesgenerator.MobyNamesGenerator.getRandomName()
}

/**
 * Session scoped object to hold conversation id for
 * Spring AI ChatMemory and system prompt,
 * which may change over time.
 */
@Component
@SessionScope
class ConversationSession(
    val chatMemory: ChatMemory,
    nameGenerator: NameGenerator = MobyNameGenerator,
) {

    private val promptPath: String get() = "prompts/system_prompt.md"

    val conversationId: String = nameGenerator.generateName()

    fun messages(): List<Message> {
        return chatMemory.get(conversationId, 100)
    }

    fun promptResource(): Resource {
        return ClassPathResource(promptPath)
    }
    
}