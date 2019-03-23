package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.HouseContract
import com.template.states.HouseState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object TransferHouseFlow{

    @StartableByRPC
    @InitiatingFlow
    class Initiator(val houseNumber: String, val newOwner: Party):FlowLogic<SignedTransaction>(){

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
            val outputHouse = HouseState(inputHouse.houseNumber,inputHouse.address,inputHouse.issuer,newOwner)
            val command = Command(HouseContract.Commands.Transfer(),inputHouse.owner.owningKey)

            val txBuilder = TransactionBuilder(notary).addInputState(inputHouses).addOutputState(outputHouse,HouseContract.ID)
                    .addCommand(command)

            val stx = serviceHub.signInitialTransaction(txBuilder)
            txBuilder.verify(serviceHub)

            return subFlow(FinalityFlow(stx))

        }
    }

//    @InitiatedBy(Initiator::class)
//    class Responder(flowSession: FlowSession): FlowLogic<Unit>(){
//
//        @Suspendable
//        override fun call() {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//    }
}

