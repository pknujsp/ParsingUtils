package main.imagenet.aihub

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import main.OpenCV.crop
import main.OpenCV.paddingAndResize
import main.OpenCV.toMat
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

private val ZIP_PATH = listOf(
    "E://166.약품식별 인공지능 개발을 위한 경구약제 이미지 데이터/01.데이터/1.Training/원천데이터/단일경구약제 5000종/zips",
    "D://166.약품식별 인공지능 개발을 위한 경구약제 이미지 데이터/01.데이터/1.Training/원천데이터/단일경구약제 5000종"
)
private const val LABELS_BASE_PATH = "C://Users/USER/Desktop/training/단일경구약제 5000종"
private const val TARGET_IMAGE_SIZE = 224
private const val IMAGE_OUTPUT_BASE_PATH = "C://Users/USER/Desktop/training/dataset/images"

private val JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

private val mutex = Mutex()

// dlmapping, itemSeq
private val productCodeMap = mutableMapOf<String, String>()
private val finalLabelsMap = mutableMapOf<String, Set<String>>()
private val printFlow = Channel<String>(capacity = 3000, onBufferOverflow = BufferOverflow.SUSPEND)

private val labelChannel: Channel<LabelFileInfo> = Channel(capacity = 3000, onBufferOverflow = BufferOverflow.SUSPEND)
private val imageChannel: Channel<ImageInfo> = Channel(capacity = 3000, onBufferOverflow = BufferOverflow.SUSPEND)

@OptIn(ExperimentalSerializationApi::class)
private fun loadProductCodes() {
    /**
    File(LABELS_BASE_PATH).listFiles()!!.run {
    forEach { productDir ->
    println("품목 코드 정보 로딩 : ${productDir.name}")
    productDir.listFiles()!!.firstNotNullOf {
    JSON.decodeFromString(AiHubLabelEntity.serializer(), it.readText()).item().first.run {
    productCodeMap[drugN] = itemSeq.toString()
    }
    }
    }
    }

    // 매핑 코드 저장
    val itemSeqMap = productCodeMap.map { it.value to it.key }.toMap()
    val jsonFile = ProductCodeEntity(productCodeMap, itemSeqMap)
    File("C://Users/USER/Desktop/training/dlmap.json").outputStream().use {
    JSON.encodeToStream(jsonFile, it)
    }
     */
    JSON.decodeFromString(ProductCodeEntity.serializer(), File("C://Users/USER/Desktop/training/dlmap.json").readText())
        .run {
            productCodeMap.putAll(dlMap)
        }

    JSON.decodeFromString(
        FinalLabelEntity.serializer(), File("C://Users/USER/Desktop/training/dataset/final_label.json").readText()
    ).run {
        finalLabelsMap.putAll(filesMap)
    }
}

private suspend fun processAndSave(imageInfo: ImageInfo) {
    if (existsImageFile(
            imageInfo.itemSeq, imageInfo.outputImageFileName
        )
    ) {
        printFlow.send("중복 : ${imageInfo.outputImageFileName}")
    } else {
        val bboxMat = imageInfo.mat.crop(imageInfo.bbox)
        val resizedMat = bboxMat.paddingAndResize(TARGET_IMAGE_SIZE)

        Imgcodecs.imwrite("${IMAGE_OUTPUT_BASE_PATH}/${imageInfo.itemSeq}/${imageInfo.outputImageFileName}", resizedMat)
        printFlow.send("사진 처리 후 저장완료 : ${imageInfo.outputImageFileName}")
    }
}


private fun existsImageFile(itemSeq: String, fileName: String): Boolean {
    return Path("${IMAGE_OUTPUT_BASE_PATH}/${itemSeq}/${fileName}").exists()
}

private fun String.toMappingCode() = split("/").first()

@OptIn(DelicateCoroutinesApi::class)
suspend fun main(): Unit = supervisorScope {
    val printJob = launch(Dispatchers.Default) {
        printFlow.consumeAsFlow().collect {
            println(it)
        }
    }
    printJob.invokeOnCompletion {
        println("출력 작업 완료")
    }

    loadProductCodes()
    val completedProducts = File("C://Users/USER/Desktop/training/dataset/completed.txt").run {
        readLines().toSet()
    }

    val labelingJob = launch(Dispatchers.IO) {
        labelChannel.consumeAsFlow().collect {
            val file = File("${LABELS_BASE_PATH}/${it.itemSeq}/${it.labelFileName}")

            if (file.exists()) {
                val label = JSON.decodeFromString(
                    AiHubLabelEntity.serializer(), file.readText()
                ).item()

                imageChannel.send(ImageInfo(mat = it.imageByte.toMat(), bbox = label.second.run {
                    Rect(x, y, width, height)
                }, itemSeq = label.first.itemSeq.toString(), srcImageFileName = file.nameWithoutExtension))
            }
        }

    }
    labelingJob.invokeOnCompletion {
        println("라벨링 파일 분석 작업 완료")
    }

    val imageProcessingJob = launch(Dispatchers.IO) {
        imageChannel.consumeAsFlow().collect {
            processAndSave(it)
        }
    }
    imageProcessingJob.invokeOnCompletion {
        println("이미지 처리 작업 완료")
    }

    val zips = ZIP_PATH.flatMap { s -> Path(s).toFile().listFiles()!!.filter { it.extension == "zip" } }
    val recordFile = File("C:\\Users\\USER\\Desktop\\training\\dataset\\record.txt")
    val zipsCount = zips.size

    zips.mapIndexed { zipNum, zip ->
        try {
            ZipInputStream(FileInputStream(zip)).use { zipInput ->
                var entry = zipInput.nextEntry
                var skip = false

                while (entry != null) {
                    if (!entry.isDirectory) {
                        if (!skip) {
                            zipInput.readAllBytes()?.let { bytes ->
                                labelChannel.send(
                                    LabelFileInfo(
                                        itemSeq = productCodeMap[entry!!.name.toMappingCode()]!!,
                                        pLabelFileName = entry!!.name,
                                        imageByte = bytes
                                    )
                                )
                            }
                        }
                    } else {
                        val itemSeq = productCodeMap.getOrDefault(entry.name.toMappingCode(), "")
                        if (itemSeq.isEmpty()) {
                            skip = true
                        } else {
                            Path("${IMAGE_OUTPUT_BASE_PATH}/${itemSeq}").run {
                                if (!exists()) createDirectories()
                            }
                            skip = completedProducts.contains(itemSeq)
                            if (skip) {
                                printFlow.send("생략 : ${entry.name}")
                            }
                        }
                    }
                    entry = zipInput.nextEntry
                }
            }

            mutex.withLock {
                recordFile.run {
                    if (!exists()) createNewFile()
                    appendText("처리 완료 : ${zip.name}\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mutex.withLock {
                recordFile.run {
                    if (!exists()) createNewFile()
                    appendText("오류 : ${zip.name} -> ${e.message}\n")
                }
            }
        }
    }

    println("작업 완료 대기중...")
    printJob.join()
    imageProcessingJob.join()
}

private class LabelFileInfo(
    val itemSeq: String, pLabelFileName: String, val imageByte: ByteArray
) {
    val labelFileName: String = pLabelFileName.split("/")[1].replace("png", "json")
}

private class ImageInfo(
    val mat: Mat, val bbox: Rect, val itemSeq: String, srcImageFileName: String
) {
    val outputImageFileName: String = "${srcImageFileName}.jpg"
}