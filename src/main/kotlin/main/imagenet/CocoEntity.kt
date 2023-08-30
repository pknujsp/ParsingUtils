package main.imagenet


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CocoEntity(
    @SerialName("annotations")
    val annotations: List<Annotation>,
    @SerialName("categories")
    val categories: List<Category>,
    @SerialName("images")
    val images: List<Image>,
    @SerialName("info")
    val info: Info,
    @SerialName("licenses")
    val licenses: List<License>
) {
    @Serializable
    data class Annotation(
        @SerialName("area")
        val area: Int,
        @SerialName("bbox")
        val bbox: List<Int>,
        @SerialName("category_id")
        val categoryId: Int,
        @SerialName("id")
        val id: Int,
        @SerialName("image_id")
        val imageId: Int,
        @SerialName("iscrowd")
        val iscrowd: Int,
        @SerialName("segmentation")
        val segmentation: List<Int>
    )

    @Serializable
    data class Category(
        @SerialName("id")
        val id: Int,
        @SerialName("name")
        val name: String,
        @SerialName("supercategory")
        val supercategory: String
    )

    @Serializable
    data class Image(
        @SerialName("date_captured")
        val dateCaptured: String,
        @SerialName("file_name")
        val fileName: String,
        @SerialName("height")
        val height: Int,
        @SerialName("id")
        val id: Int,
        @SerialName("license")
        val license: Int,
        @SerialName("width")
        val width: Int
    )

    @Serializable
    data class Info(
        @SerialName("contributor")
        val contributor: String,
        @SerialName("date_created")
        val dateCreated: String,
        @SerialName("description")
        val description: String,
        @SerialName("url")
        val url: String,
        @SerialName("version")
        val version: String,
        @SerialName("year")
        val year: String
    )

    @Serializable
    data class License(
        @SerialName("id")
        val id: Int,
        @SerialName("name")
        val name: String,
        @SerialName("url")
        val url: String
    )
}