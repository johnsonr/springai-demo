package springrod.music

import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.OllamaEmbeddingModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.event.EventListener
import springrod.music.advisors.Mentions

/**
 * Explicit configuration for the chat application.
 * Starters can make it hard to vary things.
 */
@Configuration
class ChatConfiguration {

    @Bean
    fun chatMemory(): ChatMemory = InMemoryChatMemory()

    @Primary
    @Bean
    fun premiumChatModel(@Value("\${OPENAI_API_KEY}") apiKey: String): ChatModel {
        return OpenAiChatModel(OpenAiApi(apiKey))
    }

    @Bean
    fun localChatModel(): OllamaChatModel {
        return OllamaChatModel.builder()
            .withOllamaApi(OllamaApi())
            .withDefaultOptions(
                OllamaOptions.builder()
                    .withModel("gemma2:2b")
                    .build()
            )
            .build()
    }

    @Bean
    @Primary
    fun embeddingModel(): EmbeddingModel {
        return OllamaEmbeddingModel.builder()
            .withOllamaApi(
                OllamaApi()
            )
            .withDefaultOptions(
                OllamaOptions.builder()
                    .withModel("gemma2:2b")
                    .build()
            ).build()
    }

    @EventListener
    fun applicationEventListener(notedMentionEvent: Mentions) {
        println("!!!!!!!!!! Noted mention of $notedMentionEvent")
    }

}

