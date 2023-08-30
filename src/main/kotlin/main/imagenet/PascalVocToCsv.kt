package main.imagenet

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

const val srcSize = 320
const val resize = 512
const val sizeRatio: Int = resize / srcSize

fun main() {
    val path = "C:\\Users\\USER\\Desktop\\tf\\dataset\\annotation"
    val csvPath = "C:\\Users\\USER\\Desktop\\tf\\dataset\\annots.csv"
    val csvFile = File(csvPath)
    if (csvFile.exists()) {
        csvFile.delete()
    }

    val out = csvFile.bufferedWriter()
    val documentFactory = DocumentBuilderFactory.newInstance()

    val annotsDir = File(path)

    out.use { writer ->
        annotsDir.listFiles()!!.forEach { xmlFile ->
            val annotation = parseXml(xmlFile.readText())
            
        }
    }

}


fun parseXml(xml: String): Annotation {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val document = builder.parse(xml.byteInputStream())

    val annotationElement = document.documentElement
    val filename = annotationElement.getElementsByTagName("filename").item(0).textContent
    val path = annotationElement.getElementsByTagName("path").item(0).textContent
    val width = annotationElement.getElementsByTagName("width").item(0).textContent.toInt()
    val height = annotationElement.getElementsByTagName("height").item(0).textContent.toInt()
    val depth = annotationElement.getElementsByTagName("depth").item(0).textContent.toInt()

    val objects = annotationElement.getElementsByTagName("object").run {
        val count = length
        (0..<count).map {
            val objectElement = item(it) as Element
            val name = objectElement.getElementsByTagName("name").item(0).textContent
            val xmin = objectElement.getElementsByTagName("xmin").item(0).textContent.toInt().resize()
            val xmax = objectElement.getElementsByTagName("xmax").item(0).textContent.toInt().resize()
            val ymin = objectElement.getElementsByTagName("ymin").item(0).textContent.toInt().resize()
            val ymax = objectElement.getElementsByTagName("ymax").item(0).textContent.toInt().resize()
            ObjectInfo(name, xmin, xmax, ymin, ymax)
        }
    }

    return Annotation(filename, path, width, height, depth, objects)
}

fun Int.resize(): Int = this * sizeRatio

data class Annotation(
    val filename: String,
    val path: String,
    val width: Int,
    val height: Int,
    val depth: Int,
    val objectInfo: List<ObjectInfo>
)

data class ObjectInfo(
    val name: String, val xmin: Int, val xmax: Int, val ymin: Int, val ymax: Int
)


/**
TRAINING,gs://cloud-ml-data/img/openimage/3/2520/3916261642_0a504acd60_o.jpg,Salad,0.0,0.0954,,,0.977,0.957,,
VALIDATION,gs://cloud-ml-data/img/openimage/3/2520/3916261642_0a504acd60_o.jpg,Seafood,0.0154,0.1538,,,1.0,0.802,,
TEST,gs://cloud-ml-data/img/openimage/3/2520/3916261642_0a504acd60_o.jpg,Tomato,0.0,0.655,,,0.231,0.839,,
Each row corresponds to an object localized inside a larger image, with each object specifically designated as test, train, or validation data. You'll learn more about what that means in a later stage in this notebook.
The three lines included here indicate three distinct objects located inside the same image available at gs://cloud-ml-data/img/openimage/3/2520/3916261642_0a504acd60_o.jpg.
Each row has a different label: Salad, Seafood, Tomato, etc.
Bounding boxes are specified for each image using the top left and bottom right vertices.
Here is a visualization of these three lines:
 */


/*
<annotation>
	<folder></folder>
	<filename>cefalexin-2-_jpg.rf.819825aa1fdbe72853431630541fdc9a.jpg</filename>
	<path>cefalexin-2-_jpg.rf.819825aa1fdbe72853431630541fdc9a.jpg</path>
	<source>
		<database>roboflow.com</database>
	</source>
	<size>
		<width>320</width>
		<height>320</height>
		<depth>3</depth>
	</size>
	<segmented>0</segmented>
	<object>
		<name>Drug</name>
		<pose>Unspecified</pose>
		<truncated>0</truncated>
		<difficult>0</difficult>
		<occluded>0</occluded>
		<bndbox>
			<xmin>70</xmin>
			<xmax>223</xmax>
			<ymin>145</ymin>
			<ymax>207</ymax>
		</bndbox>
	</object>
</annotation>

 */