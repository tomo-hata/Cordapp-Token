package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.io.Files
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.template.states.TestTokenType
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

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
 * Create Security Token using CreateEvolvableTokens
 */
@StartableByRPC
class CreateTestToken(

        //val importantInformationThatMayChange: String
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // read input file
        val inputfile: String = "C:\\intanalpocParm\\" + "Parm_createToken.txt"

        val text = mutableListOf<String>()
        File(inputfile).useLines { lines -> text.addAll(lines) }
        //text.forEachIndexed { i, line -> println("${i}: " + line) }

        val securityTokenName = text[0]
        val owner = text[1]
        val quantity = text[2]
        val issuer = text[3]
        val issueDate = text[4]
        val redemptionDate = text[5]
        val couponMonth = text[6]
        val couponDate = text[7]
        val couponRate = text[8]
        val bondCustodian = text[9]
        val couponPaymentTotalNumber = text[10].toInt()

        val couponPaymentStatus= mutableListOf<String>()
        for (i  in 0..couponPaymentTotalNumber - 1) {
            couponPaymentStatus.add((i + 1).toString() + ":not yet")
            println(couponPaymentStatus[i])
        }

        println("couponPaymentStatus : " + couponPaymentStatus.toString())


        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val evolvableTokenType = TestTokenType(
                securityTokenName,
                owner,
                quantity,
                issuer,
                issueDate,
                redemptionDate,
                couponMonth,
                couponDate,
                couponRate,
                bondCustodian,
                couponPaymentStatus,
                ourIdentity,
                linearId = UniqueIdentifier()
        )
        val transactionState = TransactionState(evolvableTokenType, notary = notary)
        val st = subFlow(CreateEvolvableTokens(transactionState))

        val evolvableTokenId = evolvableTokenType.linearId
        println("evolvableTokenId : " + evolvableTokenId)

        return st
    }

}
