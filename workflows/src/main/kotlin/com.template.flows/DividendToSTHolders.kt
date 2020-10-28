package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import com.template.states.*
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
 *  Dividend Payment Token to Securitytoken Holders
 */
@StartableByRPC
class DividendToSTHolders(

) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // read input file
        var inputfile: String = "C:\\intanalpocParm\\" + "Parm_dividendToken.txt"
        var text = mutableListOf<String>()
        File(inputfile).useLines { lines -> text.addAll(lines) }
        //text.forEachIndexed { i, line -> println("${i}: " + line) }

        val evolvableTokenId = text[0]
        val currency = text[1]
        val couponPaymentNum = text[2].toInt()
        //val holder = serviceHub.identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(text[2]))!!.toParty(serviceHub)
        //val holdingSTQuantity = text[3].toLong()

        println("evolvableTokenId : " + evolvableTokenId)
        println("currency :" + currency)

        // getting TokenHolder and his amount from file (actually from DB)
        // read input file
        inputfile = "C:\\intanalpocParm\\" + "Parm_ListDividend.txt"
        text = mutableListOf<String>()
        File(inputfile).useLines { lines -> text.addAll(lines) }

        val holder = serviceHub.identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(text[0].split(" : ")[0]))!!.toParty(serviceHub)
        val holdingSTQuantity = text[0].split(" : ")[1].toLong()


        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val uuid = UUID.fromString(evolvableTokenId)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(uuid))
        val tokenStateAndRef = serviceHub.vaultService.queryBy<EvolvableTokenType>(queryCriteria).states.single()
        val token = tokenStateAndRef.state.data.toPointer<EvolvableTokenType>()
        // Starting this flow with a new flow session.

        val evolvableTokenType: TestTokenType = tokenStateAndRef.state.data as TestTokenType

        val quantity = evolvableTokenType.quantity.toLong()
        val couponRate = evolvableTokenType.couponRate.split("%")[0].toDouble()

        val dividendTotalQuantity = quantity * couponRate / 100
        println("dividendTotalQuantity : " + dividendTotalQuantity)

        var paymentToken: TokenType = FiatCurrency.Companion.getInstance(currency)


        println("holder: $holder, holdingSTQuantity: $holdingSTQuantity")
        println("quantitiy: $quantity")
        println("couponRate : " + evolvableTokenType.couponRate)

        //20200128 update : we can calculate floating value for dividend
        var dividendRateParHolder = (holdingSTQuantity.toDouble() / quantity.toDouble()) * 100

        println("dividendRateParHolder : " + dividendRateParHolder)
        var dividendQuantityParHolder = (dividendTotalQuantity * dividendRateParHolder / 100).toLong()
        println("dividendQuantityParHolder : " + dividendQuantityParHolder)

        // checking sufficient amount for dividend
        val balance = serviceHub.vaultService.tokenBalance(token = paymentToken)
        val holdingPTQuantity = balance.quantity
        println("Payer holding Payment Token : " + holdingPTQuantity + " of " + balance.token.tokenIdentifier)
        println("Payer should dividend to STHolder( " + holder + " ) : " + dividendQuantityParHolder + " of " + balance.token.tokenIdentifier)

        if (dividendQuantityParHolder > holdingPTQuantity) {
            val errorMsgForInsufficientPT = "Payer do not hold sufficient payment token to dividend security token holders."
            println(errorMsgForInsufficientPT)
            throw IllegalArgumentException(errorMsgForInsufficientPT)
        }


        var moveTokensFlow = MoveFungibleTokens(PartyAndAmount( holder, dividendQuantityParHolder of paymentToken))

        var sx = subFlow(moveTokensFlow)
        //println("sx : " + sx)

        return sx
    }
}
