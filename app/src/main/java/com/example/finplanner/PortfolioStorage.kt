package com.example.finplanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

data class PlannerUiState(
    val accounts: List<AccountPlan> = emptyList(),
    val mode: CalcMode = CalcMode.GROWTH,
    val initialAmount: Double = 100_000.0,
    val monthlyDeposit: Double = 25_000.0,
    val targetAmount: Double = 1_000_000.0,
    val months: Int = 12,
    val baseRate: Double = 16.0,
    val hasPromo: Boolean = true,
    val promoRate: Double = 21.0,
    val promoMonths: Int = 2,
    val balanceType: BalanceType = BalanceType.DAILY,
    val result: CalculationResult = FinancialEngine.calculate(
        CalcMode.GROWTH, 100_000.0, 25_000.0, 1_000_000.0, 12, 16.0, true, 21.0, 2, BalanceType.DAILY
    )
)

class PlannerViewModel(context: Context) : ViewModel() {
    private val prefs = context.getSharedPreferences("fin_planner_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(PlannerUiState(accounts = loadAccounts()))
    val state: StateFlow<PlannerUiState> = _state.asStateFlow()

    init {
        recalculate()
    }

    private fun loadAccounts(): List<AccountPlan> {
        val raw = prefs.getString("saved_accounts", null) ?: return defaultSampleAccounts()
        return runCatching { json.decodeFromString<List<AccountPlan>>(raw) }
            .getOrDefault(defaultSampleAccounts())
    }

    private fun defaultSampleAccounts(): List<AccountPlan> = listOf(
        AccountPlan(
            id = UUID.randomUUID().toString(),
            name = "Подушка безопасности",
            initialAmount = 150_000.0,
            monthlyDeposit = 30_000.0,
            targetAmount = 600_000.0,
            months = 12,
            baseRate = 16.5,
            hasPromo = true,
            promoRate = 21.0,
            promoMonths = 2,
            balanceType = BalanceType.DAILY
        )
    )

    private fun persistAccounts(list: List<AccountPlan>) {
        prefs.edit().putString("saved_accounts", json.encodeToString(list)).apply()
    }

    fun updateParams(
        mode: CalcMode = _state.value.mode,
        initialAmount: Double = _state.value.initialAmount,
        monthlyDeposit: Double = _state.value.monthlyDeposit,
        targetAmount: Double = _state.value.targetAmount,
        months: Int = _state.value.months,
        baseRate: Double = _state.value.baseRate,
        hasPromo: Boolean = _state.value.hasPromo,
        promoRate: Double = _state.value.promoRate,
        promoMonths: Int = _state.value.promoMonths,
        balanceType: BalanceType = _state.value.balanceType
    ) {
        _state.update {
            it.copy(
                mode = mode,
                initialAmount = initialAmount,
                monthlyDeposit = monthlyDeposit,
                targetAmount = targetAmount,
                months = months,
                baseRate = baseRate,
                hasPromo = hasPromo,
                promoRate = promoRate,
                promoMonths = promoMonths,
                balanceType = balanceType
            )
        }
        recalculate()
    }

    private fun recalculate() {
        val s = _state.value
        val calc = FinancialEngine.calculate(
            mode = s.mode,
            initial = s.initialAmount,
            monthly = s.monthlyDeposit,
            target = s.targetAmount,
            months = s.months,
            baseRate = s.baseRate,
            hasPromo = s.hasPromo,
            promoRate = s.promoRate,
            promoMonths = s.promoMonths,
            balanceType = s.balanceType
        )
        _state.update { it.copy(result = calc) }
    }

    fun saveCurrentAsAccount(name: String) {
        val s = _state.value
        val effectiveMonthly = if (s.mode == CalcMode.GOAL) {
            s.result.requiredMonthlyDeposit.toDouble()
        } else {
            s.monthlyDeposit
        }
        val newAcc = AccountPlan(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Накопительный счет ${s.accounts.size + 1}" },
            initialAmount = s.initialAmount,
            monthlyDeposit = effectiveMonthly,
            targetAmount = if (s.mode == CalcMode.GOAL) s.targetAmount else s.result.finalBalance.toDouble(),
            months = s.months,
            baseRate = s.baseRate,
            hasPromo = s.hasPromo,
            promoRate = s.promoRate,
            promoMonths = s.promoMonths,
            balanceType = s.balanceType
        )
        val updated = listOf(newAcc) + s.accounts
        persistAccounts(updated)
        _state.update { it.copy(accounts = updated) }
    }

    fun loadAccountIntoCalculator(acc: AccountPlan) {
        updateParams(
            mode = CalcMode.GROWTH,
            initialAmount = acc.initialAmount,
            monthlyDeposit = acc.monthlyDeposit,
            targetAmount = acc.targetAmount,
            months = acc.months,
            baseRate = acc.baseRate,
            hasPromo = acc.hasPromo,
            promoRate = acc.promoRate,
            promoMonths = acc.promoMonths,
            balanceType = acc.balanceType
        )
    }

    fun deleteAccount(id: String) {
        val updated = _state.value.accounts.filterNot { it.id == id }
        persistAccounts(updated)
        _state.update { it.copy(accounts = updated) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlannerViewModel(context.applicationContext) as T
        }
    }
}
