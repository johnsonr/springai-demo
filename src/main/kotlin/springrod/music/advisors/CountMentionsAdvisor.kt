package springrod.music.advisors

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain
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
) : CallAroundAdvisor {

    override fun aroundCall(
        advisedRequest: AdvisedRequest,
        chain: CallAroundAdvisorChain
    ): AdvisedResponse {
        val mentions = neo4jTemplate.findAll<Mentions>(Mentions::class.java)
        val mentioned = mentions.filter { advisedRequest.userText.contains(it.name, ignoreCase = true) }
        for (mention in mentioned) {
            noteMention(mention)
        }
        return chain.nextAroundCall(advisedRequest)
    }

    private fun noteMention(mentions: Mentions) {
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