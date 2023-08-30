package main

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

const val DRUG_DIRECTION_FRONT = "앞면"
const val DRUG_DIRECTION_BACK = "뒷면"

fun main() {
    runBlocking {
        ClassParser(1).convert(supervisorJob = SupervisorJob(), imageZipFilePath = "D://dataset/images")
    }
}