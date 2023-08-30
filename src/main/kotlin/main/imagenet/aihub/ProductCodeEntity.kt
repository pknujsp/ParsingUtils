package main.imagenet.aihub

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ProductCodeEntity(
    @SerialName("dlMap") val dlMap: Map<String, String>,
    @SerialName("itemSeqMap") val itemSeqMap: Map<String, String>,
)
