package springrod.music.advisors

import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.annotation.Id
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.data.neo4j.core.schema.Node
import kotlin.jvm.optionals.getOrNull


enum class MentionType {
    Composer, Instrument, Performer
}

@Node
data class Mentions(
    @Id
    val name: String,
    val type: MentionType,
    val count: Int = 0,
) {

    fun increment() = copy(count = count + 1)
}

/**
 * Note mention of a particular string in the user text
 */
class CountMentionsAdvisor(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val neo4jTemplate: Neo4jTemplate,
) : CallAdvisor {

    override fun adviseCall(
        chatClientRequest: ChatClientRequest,
        chain: CallAdvisorChain
    ): ChatClientResponse {
        val userText = chatClientRequest.prompt().getUserMessage()?.text ?: ""
        val mentions = neo4jTemplate.findAll<Mentions>(Mentions::class.java)
        val mentioned = mentions.filter { userText.contains(it.name, ignoreCase = true) }
        for (mention in mentioned) {
            noteMention(mention)
        }
        return chain.nextCall(chatClientRequest)
    }

    private fun noteMention(mentions: Mentions) {
        // Tie into Spring container functionality
        applicationEventPublisher.publishEvent(
            mentions
        )
        val mention = neo4jTemplate.findById(mentions.name, Mentions::class.java)
        mention.getOrNull()?.let {
            neo4jTemplate.save(it.increment())
        }
    }

    override fun getName(): String = "NoteMentions"

    override fun getOrder(): Int = 0

}
