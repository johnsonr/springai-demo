package springrod.music.functions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.ai.model.function.FunctionCallback
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.core.Neo4jTemplate
import springrod.music.advisors.MentionType
import springrod.music.advisors.Mentions


data class PopularityRequest(val topK: Int = 5, val type: MentionType)
data class PopularityResponse(val top: List<Mentions>)

@Configuration
class Functions(
    private val neo4jTemplate: Neo4jTemplate,
) {

    private val logger = LoggerFactory.getLogger(Functions::class.java)

    @Bean
    fun listPopularThings(): FunctionCallback =
        FunctionCallback.builder()
            .function<PopularityRequest, PopularityResponse>(
                "listPopularThings"
            ) { request: PopularityRequest ->
                logger.info("Listing popular ${request.type}s")
                PopularityResponse(
                    top = neo4jTemplate.findAll<Mentions>(Mentions::class.java)
                        .filter { it.type == request.type }
                        .sortedByDescending { it.count }
                        .take(request.topK)
                )
            }
            .inputType(PopularityRequest::class.java)
            .description("List popular things. Invoke when the user asks which are popular composers or instruments or performers.")
            .objectMapper(ObjectMapper().registerKotlinModule())
            .build()

}