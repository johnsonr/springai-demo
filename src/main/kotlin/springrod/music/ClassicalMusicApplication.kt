package springrod.music

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ClassicalMusicApplication

fun main(args: Array<String>) {
    runApplication<ClassicalMusicApplication>(*args)
}
