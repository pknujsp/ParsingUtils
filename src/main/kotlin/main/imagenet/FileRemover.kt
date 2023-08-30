package main.imagenet

import java.io.File

private const val PATH = "C:\\Users\\USER\\Desktop\\augment"
private const val GRAY = "gray_"

fun main() {
    File(PATH).listFiles()!!.forEachIndexed { i, mainTypeDir ->
        mainTypeDir.listFiles()!!.forEachIndexed { j, imgDir ->
            imgDir.listFiles()!!.forEach { img ->
                if (img.name.startsWith(GRAY)) {
                    println(img.name)
                    //img.delete()
                }
            }
            println("$i - $j")
        }
    }
}