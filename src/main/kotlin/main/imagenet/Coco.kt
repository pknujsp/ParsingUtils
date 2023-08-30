package main.imagenet

import com.google.gson.GsonBuilder
import kotlinx.serialization.json.Json
import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FilenameFilter
import java.io.PrintWriter

const val path = "D:\\dataset\\augment"
const val imgSize = 640.0

fun allFiles(path: String): List<String> {
    val fileList =
        File(path).listFiles { file -> file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".jpeg")) }
    return fileList?.map { file -> file.name } ?: emptyList()
}

fun allRealFiles(path: String): List<File> {
    return File(path).listFiles()!!.toList()
}

fun allDirs(path: String): List<String> {
    return File(path).listFiles { file -> file.isDirectory }?.map { file -> file.name } ?: emptyList()
}

fun mkdirs(path: String) {
    File(path).mkdirs()
}


fun resize(mat: Mat): Mat {
    val result = Mat()
    Imgproc.resize(mat, result, Size(imgSize, imgSize))
    return result
}

fun createResize() {
    OpenCV.loadLocally()

    val types = arrayOf("val")

    for ((ri, type) in types.withIndex()) {
        val dirs = allDirs("$path/$type")
        val savePath = "$path/resized/$type"
        mkdirs(savePath)

        for ((di, dir) in dirs.withIndex()) {
            val files = allRealFiles("$path/$type/$dir")

            for (file in files) {
                val src = Imgcodecs.imread(file.absolutePath)
                val resized = resize(src)

                Imgcodecs.imwrite("$path/resized/$type/${file.name}", resized)
            }
            println("$ri $di")

        }


    }
}

fun toYoloFormat() {
    val types = arrayOf("val", "train", "test")
    val label = "0 0.5 0.5 0.99 0.99"
    val json = Json {
        ignoreUnknownKeys = true
    }
    val jpg = "jpg"
    val txt = "txt"

    for ((ri, type) in types.withIndex()) {
        val cocoFile = File("$path/$type").listFiles()!!.first { it.extension == "json" }
        val cocoEntity: CocoEntity = json.decodeFromString(CocoEntity.serializer(), cocoFile.readText())
        cocoEntity.images.forEachIndexed { index, image ->
            val txtFile = File("$path/$type/yolo/${image.fileName.replace(jpg, txt)}")
            txtFile.writeText(label)
            println("$ri - $index")
        }


    }
}

fun toCoco() {
    val types = arrayOf("val")

    val imgSize = 640
    val license = 1
    val categoryId = 1
    val area = imgSize * imgSize

    for ((ri, type) in types.withIndex()) {
        val dirs = allDirs("$path/$type")
        val jsonFileName = "instances_$type.json"
        var annotImgId = 0

        val imgs = mutableListOf<Map<String, Any>>()
        val annotations = mutableListOf<Map<String, Any>>()

        for ((di, dir) in dirs.withIndex()) {
            val files = allFiles("$path/$type/$dir")

            for (file in files) {
                println("$ri $di")

                val images = mapOf(
                    "id" to annotImgId,
                    "license" to license,
                    "file_name" to file,
                    "height" to imgSize,
                    "width" to imgSize,
                    "date_captured" to "2023-08-21T05:37:54+00:00"
                )
                val annotation = mapOf(
                    "id" to annotImgId,
                    "image_id" to annotImgId,
                    "category_id" to categoryId,
                    "bbox" to listOf(0, 0, imgSize, imgSize),
                    "area" to area,
                    "segmentation" to emptyList<Any>(),
                    "iscrowd" to 0
                )

                annotImgId++

                imgs.add(images)
                annotations.add(annotation)
            }

        }

        val json = GsonBuilder().setPrettyPrinting().create()
        val savePath = "$path/annots/$type/$jsonFileName"

        val jsonFile = File(savePath)
        val jsonData = mapOf(
            "info" to mapOf(
                "year" to "2023",
                "version" to "1",
                "description" to "",
                "contributor" to "jsp",
                "url" to "",
                "date_created" to "2023-08-21T05:37:54+00:00"
            ), "licenses" to listOf(
                mapOf(
                    "id" to 1, "url" to "https://creativecommons.org/publicdomain/zero/1.0/", "name" to "Public Domain"
                )
            ), "categories" to listOf(
                mapOf(
                    "id" to 0, "name" to "Drug", "supercategory" to "none"
                ), mapOf(
                    "id" to 1, "name" to "Drug", "supercategory" to "Drug"
                )
            ), "images" to imgs, "annotations" to annotations
        )

        jsonFile.writeText(json.toJson(jsonData))
    }
}

fun toCocoFromYolo() {
    val types = arrayOf("val", "train")
    val jpg = "jpg"

    val imgSize = 640
    val license = 1
    val categoryId = 1
    val area = imgSize * imgSize

    for ((ri, type) in types.withIndex()) {
        val txtFiles = allRealFiles("$path/$type")
        var annotImgId = 0

        val imgs = mutableListOf<Map<String, Any>>()
        val annotations = mutableListOf<Map<String, Any>>()

        for ((fi, file) in txtFiles.withIndex()) {
            println("$ri $fi")

            val images = mapOf(
                "id" to annotImgId,
                "license" to license,
                "file_name" to "${file.nameWithoutExtension}.$jpg",
                "height" to imgSize,
                "width" to imgSize,
                "date_captured" to "2023-08-21T05:37:54+00:00"
            )
            val annotation = mapOf(
                "id" to annotImgId,
                "image_id" to annotImgId,
                "category_id" to categoryId,
                "bbox" to listOf(0, 0, imgSize, imgSize),
                "area" to area,
                "segmentation" to emptyList<Any>(),
                "iscrowd" to 0
            )

            annotImgId++

            imgs.add(images)
            annotations.add(annotation)
        }


        val json = GsonBuilder().setPrettyPrinting().create()
        val jsonFileName = "instances_$type.json"
        val savePath = "$path/annots/$type/$jsonFileName"

        val jsonFile = File(savePath)
        val jsonData = mapOf(
            "info" to mapOf(
                "year" to "2023",
                "version" to "1",
                "description" to "",
                "contributor" to "jsp",
                "url" to "",
                "date_created" to "2023-08-21T05:37:54+00:00"
            ), "licenses" to listOf(
                mapOf(
                    "id" to 1, "url" to "https://creativecommons.org/publicdomain/zero/1.0/", "name" to "Public Domain"
                )
            ), "categories" to listOf(
                mapOf(
                    "id" to 0, "name" to "Drug", "supercategory" to "none"
                ), mapOf(
                    "id" to 1, "name" to "Drug", "supercategory" to "Drug"
                )
            ), "images" to imgs, "annotations" to annotations
        )

        jsonFile.writeText(json.toJson(jsonData))
    }
}


fun moveYoloToAutoMl() {
    val types = arrayOf("val", "train", "test")
    val savePath = "$path/automl"
    val printOutWriter = PrintWriter(System.out)

    for ((ri, type) in types.withIndex()) {
        val files = mutableListOf<File>()

        File("$path/$type").listFiles()!!.forEach { dir ->
            files.addAll(dir.listFiles()!!)
        }

        files.forEachIndexed { i, file ->
            file.copyTo(File("$savePath/$type/${file.name}"), overwrite = true, bufferSize = DEFAULT_BUFFER_SIZE)
            printOutWriter.write("$ri - $i\n")
        }
    }

    printOutWriter.close()
}

fun toAutoMlCsvFromYolo() {
    val types = arrayOf("val", "train", "test")
    val trainTypes = arrayOf("VALIDATION", "TRAIN", "TEST")
    val annotations = mutableListOf<String>()
    val printOutWriter = PrintWriter(System.out)
    val savePath = "C://Users/USER/Desktop/training/automl"

    for ((ri, type) in types.withIndex()) {
        val trainType = trainTypes[ri]
        val dirs = File("$path/$type").listFiles()!!

        for (itemSeq in dirs) {
            printOutWriter.write("$ri - ${itemSeq}\n")
            itemSeq.listFiles()!!.forEach {
                annotations.add("$trainType,$savePath/$type/${it.name},${itemSeq.nameWithoutExtension},0,0,1,0,1,1,0,1\n")
            }
        }
    }

    val csvFile = File("$path/automl/annotation.csv")
    val size = annotations.size
    csvFile.bufferedWriter(charset = Charsets.UTF_8).use {
        annotations.forEachIndexed { index, s ->
            printOutWriter.write("$size - $index\n")
            it.write(s)
        }
    }
    printOutWriter.close()
}

fun showLabels() {
    val file = File("D:\\dataset\\augment\\train")
    val classes = mutableListOf<String>()
    file.listFiles()!!.forEachIndexed { i, f ->
        classes.add("- ${f.name}\n")
    }
    File("D:\\dataset\\augment\\class.txt").bufferedWriter().use { writer ->
        classes.forEach {
            writer.write(it)
        }
    }
}

fun csvToCocoTxt() {
    val classes = File("C:\\Users\\USER\\Desktop\\training\\automl\\class.txt").run {
        val removeTxt = "- "
        val emptyTxt = ""
        readLines().mapIndexed { i, l ->
            l.replace(removeTxt, emptyTxt) to i.toString()
        }.toMap()
    }

    val types = mapOf("TEST" to "testlabels", "VALIDATION" to "vallabels", "TRAIN" to "trainlabels")
    val savePath = "C://Users/USER/Desktop/training/automl"
    val split = ','
    val split2 = '/'
    val jpg = ".jpg"
    val txt = ".txt"


    File("C:\\Users\\USER\\Desktop\\training\\automl\\annotation.csv").let { csv ->
        val fileList = mutableListOf<Pair<String, String>>()
        csv.readLines().forEachIndexed { i, row ->
            println(i)
            row.split(split).let { items ->
                fileList.add(
                    classes[items[2]]!!.toString() to "$savePath/${types[items[0]]}/${
                        items[1].split(split2).last().replace(jpg, txt)
                    }"
                )
            }
        }
        PrintWriter(System.out).use { printWriter ->
            fileList.forEachIndexed { index, pair ->
                printWriter.write("$index\n")
                File(pair.second).writeText("${pair.first} 0.5 0.5 1 1")
            }
        }
    }
}

fun split() {
    val path = "C:\\Users\\USER\\Desktop\\training\\automl\\YOLO-NAS\\training"
    val movePath = "C:\\Users\\USER\\Desktop\\training\\automl\\YOLO-NAS\\training\\excludes\\train"
    val brightness = "brightness"
    val image = "image"
    val labels = "labels"
    val enhance = "enhance"
    val blur = "blur"
    val split = " "
    val divSize = 4

    val jpg = "jpg"
    val txt = "txt"

    val types = listOf("trainlabels")

    PrintWriter(System.out).use { printWriter ->
        types.forEachIndexed { j, type ->
            val files = mutableMapOf<String, Data>()
            (0..<4019).forEach {
                files[it.toString()] = Data()
            }

            File("$path/$type").listFiles()!!.forEachIndexed { i, file ->
                val augmentType = file.name.run {
                    if (startsWith(blur)) Data.AugmentType.Blur
                    else if (startsWith(enhance)) Data.AugmentType.Enhance
                    else Data.AugmentType.Brightness
                }
                val itemSeq = file.readText().split(split).first()
                files[itemSeq]!!.files[augmentType]!!.add(file)
                printWriter.write("$j - $i\n")
            }


            files.forEach { (id, v) ->
                printWriter.write("$id \n")
                v.files.forEach {
                    it.value.run {
                        subList(0, size / divSize).forEach { labelFile ->
                            val fileName = labelFile.nameWithoutExtension
                            val imageFile =
                                File("C:\\Users\\USER\\Desktop\\training\\automl\\YOLO-NAS\\training\\train\\${fileName}.jpg")
                            labelFile.renameTo(File("${movePath}\\labels\\${fileName}.txt"))
                            imageFile.renameTo(File("${movePath}\\image\\${fileName}.jpg"))
                        }
                    }
                }
            }
        }

    }
}

private data class Data(
    val files: Map<AugmentType, MutableList<File>> = mapOf(
        AugmentType.Brightness to mutableListOf(),
        AugmentType.Enhance to mutableListOf(),
        AugmentType.Blur to mutableListOf()
    )
) {
    enum class AugmentType {
        Brightness, Blur, Enhance
    }
}

fun removeGray() {
    val path = "D:\\dataset\\augment\\"
    val gray = "gray_"
    val fileNamefilter = FilenameFilter { _, name ->
        name.startsWith(gray)
    }
    val printWriter = PrintWriter(System.out)
    listOf("val", "test").forEach { type ->
        File("${path}${type}").listFiles()!!.forEach { folder ->
            folder.listFiles(fileNamefilter)!!.forEach {
                it.delete()
                printWriter.write("${it.name}\n")
            }
        }
    }
    printWriter.close()
}

fun main() {
    removeGray()
}
