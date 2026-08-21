package com.dwt.ledger.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * 金额：以「分」为单位的整数，避免浮点误差。
 * 领域层统一用 Money，数据库里存 cents(Long)。
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {
    operator fun plus(other: Money) = Money(cents + other.cents)
    operator fun minus(other: Money) = Money(cents - other.cents)
    operator fun unaryMinus() = Money(-cents)
    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    val isPositive: Boolean get() = cents > 0
    val isZero: Boolean get() = cents == 0L

    /** "1,234.56"（不带货币符号，符号由 UI 决定） */
    fun format(): String = FORMAT.format(BigDecimal.valueOf(cents, 2))

    companion object {
        val ZERO = Money(0)
        private val FORMAT = DecimalFormat("#,##0.00")

        /** 把用户输入的 "12.3" / "12" / "0.05" 解析成 Money；非法输入返回 null。最多两位小数，多余位四舍五入。 */
        fun parse(text: String): Money? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return null
            return try {
                val bd = BigDecimal(trimmed).setScale(2, RoundingMode.HALF_UP)
                Money(bd.movePointRight(2).longValueExact())
            } catch (e: NumberFormatException) {
                null
            } catch (e: ArithmeticException) {
                null
            }
        }

        fun ofYuan(yuan: Long) = Money(yuan * 100)
    }
}
