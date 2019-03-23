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

                val requiredSigners = listOf(outputStates.first().issuer.owningKey, outputStates.first().owner.owningKey)
                requireThat { "Issue must sign the tx" using (command.signers.containsAll(requiredSigners)) }
            }
            is Commands.Transfer ->{
                val inputHouse = tx.inputsOfType<HouseState>()
                val outputHouse = tx.outputsOfType<HouseState>()
                requireThat { "There must be only one House Input State" using (inputHouse.size == 1) }
                requireThat { "There must be only one House Output State" using (outputHouse.size == 1) }
                val requiredSignrs = listOf(inputHouse.first().owner.owningKey)
                requireThat { "Old owner of house must sign this transaction" using (command.signers.containsAll(requiredSignrs)) }
            }
        }
    }

    interface Commands: CommandData{
        class Register: Commands
        class Transfer: Commands
    }

}