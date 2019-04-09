package com.template.states

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.template.utils.CustomisedParty
import com.template.utils.emptyOwner
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import java.security.PublicKey

data class HouseState(val houseNumber: String,
                      val address:String,
                      @JsonIgnore val municipalCorporation: Party,
                      @JsonIgnore val owner: Party) :ContractState{

    override val participants: List<AbstractParty> = listOf(owner, municipalCorporation)

    @JsonGetter("municipalCorporation")
    fun fetchMunicipalCorporation(): CustomisedParty{
        return CustomisedParty(municipalCorporation)
    }

    @JsonGetter("owner")
    fun fetchOwner(): CustomisedParty{
        return CustomisedParty(owner)
    }

    fun withNewOwner(newOwner : Party): HouseState{
        return copy(owner = newOwner)
    }

    fun withoutOwner() : HouseState{
        return copy(owner = emptyOwner())
    }
}