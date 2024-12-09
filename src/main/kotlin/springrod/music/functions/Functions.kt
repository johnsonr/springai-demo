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
import springrod.music.advisors.Performance


/**
 * Function payload
 */
data class PopularityRequest(val topK: Int = 5, val type: MentionType)

/**
 * Function response
 */
data class PopularityResponse(val top: List<Mentions>)

data class PerformanceRequest(val number: Int = 10)
data class PerformanceResponse(val upcoming: List<Performance>)

/**
 * Functions that will be exposed using Spring AI
 */
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
                    top = neo4jTemplate.findAll(Mentions::class.java)
                        .filter { it.type == request.type }
                        .sortedByDescending { it.count }
                        .take(request.topK)
                )
            }
            .inputType(PopularityRequest::class.java)
            .description("List popular things. Invoke when the user asks which are popular composers or instruments or performers.")
            .objectMapper(ObjectMapper().registerKotlinModule())
            .build()

    @Bean
    fun upcomingPerformances(): FunctionCallback =
        FunctionCallback.builder()
            .function<PerformanceRequest, PerformanceResponse>(
                "listPerformances"
            ) {
                val pr = PerformanceResponse(
                    upcoming = neo4jTemplate.findAll(Performance::class.java)
                        .sortedBy { it.date }
                        .take(it.number)
                )
                logger.info("Upcoming performances request $it returned $pr")
                pr
            }
            .inputType(PerformanceRequest::class.java)
            .description("Find upcoming performances. Invoke if a user asks for upcoming performances.")
            .objectMapper(ObjectMapper().registerKotlinModule())
            .build()

}