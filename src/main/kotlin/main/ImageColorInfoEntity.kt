package main

import kotlinx.serialization.Serializable

@Serializable
data class ImageColorInfoEntity(
    val images: List<Item>
) {

    /**
     * @param itemSeq: 품목코드
     * @param colors: 색상 정보 = key: 색상명, value: 색상명에 해당하는 이미지 파일명 리스트
     */
    @Serializable
    data class Item(
        val itemSeq: String, val colors: Map<String, List<String>>
    )
}
