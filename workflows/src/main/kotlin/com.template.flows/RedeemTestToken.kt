package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import com.template.states.TestTokenType
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import nonapi.io.github.classgraph.types.ParseException
import java.io.File
import java.io.IOException
import java.lang.Object
import java.nio.*
import java.nio.file.Files.readAllLines
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import java.util.List
import java.util.UUID

/**
 *  Redeem Security Token using RedeemFungibleTokens flow
 */
@StartableByRPC
class RedeemTestToken(
        //val evolvableTokenId: String,
        //val quantity: Long,
        //val issuer: Party
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // read input file
        val inputfile: String = "C:\\intanalpocParm\\" + "Parm_redeemToken.txt"
        val text = mutableListOf<String>()
        File(inputfile).useLines { lines -> text.addAll(lines) }
        //text.forEachIndexed { i, line -> println("${i}: " + line) }

        val evolvableTokenId = text[0]
        val quantity = text[1].toLong()
        val issuer = serviceHub.identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(text[2]))!!.toParty(serviceHub)
        val exeRedemptionDateStr = text[3]

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val uuid = UUID.fromString(evolvableTokenId)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(uuid))
        val tokenStateAndRef = serviceHub.vaultService.queryBy<EvolvableTokenType>(queryCriteria).states.single()
        val token = tokenStateAndRef.state.data.toPointer<EvolvableTokenType>()
        // Starting this flow with a new flow session.

        val evolvableTokenType: TestTokenType = tokenStateAndRef.state.data as TestTokenType

        // add 20200128 : convert String to Date (redemptionDate)
        println(evolvableTokenType.redemptionDate)

        val redemptionDate = SimpleDateFormat("yyyy/MM/dd").let{
            val parsed = it.parse(evolvableTokenType.redemptionDate)
            it.format(parsed)
        }

        println("redemptionDate : " + redemptionDate)

        val exeRedemptionDate = SimpleDateFormat("yyyy/MM/dd").let{
            val parsed = it.parse(exeRedemptionDateStr)
            it.format(parsed)
        }

        println("exeRedemptionDate : " + exeRedemptionDate)

        if (redemptionDate > exeRedemptionDate) {
            val errorMsgForRedemptionDateNotComming = "Redeem can not be done, because RedemptionDate do not come."
            println(errorMsgForRedemptionDateNotComming)
            throw IllegalArgumentException(errorMsgForRedemptionDateNotComming)
        }


        //fungibletoken
        val redeemTokensFlow = RedeemFungibleTokens(quantity of token,issuer)
        val sx = subFlow(redeemTokensFlow)

        //Updating DistributionList
        subFlow(UpdateDistributionListFlow(sx))

        return sx
    }

}
