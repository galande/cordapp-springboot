package com.template.Controller.AbilDevelopers

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
@RestController("AbilDevRupeeController")
@RequestMapping("/abilDev/rupee") // The paths for HTTP requests are relative to this base path.
class AbilDevRupeeController(@Qualifier("abilDevConnection") rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/owned")
    private fun getRupee(): List<RupeeState> {
        return proxy.vaultQueryBy<RupeeState>().states.map { it.state.data }
    }
}