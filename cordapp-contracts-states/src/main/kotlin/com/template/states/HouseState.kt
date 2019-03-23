package com.template.states

import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

data class HouseState(val houseNumber: String, val address:String, val issuer: Party, val owner: Party) :ContractState{

    override val participants: List<AbstractParty> = listOf(owner)
}