package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow
import com.r3.corda.lib.tokens.workflows.flows.evolvable.addUpdateEvolvableToken
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.rpc.*
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import com.template.TestTokenTypeContract
import com.template.states.TestTokenType
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
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
 *  Update couponRate into Security Token Type
 */
@StartableByRPC
class UpdateCouponPaymentStatus(
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // read input file
        var inputfile: String = "C:\\intanalpocParm\\" + "Parm_dividendTokenUsingMediator.txt"
        var text = mutableListOf<String>()
        File(inputfile).useLines { lines -> text.addAll(lines) }
        //text.forEachIndexed { i, line -> println("${i}: " + line) }

        val evolvableTokenId = text[0]
        val currency = text[1]
        val couponPaymentNum = text[2].toInt()

        println(evolvableTokenId)
        println(currency)
        println(couponPaymentNum)

        // read input file
        inputfile = "C:\\intanalpocParm\\" + "Parm_checkFinishDividend.txt"
        text = mutableListOf<String>()
        File(inputfile).useLines { lines -> text.addAll(lines) }
        val checkCouponPaymentNum = text[0].toInt()
        val checkFinishCouponPayment = text[1]

        // checking CouponPayment Number and witch or not finished
        if (couponPaymentNum != checkCouponPaymentNum) {
            val errorMsgForCouponPaymentNumMismatch = "Updating CouponPayment Status can not be done, because trying to update number mismatch."
            println(errorMsgForCouponPaymentNumMismatch)
            throw IllegalArgumentException(errorMsgForCouponPaymentNumMismatch)
        } else if (checkFinishCouponPayment != "done") {
            val errorMsgForCouponPaymentNotFinished = "Updating CouponPayment Status can not be done, because Coupon Payment do not finish."
            println(errorMsgForCouponPaymentNotFinished)
            throw IllegalArgumentException(errorMsgForCouponPaymentNotFinished)
        }

        val uuid = UUID.fromString(evolvableTokenId)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(uuid))
        val tokenStateAndRef = serviceHub.vaultService.queryBy<EvolvableTokenType>(queryCriteria).states.single()
        val token = tokenStateAndRef.state.data.toPointer<EvolvableTokenType>()

        //val input = tokenStateAndRef.state.data as TestTokenType
        val evolvableTokenType = tokenStateAndRef.state.data as TestTokenType

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // update couponPaymentStatus
        val currentCouponPaymentStatus = evolvableTokenType.couponPaymentStatus[couponPaymentNum - 1].split(":")[1].toString()
        println("currentCouponPaymentStatus : " + currentCouponPaymentStatus)

        val currentCouponPaymentNumber = evolvableTokenType.couponPaymentStatus[couponPaymentNum - 1].split(":")[0].toString()
        println("currentCouponPaymentNumber : " + currentCouponPaymentNumber)

        //val changeValueOfCPS = evolvableTokenType.couponPaymentStatus.set(couponPaymentNum - 1,currentCouponPaymentNumber + ":"  + "done") as MutableList<String>
        val changeValueOfCPS = evolvableTokenType.couponPaymentStatus.toMutableList() as MutableList<String>
        changeValueOfCPS.set(couponPaymentNum - 1,currentCouponPaymentNumber + ":done")
        println("changeValueOfCPS : " + changeValueOfCPS.toString())

        val output = evolvableTokenType.updateCouponPaymentStatus(changeValueOfCPS)

        val updateTx = subFlow(UpdateEvolvableToken(tokenStateAndRef,output))
        println("update couponPaymentStatus" + updateTx.toString())

        return updateTx
    }
}
