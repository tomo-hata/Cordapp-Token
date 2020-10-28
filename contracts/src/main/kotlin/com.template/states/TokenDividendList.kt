package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.template.TestTokenTypeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party


internal typealias Serializable = java.io.Serializable

public data class TokenDividendHoldersList<out Party, out Long>(
        public val holder: Party,
        public val holdingAmount: Long
) : Serializable {
    public override fun toString(): String = "TokenDividendHoldersList data is ($holder, $holdingAmount)"
}

public fun <T> TokenDividendHoldersList<T, T>.toList(): List<T> = listOf(holder, holdingAmount)
