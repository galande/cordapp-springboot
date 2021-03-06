package com.template.Controller.CentralBank

import com.template.config.NodeRPCConnection
import com.template.flows.RupeeFlow
import com.template.states.RupeeState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.*

/**
 * Define your API endpoints here.
 */
@RestController("CentralBankController")
@RequestMapping("/centralBank/rupee") // The paths for HTTP requests are relative to this base path.
class CentralBankController(@Qualifier("centralBankConnection") rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/all")
    private fun getRupee(): List<RupeeState> {
        return proxy.vaultQueryBy<RupeeState>().states.map { it.state.data }
    }

    @PostMapping(value = "/issue")
    private fun issueRupee(@RequestParam amount: Int, @RequestParam owner: String): String{
        val ownerParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(owner))?: "Unknown Party"

        return proxy.startFlowDynamic(RupeeFlow.Issue::class.java,amount, ownerParty).returnValue.get().toString()
    }
}