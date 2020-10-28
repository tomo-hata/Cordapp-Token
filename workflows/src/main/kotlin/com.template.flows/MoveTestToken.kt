package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import com.template.states.TestTokenType
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
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
 *  Move Security Token using MoveFungibleTokens flow
 */
@StartableByRPC
class MoveTestToken(
        //val evolvableTokenId: String,
        //val quantity: Long,
        //val recipient: Party
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // read input file
        val inputfile: String = "C:\\intanalpocParm\\" + "Parm_moveToken.txt"
        val text = mutableListOf<String>()
        File(inputfile).useLines { lines -> text.addAll(lines) }
        //text.forEachIndexed { i, line -> println("${i}: " + line) }

        val evolvableTokenId = text[0]
        val quantity = text[1].toLong()
        val recipient = serviceHub.identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(text[2]))!!.toParty(serviceHub)

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val uuid = UUID.fromString(evolvableTokenId)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(uuid))
        val tokenStateAndRef = serviceHub.vaultService.queryBy<EvolvableTokenType>(queryCriteria).states.single()
        val token = tokenStateAndRef.state.data.toPointer<EvolvableTokenType>()
        // Starting this flow with a new flow session.

        //fungibletoken
        val moveTokensFlow = MoveFungibleTokens(listOf(PartyAndAmount( recipient, quantity of token)))
        val sx = subFlow(moveTokensFlow)

        //Updating DistributionList
        subFlow(UpdateDistributionListFlow(sx))

        return sx
    }
}
