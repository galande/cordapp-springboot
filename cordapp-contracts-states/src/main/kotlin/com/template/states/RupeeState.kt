package com.template.states

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.template.utils.CustomisedParty
import com.template.utils.emptyOwner
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

data class RupeeState(val amount: Int,
                      @JsonIgnore val issuer: Party,
                      @JsonIgnore val owner: Party) :ContractState{

    override val participants: List<AbstractParty> = listOf(issuer, owner)

    @JsonGetter("issuer")
    fun fetchIssuer(): CustomisedParty{
        return CustomisedParty(issuer)
    }

    @JsonGetter("owner")
    fun fetchOwner(): CustomisedParty {
        return CustomisedParty(owner)
    }

    fun withNewOwner(newOwner: Party) : RupeeState{
        return this.copy(owner= newOwner)
    }

    fun withoutOwner(): RupeeState{
        return copy(owner = emptyOwner())
    }

}

data class RupeeChange(val otherPartyRupee: RupeeState, val myRupee: RupeeState){

    fun hasChange() : Boolean{
        return (myRupee.amount > 0)
    }
}