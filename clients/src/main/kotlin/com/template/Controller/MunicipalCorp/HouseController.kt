package com.template.Controller.MunicipalCorp

import com.template.config.NodeRPCConnection
import com.template.flows.HouseFlow
import com.template.states.HouseState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.transactions.CoreTransaction
import net.corda.core.transactions.SignedTransaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Define your API endpoints here.
 */
@RestController("MuniciapalCorpController")
@RequestMapping("/municiaplCorp/house") // The paths for HTTP requests are relative to this base path.
class MuniciapalCorpController(@Qualifier("municipalCorpConnection") rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/all")
    private fun getHouse(): List<HouseState> {
        return proxy.vaultQueryBy<HouseState>().states.map { it.state.data }
    }

    @GetMapping("/history")
    private fun getHistory(): List<CoreTransaction>{
        return proxy.internalVerifiedTransactionsSnapshot().map { it.coreTransaction }
    }

    @PostMapping(value = "/register")
    private fun registerHouse(@RequestParam houseNumber: String, @RequestParam address: String, @RequestParam owner: String): String{
        val ownerParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(owner))?: "Unknown Party"

        return proxy.startFlowDynamic(HouseFlow.Register::class.java, houseNumber, address, ownerParty).returnValue.get().toString()
    }

    @PostMapping(value = "/transfer")
    private fun transferHouse(@RequestParam houseNumber: String, @RequestParam owner: String): String{
        val newOwner = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(owner))?: "Unknown Party"

        return proxy.startFlowDynamic(HouseFlow.Transfer::class.java, houseNumber, newOwner).returnValue.get().toString()
    }

    @PostMapping(value = "/upload")
    private fun uploadInfo(@RequestBody file: MultipartFile): String{

        val zipFile = createZip(file, file.originalFilename)
        val hash = proxy.uploadAttachment(zipFile)
        return hash.toString()
    }

    @GetMapping("/download")
    private fun getAttachemnt(@RequestParam("hash") hash: String) : ResponseEntity<InputStreamResource>{
        val inputStream = proxy.openAttachment(SecureHash.parse(hash))
        val zis = ZipInputStream(inputStream)

        zis.nextEntry

        val fileName = "test.xml"
        return return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(InputStreamResource(zis))
    }

    private fun createZip(file: MultipartFile, fileName: String): ByteArrayInputStream{
        val byteArrayOutputStream = ByteArrayOutputStream()

        try {
            val zos = ZipOutputStream(byteArrayOutputStream)

            val entry = ZipEntry(fileName)
            zos.putNextEntry(entry)
            zos.write(file.bytes)
            zos.closeEntry()

        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

        return ByteArrayInputStream(byteArrayOutputStream.toByteArray())
    }
}