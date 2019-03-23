package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.sun.org.apache.xerces.internal.impl.xpath.XPath
import com.template.contracts.HouseContract
import com.template.states.HouseState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

object HouseRegTransFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val houseNumber: String, val address: String, val owner: Party, val newOwner: Party) : FlowLogic<SignedTransaction>() {

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
            val signers = listOf(ourIdentity.owningKey,owner.owningKey)
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
            subFlow(FinalityFlow(signedTx))

            println("Registration complete...Transfering to new owner")
            val info = TransferInfo(houseNumber,newOwner)
            counterPartySession.send(info)
            println("Asked counterParty $owner to transferOwnership to $newOwner...and waiting to confirmation")

            val ftx = counterPartySession.receive<SignedTransaction>().unwrap { it }
            println("Recived final tx $ftx")
            return ftx
        }

    }

    @InitiatedBy(Initiator::class)
    class Reciever(val counterPartySession: FlowSession) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(counterPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                    if (stx.toLedgerTransaction(serviceHub, false).outputsOfType<HouseState>().isEmpty()) {
                        throw FlowException("Register transaction did not contain an output HouseState.")
                    }
                }
            }

            subFlow(signTransactionFlow)

            val transferInfo = counterPartySession.receive<TransferInfo>().unwrap{ it}
            println("received transfer info: ${transferInfo.houseNumber}, ${transferInfo.newOwner}")

            val tranTx = subFlow(TransferHouseFlow.Initiator(transferInfo.houseNumber,transferInfo.newOwner))
            println("Sending back signed tx of TransferHouseFlow")
            counterPartySession.send(tranTx)
        }

    }

    @CordaSerializable
    data class TransferInfo(val houseNumber: String, val newOwner: Party)
}
