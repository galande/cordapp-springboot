package com.template.contracts

import com.template.states.HouseState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class HouseContract : Contract{

    companion object {
        const val ID = "com.template.contracts.HouseContract"
    }

    override fun verify(tx: LedgerTransaction) {
        requireThat { "Tx must contain command" using (!tx.commands.isEmpty()) }

        val command = tx.getCommand<Commands>(0)
        when(command.value){
            is Commands.Register ->{
                requireThat { "There should not be Input state" using tx.inputStates.isEmpty() }

                val outputStates = tx.outputsOfType<HouseState>()
                requireThat { "There should be only one output" using (outputStates.size == 1) }

                val requiredSigners = listOf(outputStates.first().municipalCorporation.owningKey, outputStates.first().owner.owningKey)
                requireThat { "Issue must sign the tx" using (command.signers.containsAll(requiredSigners)) }
            }
            is Commands.Transfer ->{
                val inputHouses = tx.inputsOfType<HouseState>()
                val outputHouses = tx.outputsOfType<HouseState>()
                requireThat { "There must be only one House Input State" using (inputHouses.size == 1) }
                requireThat { "There must be only one House Output State" using (outputHouses.size == 1) }

                val inputHouse = inputHouses.first()
                val outputHouse = outputHouses.first()
                requireThat { "Input and Output House must be same except owner" using (inputHouse.withoutOwner() == outputHouse.withoutOwner()) }

                val requiredSignrs = setOf(inputHouse.municipalCorporation.owningKey, inputHouse.owner.owningKey,outputHouse.owner.owningKey)
                println("requiredSignrs: ${requiredSignrs.size}")
                println("CommandSigners: ${command.signers.toSet().size}")
                requireThat { "Old owner of house must sign this transaction" using (command.signers.toSet() == requiredSignrs) }
            }
        }
    }

    interface Commands: CommandData{
        class Register: Commands
        class Transfer: Commands
    }

}