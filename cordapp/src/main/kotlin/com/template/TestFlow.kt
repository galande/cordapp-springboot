package com.template

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.HouseContract
import com.template.flows.HouseRegisterFlow
import com.template.states.HouseState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

object TestFlow{

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val counterParty : Party) : FlowLogic<Unit>() {

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
        override fun call(){
            progressTracker.currentStep = TX_BUILDING

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            val counterPartySession = initiateFlow(counterParty)
            val myName = "Bhausaheb"
            counterPartySession.send(myName)
            println("Sent to counterparty")

            val fullName = counterPartySession.receive<String>().unwrap { it }
            println("Recieved full name $fullName")

        }

    }

    @InitiatedBy(Initiator::class)
    class Reciever(val counterPartySession: FlowSession) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            val myName = counterPartySession.receive<String>().unwrap {it }
            println("Recived to otherparty name $myName")
            val fullName = "$myName Galande"
            println("Sending back full name $fullName")
            counterPartySession.send(fullName)
        }

    }
}