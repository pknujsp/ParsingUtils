package main.imagenet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import main.DRUG_DIRECTION_BACK
import main.DRUG_DIRECTION_FRONT
import main.imagenet.aihub.AiHubLabelEntity
import java.io.File

private const val PATH = "E://166.약품식별 인공지능 개발을 위한 경구약제 이미지 데이터/01.데이터/1.Training/라벨링데이터/단일경구약제 5000종"
private const val DIVIDER = "/"

private val JSON = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

// 앞면 - 뒷면
// 배경색

val imgDirections = listOf(DRUG_DIRECTION_FRONT, DRUG_DIRECTION_BACK)

val splitFlow = Channel<List<AiHubLabelEntity.Image>>(capacity = 10000, onBufferOverflow = BufferOverflow.SUSPEND)

fun splitFiles(labels: List<AiHubLabelEntity.Image>): Map<String, Map<String, Int>> {
    val divMap = mutableMapOf<String, Map<String, Int>>()

    imgDirections.forEach { direction ->
        labels.filter { it.drugDir == direction }.let { filtered ->
            if (filtered.isNotEmpty()) {
                val mutableMap = mutableMapOf<String, Int>()
                filtered.map { it.backColor }.toSet().forEach { color ->
                    mutableMap[color] = filtered.filter { it.backColor == color }.size
                }
                divMap[direction] = mutableMap
            }
        }
    }
    return divMap
}

suspend fun main() {
    coroutineScope {
        launch {
            File(PATH).listFiles()!!.run {
                val productCounts = size

                forEach { productDir ->
                    val labels = productDir.listFiles()!!.map {
                        JSON.decodeFromString(AiHubLabelEntity.serializer(), it.readText()).images[0]
                    }
                    splitFlow.send(labels)
                }
            }
        }

        launch(Dispatchers.Default) {
            splitFlow.consumeAsFlow().collect {
                val map = splitFiles(it)
                println(it.first().fileName)
            }
        }
    }
}