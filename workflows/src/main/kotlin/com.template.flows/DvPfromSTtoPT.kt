package com.template.flows

//import net.corda.core.flows.IdentitySyncFlow
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import com.template.states.TestTokenType
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.io.File
import java.io.IOException
import java.lang.Object
import java.nio.*
import java.nio.file.Files.readAllLines
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.UUID

import java.util.Date
import java.text.SimpleDateFormat

import kotlin.collections.List


/**
 *  DvP from Security Token to Fiatcurrency(payment token)
 */
@StartableByRPC
@InitiatingFlow
class DvPfromSTtoPTInitiateFlow(
        //val eid: String,
        //val quantityByST: Long,
        //val recipient: String,
        //val currency: String,
        //val quantityByPT: Long
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        // read input file
        val inputfile: String = "C:\\intanalpocParm\\" + "Parm_dvpfromSTtoPT.txt"
        val text = mutableListOf<String>()
        File(inputfile).useLines { lines -> text.addAll(lines) }
        //text.forEachIndexed { i, line -> println("${i}: " + line) }

        val eid = text[0]
        val quantityByST = text[1].toLong()
        val recipient = serviceHub.identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(text[2]))!!.toParty(serviceHub)
        val currency = text[3]
        val quantityByPT = text[4].toLong()
        val exeDvPDateStr = text[5]



        val uuid = UUID.fromString(eid)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(uuid))
        val tokenStateAndRef = serviceHub.vaultService.queryBy<EvolvableTokenType>(queryCriteria).states.single()
        val token = tokenStateAndRef.state.data.toPointer<EvolvableTokenType>()
        // Starting this flow with a new flow session.

        val evolvableTokenType: TestTokenType = tokenStateAndRef.state.data as TestTokenType

        // add 20200128 : check redemptionDate
        val redemptionDate = SimpleDateFormat("yyyy/MM/dd").let{
            val parsed = it.parse(evolvableTokenType.redemptionDate)
            it.format(parsed)
        }

        println("redemptionDate : " + redemptionDate)

        val exeDvPDate = SimpleDateFormat("yyyy/MM/dd").let{
            val parsed = it.parse(exeDvPDateStr)
            it.format(parsed)
        }

        println("exeDvPDate : " + exeDvPDate)

        if (redemptionDate <= exeDvPDate) {
            val errorMsgForRedemptionDateOver = "DvP can not be done, because RedemptionDate has already came."
            println(errorMsgForRedemptionDateOver)
            throw IllegalArgumentException(errorMsgForRedemptionDateOver)
        }


        // checking sufficient amount for moving to Security Token
        val balance = serviceHub.vaultService.tokenBalance(token = token)
        val holdingSTQuantity = balance.quantity
        println("Payer holding Security Token : " + holdingSTQuantity + " of " + balance.token.tokenIdentifier)
        println("Payer should move to Recipient( " + recipient + " ) : " + quantityByST + " of " + balance.token.tokenIdentifier)

        if (quantityByST > holdingSTQuantity) {
            val errorMsgForInsufficientST = "Payer do not hold sufficient security token to recipient."
            println(errorMsgForInsufficientST)
            throw IllegalArgumentException(errorMsgForInsufficientST)
        }


        val amount =  quantityByST of token
        // We can specify preferred notary in cordapp config file, otherwise the first one from network parameters is chosen.

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txBuilder = TransactionBuilder(notary)

        addMoveFungibleTokens(txBuilder, serviceHub, listOf(PartyAndAmount( recipient, amount)),ourIdentity)

        // Initiate new flow session. If this flow is supposed to be called as inline flow, then session should have been already passed.
        val session = initiateFlow(recipient)
        // Ask for input stateAndRefs - send notification with the amount to exchange.
        session.send(Trade(currency,quantityByPT))
        // Receive GBP states back.
        val inputs = subFlow(ReceiveStateAndRefFlow<FungibleToken>(session))
        // Receive outputs.
        val outputs = session.receive<List<FungibleToken>>().unwrap { it }

        addMoveTokens(txBuilder, inputs, outputs)

        //subFlow(SyncKeyMappingFlow(session, txBuilder.toWireTransaction(serviceHub)))

        // Because states on the transaction can have confidential identities on them, we need to sign them with corresponding keys.
        val ourSigningKeys = txBuilder.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub)
        val initialStx = serviceHub.signInitialTransaction(txBuilder, signingPubKeys = ourSigningKeys)
        // Collect signatures from the new house owner.
        val stx = subFlow(CollectSignaturesFlow(initialStx, listOf(session), ourSigningKeys))

        // Update distribution list.
        subFlow(UpdateDistributionListFlow(stx))
        // Finalise transaction! If you want to have observers notified, you can pass optional observers sessions.
        val ftx = subFlow(ObserverAwareFinalityFlow(stx, listOf(session)))

        return ftx
    }

    @InitiatedBy(DvPfromSTtoPTInitiateFlow::class)
    class DvPfromSTtoPTFlowHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            // Receive Trade.
            val trdata = otherSession.receive<Trade>().unwrap { it }
            val currency = trdata.currency
            val quantityByPT = trdata.quantityByPT

            val paymentToken = FiatCurrency.Companion.getInstance(currency)
            val amount = Amount(quantityByPT,paymentToken)


            // checking sufficient amount for paying to Payment Token
            val balance = serviceHub.vaultService.tokenBalance(token = paymentToken)
            val holdingPTQuantity = balance.quantity
            println("Payer holding Payment Token : " + holdingPTQuantity + " of " + balance.token.tokenIdentifier)
            println("Payer should settle to Recipient( " + otherSession.counterparty + " ) : " + quantityByPT + " of " + balance.token.tokenIdentifier)

            if (quantityByPT > holdingPTQuantity) {
                val errorMsgForInsufficientPT = "Payer do not hold sufficient payment token to recipient."
                println(errorMsgForInsufficientPT)
                throw IllegalArgumentException(errorMsgForInsufficientPT)
            }


            // Generate fresh key, possible change outputs will belong to this key.
            //val changeHolder = serviceHub.keyManagementService.freshKeyAndCert(ourIdentityAndCert, false).party.anonymise()
            val changeHolder = ourIdentity
            // Chose state and refs to send back.

            val (inputs, outputs) = TokenSelection(serviceHub).generateMove(
                    lockId = runId.uuid,
                    partyAndAmounts = listOf(PartyAndAmount(otherSession.counterparty,amount)),
                    changeHolder = changeHolder
            )
            subFlow(SendStateAndRefFlow(otherSession, inputs))
            otherSession.send(outputs)
            //subFlow(SyncKeyMappingFlowHandler(otherSession))

            subFlow(object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    // We should perform some basic sanity checks before signing the transaction. This step was omitted for simplicity.
                }
            })
            subFlow(ObserverAwareFinalityFlowHandler(otherSession))
        }
    }
}

@CordaSerializable
data class Trade(val currency: String,val quantityByPT: Long)
