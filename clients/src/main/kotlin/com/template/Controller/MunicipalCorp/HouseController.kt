package com.template.Controller.MunicipalCorp

import com.template.config.NodeRPCConnection
import com.template.flows.HouseFlow
import com.template.states.HouseState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.*

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
}