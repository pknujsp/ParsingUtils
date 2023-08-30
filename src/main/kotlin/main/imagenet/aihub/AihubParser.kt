package main.imagenet.aihub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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

private const val ZIP_PATH = "E://166.약품식별 인공지능 개발을 위한 경구약제 이미지 데이터/01.데이터/1.Training/원천데이터/단일경구약제 5000종/zips"
private const val LABELS_BASE_PATH = "C://Users/USER/Desktop/training/단일경구약제 5000종"
private const val TARGET_IMAGE_SIZE = 224
private const val IMAGE_OUTPUT_BASE_PATH = "C://Users/USER/Desktop/training/dataset/images"

private val excludedLabelFiles = mutableListOf<String>()

private val JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

// dlmapping, itemSeq
private val productCodeMap = mutableMapOf<String, String>()
private val printFlow = Channel<String>(capacity = 2000, onBufferOverflow = BufferOverflow.SUSPEND)

private val labelChannel: Channel<LabelFileInfo> = Channel(capacity = 2000, onBufferOverflow = BufferOverflow.SUSPEND)
private val imageChannel: Channel<ImageInfo> = Channel(capacity = 2000, onBufferOverflow = BufferOverflow.SUSPEND)

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

}

private suspend fun processAndSave(imageInfo: ImageInfo) {
    val bboxMat = imageInfo.mat.crop(imageInfo.bbox)
    val resizedMat = bboxMat.paddingAndResize(TARGET_IMAGE_SIZE)

    val path = "${IMAGE_OUTPUT_BASE_PATH}/${imageInfo.itemSeq}"
    File(path).apply {
        if (!exists()) mkdirs()
    }

    Imgcodecs.imwrite("${path}/${imageInfo.outputImageFileName}", resizedMat)
    printFlow.send("사진 처리 후 저장완료 : ${imageInfo.outputImageFileName}")
}

private fun String.toMappingCode() = split("/").first()

suspend fun main(): Unit = supervisorScope {
    launch(Dispatchers.IO) {
        printFlow.consumeAsFlow().collect {
            println(it)
        }
    }

    loadProductCodes()

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
            } else {
                excludedLabelFiles.add(file.name)
            }
        }
    }

    launch(Dispatchers.IO) {
        imageChannel.consumeAsFlow().collect {
            processAndSave(it)
        }
    }

    val zips = Path(ZIP_PATH).toFile().listFiles()!!.filter { it.extension == "zip" }

    for (zip in zips) {
        try {
            ZipInputStream(FileInputStream(zip)).use { zipInput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
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
                    entry = zipInput.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    labelingJob.invokeOnCompletion {
        File("C://Users/USER/Desktop/training/학습데이터/제외된파일.txt").writeText(excludedLabelFiles.joinToString("\n"))
    }
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