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
class UpdateCouponRate(
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // read input file
        val inputfile: String = "C:\\intanalpocParm\\" + "Parm_updateCR.txt"
        val text = mutableListOf<String>()
        File(inputfile).useLines { lines -> text.addAll(lines) }
        //text.forEachIndexed { i, line -> println("${i}: " + line) }

        val evolvableTokenId = text[0]
        val newCouponRate = text[1]

        val uuid = UUID.fromString(evolvableTokenId)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(uuid))
        val tokenStateAndRef = serviceHub.vaultService.queryBy<EvolvableTokenType>(queryCriteria).states.single()
        val token = tokenStateAndRef.state.data.toPointer<EvolvableTokenType>()

        val input = tokenStateAndRef.state.data as TestTokenType
        val output = input.updateCouponRate(newCouponRate)

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        /**
        val txBuilder = TransactionBuilder(notary)

        val signer = txBuilder.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub)
        val updateCouponRateCommand = Command(TestTokenTypeContract.Commands.UpdateCouponRate(),signer)

        txBuilder.withItems(tokenStateAndRef,StateAndContract(output,TestTokenTypeContract.TESTTOKEN_CONTRACT_ID),updateCouponRateCommand)
        txBuilder.verify(serviceHub)
        //val sx = serviceHub.signInitialTransaction(txBuilder)
        */

        val sx = subFlow(UpdateEvolvableToken(tokenStateAndRef,output))


        return sx
    }
}
