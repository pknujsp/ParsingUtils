package main

import kotlinx.serialization.Serializable
import org.opencv.core.Mat

/*
CSV 행 형식 : gs://bucket/filename4.bmp,sunflowers
 */

@Serializable
data class JsonEntity(
    val annotations: List<Annotation>,
    val images: List<Image>,
) {
    @Serializable
    data class Annotation(
        val bbox: List<Int>,
    )

    @Serializable
    data class Image(
        val file_name: String,
        val item_seq: Long,
        val drug_dir: String,
        val dl_mapping_code: String,
        val print_front: String = "",
        val print_back: String = ""
    )
}


data class ImageEntity(
    val fileName: String,
    val mat: Mat,
    var path: String,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    val itemSeq: String,
) {
    constructor(fileName: String, mat: Mat, percent: Int) : this(fileName, mat, "", 0, 0, 0, 0, "")
}


/**
{
"images": [
{
"file_name": "K-000030_0_2_0_1_90_300_200.png",
"width": 976,
"height": 1280,
"imgfile": "K-000030_0_2_0_1_90_300_200.png",
"drug_N": "K-000030",
"drug_S": "정상알약",
"back_color": "연회색 배경",
"drug_dir": "앞면",
"light_color": "주광색",
"camera_la": 90,
"camera_lo": 300,
"size": 200,
"dl_idx": "29",
"dl_mapping_code": "K-000030",
"dl_name": "테라싸이클린캡슐 250mg",
"dl_name_en": "Teracyclin Cap. 250mg",
"img_key": "http://connectdi.com/design/img/drug/154601161228500067.jpg",
"dl_material": "테트라사이클린염산염",
"dl_material_en": "Tetracycline Hydrochloride",
"dl_custom_shape": "경질캡슐제",
"dl_company": "(주)종근당",
"dl_company_en": "Chong Kun Dang Pharmaceutical",
"di_company_mf": "",
"di_company_mf_en": "",
"item_seq": 196000001,
"di_item_permit_date": "19600614",
"di_class_no": "[06150]주로 그람양성, 음성균, 리케치아, 비루스에 작용하는 것",
"di_etc_otc_code": "전문의약품",
"di_edi_code": "643303740,A01200351",
"chart": "황색의 결정 또는 결정성 가루가 들어 있는 상부는 갈색, 하부는 담회색의 캅셀이다.",
"drug_shape": "장방형",
"thick": 6.35,
"leng_long": 17.6,
"leng_short": 6.07,
"print_front": "CKD 250",
"print_back": "",
"color_class1": "갈색",
"color_class2": "청록",
"line_front": "",
"line_back": "",
"img_regist_ts": "20041126",
"form_code_name": "경질캡슐제, 산제",
"mark_code_front_anal": "",
"mark_code_back_anal": "",
"mark_code_front_img": "",
"mark_code_back_img": "",
"mark_code_front": "",
"mark_code_back": "",
"change_date": "20200313",
"id": 1
}
],
"type": "instances",a
"annotations": [
{
"area": 105469,
"iscrowd": 0,
"bbox": [
266,
546,
427,
247
],
"category_id": 1,
"ignore": 0,
"segmentation": [],
"id": 1,
"image_id": 1
}
],
"categories": [
{
"supercategory": "pill",
"id": 1,
"name": "Drug"
}
]
}
 */