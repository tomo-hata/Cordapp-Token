package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import java.util.*

import java.io.IOException
import java.nio.*
import java.nio.file.Path
import java.nio.file.Paths
import java.util.List
import java.util.UUID
import java.nio.file.Files.readAllLines
import java.lang.Object
import java.io.File


/**
 *  Move Payment Token using MoveFungibleTokens flow
 */
@StartableByRPC
class MovePaymentToken(
        //val currency: String,
        //val quantity: Long,
        //val recipient: Party
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // read input file
        val inputfile: String = "C:\\intanalpocParm\\" + "Parm_movePT.txt"
        val text = mutableListOf<String>()
        File(inputfile).useLines { lines -> text.addAll(lines) }
        //text.forEachIndexed { i, line -> println("${i}: " + line) }

        val currency = text[0]
        val quantity = text[1].toLong()
        val recipient = serviceHub.identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(text[2]))!!.toParty(serviceHub)

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val paymentToken = FiatCurrency.Companion.getInstance(currency)
        val amount = Amount(quantity,paymentToken)

        // checking sufficient amount for paying to Payment Tokend
        val balance = serviceHub.vaultService.tokenBalance(token = paymentToken)
        val holdingPTQuantity = balance.quantity
        println("Payer holding Payment Token : " + holdingPTQuantity + " of " + balance.token.tokenIdentifier)
        println("Payer should settle to Recipient( " + recipient + " ) : " + quantity + " of " + balance.token.tokenIdentifier)

        if (quantity > holdingPTQuantity) {
            val errorMsgForInsufficientPT = "Payer do not hold sufficient payment token to recipient."
            println(errorMsgForInsufficientPT)
            throw IllegalArgumentException(errorMsgForInsufficientPT)
        }

        //fungibletoken
        val moveTokensFlow = MoveFungibleTokens(listOf(PartyAndAmount( recipient, quantity of paymentToken)))
        return subFlow(moveTokensFlow)
    }
}
