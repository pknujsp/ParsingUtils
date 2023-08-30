package main.imagenet.aihub

import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import java.io.File


private val JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}


suspend fun main() {
    supervisorScope {
        val imagePath = "C:\\Users\\USER\\Desktop\\training\\dataset\\images"
        val labelPath = "C:\\Users\\USER\\Desktop\\training\\단일경구약제 5000종"

        File(labelPath).listFiles()!!.forEach { file -> println(file.nameWithoutExtension to file.list()!!.size) }
    }
}