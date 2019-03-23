package com.template.contracts

import com.template.states.RupeeState
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class RupeeContract : Contract{

    companion object {
        const val ID = "com.template.contracts.RupeeContract"
    }

    override fun verify(tx: LedgerTransaction) {
       val commands = tx.commands
        requireThat { "There should be atleast one command" using commands.isNotEmpty() }
        val command = commands.first()

        when(command.value){
            is Commands.issue -> {
                requireThat {
                    "There should not be input in tx" using tx.inputStates.isEmpty()
                    val outputs = tx.outputsOfType<RupeeState>()
                    "There must be only one output" using (outputs.size == 1)
                    val rupeeOutput = outputs.first()
                    val requiredSigners = listOf(rupeeOutput.issuer.owningKey, rupeeOutput.owner.owningKey)

                    "Reserve bank and ruppe owner must sign the tx" using (command.signers.toSet() == requiredSigners.toSet())
                }

            }

            is Commands.transfer -> {

                requireThat {

                    val inputs = tx.inputsOfType<RupeeState>()
                    val inputAmount = inputs.map { it.amount }.sum()

                    val outputs =tx.outputsOfType<RupeeState>()
                    val outputAmount = outputs.map { it.amount }.sum()

                    "Input amount and Output amount must be the same" using (inputAmount == outputAmount)
                    "There must be at least one input" using (inputs.isNotEmpty())

                    "there must be atleast one output" using (outputs.isNotEmpty())
                    val inputOwner = inputs.map { it.owner.owningKey }.distinct()
                    val outputOwner = outputs.map { it.owner.owningKey }.distinct()
                    val requiredSigners = inputOwner + outputOwner
                    "Input and output owners must be signers" using (command.signers.containsAll(requiredSigners))
                }



            }
        }
    }

    interface Commands : CommandData{
        class issue: Commands
        class transfer: Commands
    }
}