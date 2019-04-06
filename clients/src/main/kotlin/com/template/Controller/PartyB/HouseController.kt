package com.template.Controller.PartyA

import com.template.config.NodeRPCConnection
import com.template.flows.HouseRegisterFlow
import com.template.flows.RupeeFlow
import com.template.states.HouseState
import com.template.states.RupeeState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.*

/**
 * Define your API endpoints here.
 */
@RestController("PartyAHouseController")
@RequestMapping("/partyA/house") // The paths for HTTP requests are relative to this base path.
class HouseController(@Qualifier("partyAConnection") rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/owned")
    private fun getHouse(): List<HouseState> {
        return proxy.vaultQueryBy<HouseState>().states.map { it.state.data }
    }

    @PostMapping(value = "/register")
    private fun issueRupee(@RequestParam houseNumber: String, @RequestParam address: String, @RequestParam owner: String): String{
        val ownerParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(owner))?: "Unknown Party"

        return proxy.startFlowDynamic(HouseRegisterFlow.Initiator::class.java, houseNumber, address, ownerParty).returnValue.get().toString()
    }
}