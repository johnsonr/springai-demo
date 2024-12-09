package springrod.music.db

import org.neo4j.cypherdsl.core.renderer.Dialect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Neo4jConfiguration {

    /**
     * Needed to suppress deprecation warnings from Neo SDN 6.
     */
    @Bean
    fun cypherDslConfiguration(): org.neo4j.cypherdsl.core.renderer.Configuration {
        return org.neo4j.cypherdsl.core.renderer.Configuration.newConfig()
            .withDialect(Dialect.NEO4J_5).build();
    }
}