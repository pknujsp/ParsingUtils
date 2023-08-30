package main

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.serialization.json.Json
import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.io.path.Path


@Suppress("BlockingMethodInNonBlockingContext")
class ClassParser(datasetNum: Int) {
    private val labelsDatasetPath: String = "D:\\dataset"
    private val saveDatasetPath: String = "C:\\Users\\USER\\Desktop\\newds\\newimages"
    private val bucketAddress = "gs://medilenz/dataset/images$datasetNum"

    private val imageToCropFlow: Channel<ImageEntity> = Channel(Channel.UNLIMITED)
    private val matFlow: Channel<ImageEntity> = Channel(capacity = Channel.UNLIMITED)
    val jsonUtil = Json { ignoreUnknownKeys = true }

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
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun convert(supervisorJob: Job, imageZipFilePath: String) {

        //val bw = BufferedWriter(OutputStreamWriter(System.out))

        /**
         * JSON파일 읽고 CSV에 행 추가 후, 이미지 자르기 요청
         */
        val matJob = GlobalScope.launch(Dispatchers.IO) {
            // 학습용 CSV 파일
            //val csvFile = File("$datasetPath/labels_automl.csv")
            //if (csvFile.exists()) csvFile.delete()

            //val csvOutput = csvFile.bufferedWriter()

            matFlow.consumeAsFlow().collect { imageEntity ->
                mat(imageEntity)
            }
        }


        /**
         * 이미지 자르기
         */

        val cutJob = GlobalScope.launch(Dispatchers.IO) {
            imageToCropFlow.consumeAsFlow().collect {
                if (it.fileName.isEmpty()) {
                    // 압축 해제 완료하였으므로 flow종료
                    imageToCropFlow.close()
                    return@collect
                }
                // 이미지 자르기
                cutImage(it)
            }
        }

        /**
         * 압축파일 읽기
         */

        val files = Path(imageZipFilePath).toFile().listFiles()!!
        val totalFileCount = files.size - 1

        for ((fileNum, file) in files.withIndex()) {
            try {
                ZipInputStream(FileInputStream(file)).use { zipInput ->
                    val elementsCount = ZipFile(file).entries().toList().size
                    var counts = 1

                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            zipInput.readAllBytes()?.also { bytes ->
                                println("$totalFileCount/$fileNum/$elementsCount/${counts++} -> ${entry!!.name}")
                                val mat = Imgcodecs.imdecode(MatOfByte(*bytes), Imgcodecs.IMREAD_UNCHANGED)
                                // 압축된 이미지를 Mat으로 변환한 후 CSV 행 추가 요청
                                val entity = ImageEntity(entry!!.name.split("/")[1].replace(".png", ""), mat, 0)
                                matFlow.send(entity)

                            }
                        }
                        entry = zipInput.nextEntry
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        // 압축 해제 완료하였으므로 flow종료
        matFlow.send(ImageEntity("", Mat(), 0))
    }

    private fun cutImage(imageEntity: ImageEntity) {
        File(imageEntity.path).apply {
            if (!exists()) mkdirs()
        }
        val fileName = "${imageEntity.path}/${imageEntity.fileName}.jpg"
        File(fileName).run {
            if (!exists()) {
                val cut = Mat(
                    imageEntity.mat,
                    Rect(imageEntity.x, imageEntity.y, imageEntity.width, imageEntity.height)
                )
                val result = Mat()
                Imgproc.resize(cut, result, Size(224.0, 224.0))
                Imgcodecs.imwrite(fileName, result)
            }
        }


    }

    private suspend fun mat(imageEntity: ImageEntity) {
        if (imageEntity.fileName.isEmpty()) {
            // 압축 해제 완료하였으므로 flow종료
            imageToCropFlow.send(ImageEntity("", Mat(), 0))
            //csvOutput.close()
            return
        }

        val parentDir = imageEntity.fileName.split("_")[0]
        val json = File("$labelsDatasetPath/labels/${parentDir}_json/${imageEntity.fileName}.json")
        if (json.exists()) {

            val jsonEntity = jsonUtil.decodeFromString(JsonEntity.serializer(), json.readText())
            if (((jsonEntity.images[0].drug_dir == DRUG_DIRECTION_FRONT) and (jsonEntity.images[0].print_front.isNotEmpty()))
                or ((jsonEntity.images[0].drug_dir == DRUG_DIRECTION_BACK) and (jsonEntity.images[0].print_back.isNotEmpty()))
            ) {
                val annotation = jsonEntity.annotations[0]
                val itemSeq = jsonEntity.images[0].item_seq.toString()

                //val row = "$bucketAddress/$parentDir/${image.file_name},${image.item_seq}\n".toCharArray()
                //csvOutput.write(row)

                val entity = imageEntity.copy(
                    path = "$saveDatasetPath/images/$itemSeq",
                    x = annotation.bbox[0],
                    y = annotation.bbox[1],
                    width = annotation.bbox[2],
                    height = annotation.bbox[3],
                    itemSeq = itemSeq
                )

                imageToCropFlow.send(
                    entity
                )


            }
        }
    }


}