package com.template.states

import com.google.common.io.ByteStreams.copy
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.template.TestTokenTypeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party


@BelongsToContract(TestTokenTypeContract::class)
class TestTokenType(
        val securityTokenName : String,
        val owner : String,
        val quantity : String,
        val issuer : String,
        val issueDate : String,
        val redemptionDate : String,
        val couponMonth : String,
        val couponDate : String,
        val couponRate : String,
        val bondCustodian : String,
        val couponPaymentStatus : MutableList<String>,
        val maintainer: Party,
        override val linearId: UniqueIdentifier,
        override val fractionDigits: Int = 0
) : EvolvableTokenType() {
    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }

    override val maintainers: List<Party> get() = listOf(maintainer)


    // add 20200121 start
    fun updateCouponRate(newCouponRate: String) = copy(couponRate = newCouponRate)
    fun updateCouponPaymentStatus(newCouponPaymentStatus: MutableList<String>) = copy(couponPaymentStatus = newCouponPaymentStatus)
    private fun copy(
            securityTokenName: String = this.securityTokenName,
            owner: String = this.owner,
            quantity: String = this.quantity,
            issuer: String = this.issuer,
            issueDate: String = this.issueDate,
            redemptionDate: String = this.redemptionDate,
            couponMonth: String = this.couponMonth,
            couponDate: String = this.couponDate,
            couponRate: String = this.couponRate,
            bondCustodian: String = this.bondCustodian,
            couponPaymentStatus: MutableList<String> = this.couponPaymentStatus,
            maintainer: Party = this.maintainer,
            linearId: UniqueIdentifier = this.linearId,
            fractionDigits: Int =  this.fractionDigits
    ) = TestTokenType(securityTokenName,
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
            maintainer,
            linearId,
            fractionDigits)
    // add 20200121 end
}
