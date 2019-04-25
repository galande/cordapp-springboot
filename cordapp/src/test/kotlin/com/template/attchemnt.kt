package com.template

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream



fun main(args: Array<String>){

    val xmlFile = File("/home/bg/Blockchain/corda/myprojects/cordapp-springboot/cordapp/src/test/kotlin/com/template/test.xml")
    val inputStream = xmlFile.inputStream()


    val byteArrayOutputStream = ByteArrayOutputStream()

    try {
        ZipOutputStream(byteArrayOutputStream).use { zos ->

            val entry = ZipEntry("test.xml")

            zos.putNextEntry(entry)
            zos.write(inputStream.read())
            zos.closeEntry()
        }
    } catch (ioe: IOException) {
        ioe.printStackTrace()
    }


    val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
    println(byteArrayInputStream.read())
}
