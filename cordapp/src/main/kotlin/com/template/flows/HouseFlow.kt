package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.HouseContract
import com.template.states.HouseState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalStateException

object HouseFlow {

    @InitiatingFlow
    @StartableByRPC
    class Register(val houseNumber: String, val address: String, val owner: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            object TX_BUILDING : ProgressTracker.Step("Building Transaction")
            object TX_VERIFICATION : ProgressTracker.Step("Verifying Transaction")
            object TX_SIGNING : ProgressTracker.Step("Signing Transaction")
            object TX_FINALISING : ProgressTracker.Step("Sending to Notary and Finalising")
        }

        fun tracker() = ProgressTracker(TX_BUILDING,
                TX_VERIFICATION,
                TX_SIGNING,
                TX_FINALISING)

        override val progressTracker: ProgressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = TX_BUILDING

            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            val houseOutput = HouseState(houseNumber, address, ourIdentity, owner)
            val signers = listOf(ourIdentity.owningKey, owner.owningKey)
            val registerCommand = Command(HouseContract.Commands.Register(), signers)

            val txBuilder = TransactionBuilder(notary).addOutputState(houseOutput, HouseContract.ID)
                    .addCommand(registerCommand)

            progressTracker.currentStep = TX_SIGNING
            val partlySignedTx = serviceHub.signInitialTransaction(txBuilder)

            val counterPartySession = initiateFlow(owner)
            val signedTx = subFlow(CollectSignaturesFlow(partlySignedTx, listOf(counterPartySession)))


            progressTracker.currentStep = TX_VERIFICATION
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = TX_FINALISING
            return subFlow(FinalityFlow(signedTx))
        }

    }

    @InitiatedBy(Register::class)
    class RegisterResponder(val counterPartySession: FlowSession) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            val municipalCorporation = serviceHub.networkMapCache
                    .getPeerByLegalName(CordaX500Name("Municipal Corporation", "Pune", "IN"))
                    ?: throw IllegalStateException("Party Municipal corporation not found")
            val signTransactionFlow = object : SignTransactionFlow(counterPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    "Municipal Corporation can only register the new house" using (counterPartySession.counterparty == municipalCorporation)
                    "Register transaction did not contain an output HouseState" using (stx.toLedgerTransaction(serviceHub, false).outputsOfType<HouseState>().isNotEmpty())
                }
            }

            subFlow(signTransactionFlow)
        }

    }

    @StartableByRPC
    @InitiatingFlow
    class Transfer(val houseNumber: String, val newOwner: Party):FlowLogic<SignedTransaction>(){

        companion object {
            object TX_BUILDING : ProgressTracker.Step("Building Transaction")
            object TX_VERIFICATION : ProgressTracker.Step("Verifying Transaction")
            object TX_SIGNING : ProgressTracker.Step("Signing Transaction")
            object TX_FINALISING : ProgressTracker.Step("Sending to Notary and Finalising")
        }

        fun tracker() = ProgressTracker(TX_BUILDING,TX_VERIFICATION,TX_SIGNING,TX_FINALISING)

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = TX_BUILDING
            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            val inputHouses = serviceHub.vaultService.queryBy<HouseState>().states.singleOrNull{it.state.data.houseNumber == houseNumber}?:throw FlowException("No state found in the vault")
            val inputHouse = inputHouses.state.data
            val outputHouse = inputHouse.withNewOwner(newOwner)
            val command = Command(HouseContract.Commands.Transfer(), listOf(ourIdentity.owningKey, inputHouse.owner.owningKey, outputHouse.owner.owningKey))

            val txBuilder = TransactionBuilder(notary)
                    .addInputState(inputHouses)
                    .addOutputState(outputHouse,HouseContract.ID)
                    .addCommand(command)

            txBuilder.verify(serviceHub)
            val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)
            val counterPartiesSessions = setOf(initiateFlow(inputHouse.owner), initiateFlow(outputHouse.owner))
            val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, counterPartiesSessions))

            return subFlow(FinalityFlow(fullySignedTx))
        }
    }

    @InitiatedBy(Transfer::class)
    class TransferResponder(val counterPartySession: FlowSession): FlowLogic<Unit>(){

        @Suspendable
        override fun call() {
            val municipalCorporation = serviceHub.networkMapCache
                    .getPeerByLegalName(CordaX500Name("Municipal Corporation", "Pune", "IN"))
                    ?: throw IllegalStateException("Party Municipal corporation not found")
            val signedTransaction = object : SignTransactionFlow(counterPartySession){
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    "Municipal Corporation can only register the new house" using (counterPartySession.counterparty == municipalCorporation)
                    "Municipal Corporation must sign first" using (stx.sigs.any { sigs-> sigs.by == counterPartySession.counterparty.owningKey })
                }
            }
            subFlow(signedTransaction)
        }
    }
}
