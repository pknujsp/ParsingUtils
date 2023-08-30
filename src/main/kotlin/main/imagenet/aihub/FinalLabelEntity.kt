package main.imagenet.aihub

import kotlinx.serialization.Serializable

@Serializable
data class FinalLabelEntity(
    val filesMap: Map<String, Set<String>>
)
