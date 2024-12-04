package springrod.music

import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.stereotype.Component
import springrod.music.advisors.MentionType
import springrod.music.advisors.Mentions
import java.nio.charset.Charset

val TRACK_MENTIONS =
    listOf(
        Mentions(name = "Brahms", type = MentionType.Composer),
        Mentions(name = "Beethoven", type = MentionType.Composer),
        Mentions(name = "Mozart", type = MentionType.Composer),
        Mentions(name = "Chopin", type = MentionType.Composer),
        Mentions(name = "Mahler", type = MentionType.Composer),
        Mentions(name = "Piano", type = MentionType.Instrument),
        Mentions(name = "Violin", type = MentionType.Instrument),
        Mentions(name = "Cello", type = MentionType.Instrument),
        Mentions(name = "Trumpet", type = MentionType.Instrument),
        Mentions(name = "Ashkenazy", type = MentionType.Performer),
        Mentions(name = "Callas", type = MentionType.Performer),
    )

/**
 * Quick and dirty way to load on startup
 */
@Component
class Populator(
    private val vectorStore: VectorStore,
    private val neo4jTemplate: Neo4jTemplate,
) {

    private val logger = LoggerFactory.getLogger(Populator::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        logger.info("Populating database if necessary")
        populateVectorDatabase()
        populateMentions()
    }

    private fun populateMentions() {
        val mentions = neo4jTemplate.findAll(Mentions::class.java)
        if (mentions.isEmpty()) {
            logger.info("No mentions found in database, adding some")
            neo4jTemplate.saveAll(TRACK_MENTIONS)
        }
    }

    private fun populateVectorDatabase() {
        val results = vectorStore.similaritySearch("physical store")
        if (results.isEmpty()) {
            logger.info("Populating vector store")
            val resourcePatternResolver = PathMatchingResourcePatternResolver()

            try {
                // Find all files in the documents directory
                val resources = resourcePatternResolver.getResources("classpath:documents/**/*.*")

                // Convert each resource to a Document
                val documents = resources.mapNotNull { resource ->
                    try {
                        val content = resource.getContentAsString(Charset.defaultCharset())
                        Document(
                            content,
                            mapOf("filename" to resource.filename)
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to read document ${resource.filename}", e)
                        null
                    }
                }

                if (documents.isNotEmpty()) {
                    vectorStore.add(documents)
                    logger.info("Successfully populated vector store with ${documents.size} documents")
                } else {
                    logger.warn("No documents found in documents directory")
                }

            } catch (e: Exception) {
                logger.error("Failed to populate vector store", e)
            }
        }
    }
}

