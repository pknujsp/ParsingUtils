package main.imagenet.aihub


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

const val DIR_FRONT = "앞면"
const val DIR_BACK = "뒷면"

private const val DIVIDER = "/"


@Serializable
data class AiHubLabelEntity(
    @SerialName("images") val images: List<Image> = listOf(),
    @SerialName("annotations") val annotations: List<Annotation> = listOf(),
) {

    var fileName: String = ""

    @Serializable
    data class Image(
        @SerialName("file_name") val fileName: String = "",
        @SerialName("width") val width: Int = 0,
        @SerialName("height") val height: Int = 0,
        @SerialName("imgfile") val imgfile: String = "",
        @SerialName("back_color") val backColor: String = "",
        @SerialName("drug_dir") val drugDir: String = "",
        @SerialName("light_color") val lightColor: String = "",
        @SerialName("camera_la") val cameraLa: Int = 0,
        @SerialName("camera_lo") val cameraLo: Int = 0,
        @SerialName("size") val size: Int = 0,
        @SerialName("drug_N") val drugN: String = "",
        @SerialName("dl_custom_shape") val dlCustomShape: String = "",
        @SerialName("item_seq") val itemSeq: Long = 0L,
        @SerialName("drug_shape") val drugShape: String = "",
        @SerialName("print_front") val printFront: String = "",
        @SerialName("print_back") val printBack: String = "",
        @SerialName("color_class1") val colorClass1: String = "",
        @SerialName("color_class2") val colorClass2: String = "",
        @SerialName("line_front") val lineFront: String = "",
        @SerialName("line_back") val lineBack: String = "",
        @SerialName("form_code_name") val formCodeName: String = "",
        @SerialName("mark_code_front_anal") val markCodeFrontAnal: String = "",
        @SerialName("mark_code_back_anal") val markCodeBackAnal: String = "",
        @SerialName("mark_code_front_img") val markCodeFrontImg: String = "",
        @SerialName("mark_code_back_img") val markCodeBackImg: String = "",
        @SerialName("mark_code_front") val markCodeFront: String = "",
        @SerialName("mark_code_back") val markCodeBack: String = "",
    ) {
        fun isPrintImage(): Boolean =
            (printFront.isNotEmpty() and (drugDir == DIR_FRONT)) or (printBack.isNotEmpty() and (drugDir == DIR_BACK))
    }

    @Serializable
    data class Annotation(
        @SerialName("bbox") val bbox: List<Int> = listOf(),
    ) {
        fun objectInfo() = ObjectInfo(
            x = bbox[0],
            y = bbox[1],
            width = bbox[2],
            height = bbox[3],
        )

        data class ObjectInfo(
            val x: Int, val y: Int, val width: Int, val height: Int
        )
    }

    fun item() = images.first() to annotations.first().objectInfo()

    fun file(path: String) = images.first().let {
        File("${path}${DIVIDER}${it.drugN}_json${DIVIDER}${fileName}")
    }
}