package main.imagenet.aihub

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import java.io.File

private val JSON = kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val map = mutableMapOf<String, Set<String>>()

    File("C://Users/USER/Desktop/training/단일경구약제 5000종").listFiles()!!.forEach { productDir ->
        println(productDir.name)
        map[productDir.name] = productDir.list()!!.toSet()
    }

    FinalLabelEntity(map).run {
        File("C://Users/USER/Desktop/training/dataset/final_label.json").outputStream().use {
            JSON.encodeToStream(this, it)
        }
    }

    println("저장완료")
}