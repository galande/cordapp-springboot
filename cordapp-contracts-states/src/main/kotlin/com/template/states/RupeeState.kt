package com.template.states

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

data class RupeeState(val amount: Int, val issuer: Party, val owner: Party) :ContractState{

    override val participants: List<AbstractParty> = listOf(owner)

    fun withNewOwner(newOwner: Party) : RupeeState{
        return this.copy(owner= newOwner)
    }

}

data class RupeeChange(val otherPartyRupee: RupeeState, val myRupee: RupeeState){

    fun hasChange() : Boolean{
        return (myRupee.amount > 0)
    }
}