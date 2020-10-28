package com.template

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.template.states.TestTokenType
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */

class TestTokenTypeContract : EvolvableTokenContract(), Contract {

    /**
    // add 20200121 start
    companion object {
        @JvmStatic
        val TESTTOKEN_CONTRACT_ID = "com.template.TestTokenTypeContract"
    }
    // add 20200121 end
    */

    override fun additionalCreateChecks(tx: LedgerTransaction) = Unit
    override fun additionalUpdateChecks(tx: LedgerTransaction) = Unit

    /**
    // add 20200121 start
    interface Commands : CommandData {
        class UpdateCouponRate : TypeOnlyCommandData(), Commands
    }
    // add 20200121 end

    // add 20200121 start
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<TestTokenTypeContract.Commands>()
        when (command.value) {
            is Commands.UpdateCouponRate -> requireThat {
                // will add any controls
            }
        }
    }
    // add 20200121 end
    */

}
