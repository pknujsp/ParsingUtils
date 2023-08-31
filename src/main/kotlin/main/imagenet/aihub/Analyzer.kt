package main.imagenet.aihub

import java.io.File
import kotlin.io.path.Path


fun main() {
    val labelPath = "C://Users/USER/Desktop/training/단일경구약제 5000종"
    val imagePath = "C://Users/USER/Desktop/training/dataset/images"

    val labelFiles = Path(labelPath).toFile().listFiles()!!.associate { it.nameWithoutExtension to it.list()!!.size }
    val imageFiles = Path(imagePath).toFile().listFiles()!!.associate { it.nameWithoutExtension to it.list()!!.size }

    val completedProducts = mutableListOf<String>()
    imageFiles.forEach { (key, value) ->
        if (value == labelFiles[key]) {
            completedProducts.add(key)
        }
    }

    println(completedProducts)
    File("C://Users/USER/Desktop/training/dataset/completed.txt").run {
        writeText(completedProducts.joinToString("\n"))
    }
}