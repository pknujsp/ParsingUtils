package main

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import nu.pattern.OpenCV
import java.io.File


class DetectionParser(private val datasetNum: Int) {
    private val datasetPath: String = "E://DrugDetection.v1i.yolov8"

    private val bucketAddress = "gs://medilenz/detection_dataset/images$datasetNum"

    companion object {
        init {
            OpenCV.loadLocally()
        }
    }

    /**
     * 1. json 파일을 읽어서
     * 2. automl 라벨링 csv파일을 생성하면서
     * 3. 학습 이미지 파일을 자른다.
     */
    suspend fun convert(supervisorJob: Job) {
        /**
         * txt파일 로드하고 csv 행 추가
         */
        withContext(Dispatchers.IO + Job(supervisorJob)) {
            val csvFile = File("$datasetPath/detection_labels_automl.csv")
            if (csvFile.exists()) csvFile.delete()
            val csvOutput = csvFile.bufferedWriter()
            val names = listOf("train/labels")

            var totalFileCounts = 0

            names.forEach { type ->
                File("$datasetPath/$type").listFiles().apply {
                    totalFileCounts += this?.size ?: 0
                }?.forEachIndexed { idx, txtFile ->

                    val txt = txtFile.readText()

                    if(txt.isEmpty() || txt.contains("\n")) return@forEachIndexed

                    val pos = txt.split(" ").map { it.toFloat() }

                    val xCenter = pos[1]
                    val yCenter = pos[2]
                    val width = pos[3]
                    val height = pos[4]

                    val xMin = (xCenter - width / 2f)
                    val yMin = (yCenter - height / 2f)
                    val xMax = (xCenter + width / 2f)
                    val yMax = (yCenter + height / 2f)

                    val row =
                        "$bucketAddress/${txtFile.nameWithoutExtension}.jpg,medicine,$xMin,$yMin,,,$xMax,$yMax,,\n".toCharArray()

                    println("$totalFileCounts -> $idx")
                    csvOutput.write(row)
                }
            }

            csvOutput.close()

        }
    }
}


// 0 0.48984375 0.525 0.1421875 0.4609375