package com.template.Controller

import com.template.flows.RupeeFlow
import com.template.config.NodeRPCConnection
import net.corda.core.identity.CordaX500Name
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

/**
 * Define your API endpoints here.
 */
@RestController("PartyAController")
@RequestMapping("/partyA") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
    private fun templateendpoint(): String {
        return "Define an endpoint here."
    }

    @PostMapping(value = "/issueRupee")
    private fun issueRupee(@RequestParam amount: Int, @RequestParam owner: String): String{
        val ownerParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(owner))?: "Unknown Party"

        return proxy.startFlowDynamic(RupeeFlow.Issue::class.java,amount, ownerParty).returnValue.get().toString()
    }
}