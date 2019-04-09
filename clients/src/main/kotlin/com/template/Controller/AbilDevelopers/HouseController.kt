package com.template.Controller.AbilDevelopers

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
@RestController("AbilDevHouseController")
@RequestMapping("/abilDev/house") // The paths for HTTP requests are relative to this base path.
class AbilDevHouseController(@Qualifier("abilDevConnection") rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/owned")
    private fun getHouse(): List<HouseState> {
        return proxy.vaultQueryBy<HouseState>().states.map { it.state.data }
    }
}