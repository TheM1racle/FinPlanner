package com.example.finplanner

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

@Serializable
enum class BalanceType(val title: String) {
    DAILY("На ежедневный остаток"),
    MINIMUM("На минимальный остаток")
}

@Serializable
enum class CalcMode(val title: String) {
    GROWTH("Накопить максимум"),
    GOAL("К цели (расчет взноса)")
}

@Serializable
data class AccountPlan(
    val id: String,
    val name: String,
    val initialAmount: Double,
    val monthlyDeposit: Double,
    val targetAmount: Double,
    val months: Int,
    val baseRate: Double,
    val hasPromo: Boolean,
    val promoRate: Double,
    val promoMonths: Int,
    val balanceType: BalanceType
)

data class MonthPoint(
    val monthIndex: Int,
    val yearMonth: String,
    val totalInvested: BigDecimal,
    val monthlyInterest: BigDecimal,
    val totalInterest: BigDecimal,
    val endBalance: BigDecimal,
    val appliedRate: Double
)

data class CalculationResult(
    val finalBalance: BigDecimal,
    val totalInvested: BigDecimal,
    val totalInterest: BigDecimal,
    val requiredMonthlyDeposit: BigDecimal,
    val effectiveAnnualYield: Double,
    val schedule: List<MonthPoint>
)

object FinancialEngine {
    private val mc = MathContext(16, RoundingMode.HALF_EVEN)
    private val HUNDRED = BigDecimal("100")

    fun calculate(
        mode: CalcMode,
        initial: Double,
        monthly: Double,
        target: Double,
        months: Int,
        baseRate: Double,
        hasPromo: Boolean,
        promoRate: Double,
        promoMonths: Int,
        balanceType: BalanceType
    ): CalculationResult {
        if (mode == CalcMode.GOAL) {
            val requiredMonthly = solveForMonthlyDeposit(
                initial = initial,
                target = target,
                months = months,
                baseRate = baseRate,
                hasPromo = hasPromo,
                promoRate = promoRate,
                promoMonths = promoMonths,
                balanceType = balanceType
            )
            val res = simulate(
                initial = BigDecimal.valueOf(initial),
                monthly = requiredMonthly,
                months = months,
                baseRate = baseRate,
                hasPromo = hasPromo,
                promoRate = promoRate,
                promoMonths = promoMonths,
                balanceType = balanceType
            )
            return res.copy(requiredMonthlyDeposit = requiredMonthly.setScale(2, RoundingMode.HALF_UP))
        }

        return simulate(
            initial = BigDecimal.valueOf(initial),
            monthly = BigDecimal.valueOf(monthly),
            months = months,
            baseRate = baseRate,
            hasPromo = hasPromo,
            promoRate = promoRate,
            promoMonths = promoMonths,
            balanceType = balanceType
        )
    }

    private fun solveForMonthlyDeposit(
        initial: Double,
        target: Double,
        months: Int,
        baseRate: Double,
        hasPromo: Boolean,
        promoRate: Double,
        promoMonths: Int,
        balanceType: BalanceType
    ): BigDecimal {
        val targetBd = BigDecimal.valueOf(target)
        val zeroCheck = simulate(
            BigDecimal.valueOf(initial), BigDecimal.ZERO, months,
            baseRate, hasPromo, promoRate, promoMonths, balanceType
        )
        if (zeroCheck.finalBalance >= targetBd) return BigDecimal.ZERO

        var low = BigDecimal.ZERO
        var high = targetBd.max(BigDecimal("10000000"))

        repeat(45) {
            val mid = low.add(high).divide(BigDecimal("2"), mc)
            val sim = simulate(
                BigDecimal.valueOf(initial), mid, months,
                baseRate, hasPromo, promoRate, promoMonths, balanceType
            )
            if (sim.finalBalance < targetBd) {
                low = mid
            } else {
                high = mid
            }
        }
        return high.setScale(2, RoundingMode.CEILING)
    }

    private fun simulate(
        initial: BigDecimal,
        monthly: BigDecimal,
        months: Int,
        baseRate: Double,
        hasPromo: Boolean,
        promoRate: Double,
        promoMonths: Int,
        balanceType: BalanceType
    ): CalculationResult {
        var currentBalance = initial
        var totalInvested = initial
        var totalInterest = BigDecimal.ZERO
        val points = ArrayList<MonthPoint>(months + 1)

        val startDate = LocalDate.now()
        points.add(
            MonthPoint(
                monthIndex = 0,
                yearMonth = "Старт",
                totalInvested = initial.setScale(2, RoundingMode.HALF_UP),
                monthlyInterest = BigDecimal.ZERO,
                totalInterest = BigDecimal.ZERO,
                endBalance = initial.setScale(2, RoundingMode.HALF_UP),
                appliedRate = if (hasPromo && promoMonths > 0) promoRate else baseRate
            )
        )

        var cursorDate = startDate

        for (m in 1..months) {
            val activeRate = if (hasPromo && m <= promoMonths) promoRate else baseRate
            val rateBd = BigDecimal.valueOf(activeRate)
            val nextMonthDate = startDate.plusMonths(m.toLong())

            val startOfMonthBalance = currentBalance

            val baseForInterest = when (balanceType) {
                BalanceType.DAILY -> startOfMonthBalance.add(monthly)
                BalanceType.MINIMUM -> startOfMonthBalance
            }

            var monthInterest = BigDecimal.ZERO
            var d = cursorDate
            while (d.isBefore(nextMonthDate)) {
                val daysInYear = BigDecimal.valueOf(if (d.isLeapYear) 366L else 365L)
                val dailyInterest = baseForInterest
                    .multiply(rateBd, mc)
                    .divide(HUNDRED.multiply(daysInYear, mc), mc)
                monthInterest = monthInterest.add(dailyInterest, mc)
                d = d.plusDays(1)
            }

            monthInterest = monthInterest.setScale(2, RoundingMode.HALF_UP)
            currentBalance = currentBalance.add(monthly).add(monthInterest)
            totalInvested = totalInvested.add(monthly)
            totalInterest = totalInterest.add(monthInterest)

            val ym = YearMonth.from(nextMonthDate)
            val label = "${ym.monthValue.toString().padStart(2, '0')}.${ym.year % 100}"

            points.add(
                MonthPoint(
                    monthIndex = m,
                    yearMonth = label,
                    totalInvested = totalInvested.setScale(2, RoundingMode.HALF_UP),
                    monthlyInterest = monthInterest,
                    totalInterest = totalInterest.setScale(2, RoundingMode.HALF_UP),
                    endBalance = currentBalance.setScale(2, RoundingMode.HALF_UP),
                    appliedRate = activeRate
                )
            )
            cursorDate = nextMonthDate
        }

        val effectiveYield = if (totalInvested > BigDecimal.ZERO && months > 0) {
            totalInterest.toDouble() / totalInvested.toDouble() * (12.0 / months.toDouble()) * 100.0
        } else 0.0

        return CalculationResult(
            finalBalance = currentBalance.setScale(2, RoundingMode.HALF_UP),
            totalInvested = totalInvested.setScale(2, RoundingMode.HALF_UP),
            totalInterest = totalInterest.setScale(2, RoundingMode.HALF_UP),
            requiredMonthlyDeposit = monthly.setScale(2, RoundingMode.HALF_UP),
            effectiveAnnualYield = effectiveYield,
            schedule = points
        )
    }
}
