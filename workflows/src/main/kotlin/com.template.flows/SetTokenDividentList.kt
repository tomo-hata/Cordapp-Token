package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.io.Files
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import com.template.states.TestTokenType
import com.template.states.TokenDividendHoldersList
import com.template.states.toList
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
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
 * Read text
 */
@StartableByRPC
class SetTokenDividentList(

        //val holder: Party,
        //val holdingAmount: Long
) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()


    @Suspendable
    override fun call(): String {

        // read input file
        val inputfile: String = "C:\\intanalpocParm\\" + "Parm_ListDividend.txt"

        val text = mutableListOf<String>()

        var holder:Party? = null
        var holdingAmount:Long? = null

        val dividendHoldersList = mutableListOf<Pair<Party, Long>>(
                //Pair(holder, holdingAmount) as Pair<Party, Long>
        )
        File(inputfile).useLines { lines -> text.addAll(lines) }
        text.forEachIndexed { i, line ->
            println(
                "${i}: " + line.split(" : ")[0] + " :: " + line.split(" : ")[1]
            )
            holder = serviceHub.identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(line.split(" : ")[0]))!!.toParty(serviceHub)
            holdingAmount = line.split(" : ")[1].toLong()

            dividendHoldersList.add(Pair(holder,holdingAmount)as Pair<Party,Long>)

        }

        var rst: String? = null

        for ((holder, holdingAmount) in dividendHoldersList) {
            println("holder: $holder, holdingAmount: $holdingAmount")
            rst = """${rst}holder: $holder, holdingAmount: $holdingAmount"""
        }

        return "rst : " + rst as String


        /**
        val setValue = TokenDividendHoldersList(holder,holdingAmount)

        val list = setValue.toList()

        val output: String? = null
        val listMax = list.size -1
        for(i in 0..listMax){
            println(list[i].toString())
            output.plus(" : " + list[i].toString())

        }

        return "listsize : " + list.size + "" + output
        */

    }

}
