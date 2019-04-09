package com.template.utils

import net.corda.core.crypto.NullKeys
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.toBase58String

fun emptyOwner() : Party{
    return Party(CordaX500Name("Dummy","Dummy","IN"), NullKeys.NullPublicKey)
}

data class CustomisedParty(val organisation: String, val locality: String, val country: String, val key: String){
    constructor(party: Party): this(
            party.name.organisation,
            party.name.locality,
            party.name.country,
            party.owningKey.toBase58String()
    )
}