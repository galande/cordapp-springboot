package com.template

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.RupeeContract
import com.template.states.RupeeChange
import com.template.states.RupeeState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.ProgressTracker


object RupeeFlow{

    @StartableByRPC
    @InitiatingFlow
    class Issue(private val amount : Int, private val owner : Party): FlowLogic<Unit>(){

        companion object {
            object TX_BUILDING : ProgressTracker.Step("Building Transaction")
            object TX_VERIFICATION : ProgressTracker.Step("Verifying Transaction")
            object TX_SIGNING : ProgressTracker.Step("Signing Transaction")
            object TX_FINALISING : ProgressTracker.Step("Sending to Notary and Finalising")
            fun trackor() = ProgressTracker(TX_BUILDING, TX_VERIFICATION, TX_SIGNING, TX_FINALISING)
        }

        override val progressTracker = trackor()

        @Suspendable
        override fun call() {
            progressTracker.currentStep = TX_BUILDING
           val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val rupeeOutput = RupeeState(amount,ourIdentity,owner)
            val command = Command(RupeeContract.Commands.issue(), listOf(ourIdentity.owningKey,owner.owningKey))

            val txBuilder = TransactionBuilder(notary).addOutputState(rupeeOutput, RupeeContract.ID)
                    .addCommand(command)

            progressTracker.currentStep = TX_VERIFICATION
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = TX_SIGNING
            val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)

            val counterParty = initiateFlow(owner)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, listOf(counterParty)))

            progressTracker.currentStep = TX_FINALISING
            subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(Issue::class)
    class Responder(val counterPartySession: FlowSession) : FlowLogic<Unit>(){

        @Suspendable
        override fun call() {

            val reserveBank = serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name("PartyA","London","GB"))

            val signTransactionFlow = object : SignTransactionFlow(counterPartySession){
                override fun checkTransaction(stx: SignedTransaction) {
                    requireThat {
                        "Only Reserve Bank can issue the rupee" using (counterPartySession.counterparty == reserveBank)
                        "Bank must sign tx first" using (stx.sigs.any { sig -> sig.by == counterPartySession.counterparty.owningKey})
                    }
                }
            }

            subFlow(signTransactionFlow)
        }
    }

    @StartableByRPC
    @InitiatingFlow
    class Transfer(private val amount: Int, private val newOwner: Party) : FlowLogic<Unit>(){

        companion object {
            object TX_BUILDING : ProgressTracker.Step("Building Transaction")
            object TX_VERIFICATION : ProgressTracker.Step("Verifying Transaction")
            object TX_SIGNING : ProgressTracker.Step("Signing Transaction")
            object TX_FINALISING : ProgressTracker.Step("Sending to Notary and Finalising")
            fun trackor() = ProgressTracker(TX_BUILDING, TX_VERIFICATION, TX_SIGNING, TX_FINALISING)
        }

        override val progressTracker = trackor()


        @Suspendable
        override fun call() {
            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            val txBuilder = TransactionBuilder(notary)

            val rupeeInputs = serviceHub.vaultService.queryBy<RupeeState>().states
            val rupeesInput = calculateStates(amount,rupeeInputs)

            rupeesInput.forEach { txBuilder.addInputState(it)}

            val finalStates = calculateChange(rupeesInput.map { it.state.data },amount, newOwner)

            val command = Command(RupeeContract.Commands.transfer(), listOf(ourIdentity.owningKey,finalStates.otherPartyRupee.owner.owningKey))
            txBuilder.addOutputState(finalStates.otherPartyRupee,RupeeContract.ID )
                    .addCommand(command)

            if (finalStates.hasChange()) txBuilder.addOutputState(finalStates.myRupee, RupeeContract.ID)

            txBuilder.verify(serviceHub)

            val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)

            val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, listOf(initiateFlow(newOwner))))
            subFlow(FinalityFlow(fullySignedTx))
        }
    }


    @InitiatedBy(Transfer::class)
    class TransferResponder(val session: FlowSession) : FlowLogic<Unit>(){

        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(session) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                    "Owner must sign the tx first" using (stx.sigs.any { sig -> sig.by == session.counterparty.owningKey })
                }
            }

            subFlow(signTransactionFlow)
        }
    }

    fun calculateStates(amount : Int, states: List<StateAndRef<RupeeState>>) : List<StateAndRef<RupeeState>>{

        var total = 0
        var inputs = listOf<StateAndRef<RupeeState>>()
       for (rupee in states){
           if (total >= amount) break
           total += rupee.state.data.amount
           inputs += rupee
       }

        requireThat { "Party does not have enough Rupee" using (total >= amount) }
        return inputs
    }

    fun calculateChange(rupees: List<RupeeState>, unit : Int, newOwner: Party) : RupeeChange {

        val total = rupees.map { it.amount }.sum()
        val totalRupee = rupees.first().copy(amount = total)
        return RupeeChange(totalRupee.copy(amount = unit, owner = newOwner),totalRupee.copy(amount = total-unit))
    }

}