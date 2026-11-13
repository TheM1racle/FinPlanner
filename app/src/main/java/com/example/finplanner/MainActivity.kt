package com.example.finplanner

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: PlannerViewModel by viewModels {
        PlannerViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelDynamicTheme {
                FinPlannerApp(vm = viewModel)
            }
        }
    }
}

@Composable
fun PixelDynamicTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinPlannerApp(vm: PlannerViewModel) {
    val state by vm.state.collectAsState()
    var currentTab by remember { mutableIntStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val view = LocalView.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val titleText = when (currentTab) {
                        0 -> "Калькулятор счета"
                        1 -> "Мой портфель (${state.accounts.size})"
                        else -> "Календарь выплат"
                    }
                    Text(text = titleText, fontWeight = FontWeight.Bold)
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        currentTab = 0
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Расчет") },
                    label = { Text("Расчет") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        currentTab = 1
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Счета") },
                    label = { Text("Счета") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        currentTab = 2
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = "Таблица") },
                    label = { Text("Таблица") }
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        showSaveDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Добавить") },
                    text = { Text("В портфель") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> CalculatorScreen(state = state, vm = vm)
                1 -> PortfolioScreen(
                    accounts = state.accounts,
                    onOpenInCalc = { acc ->
                        vm.loadAccountIntoCalculator(acc)
                        currentTab = 0
                    },
                    onDelete = { id -> vm.deleteAccount(id) }
                )
                2 -> ScheduleScreen(state = state)
            }
        }
    }

    if (showSaveDialog) {
        var accName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранить счет") },
            text = {
                OutlinedTextField(
                    value = accName,
                    onValueChange = { accName = it },
                    label = { Text("Название (напр. Т-Банк / Сейф)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        vm.saveCurrentAsAccount(accName)
                        showSaveDialog = false
                        currentTab = 1
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun CalculatorScreen(state: PlannerUiState, vm: PlannerViewModel) {
    val view = LocalView.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PillSelector(
                options = CalcMode.entries.map { it.title },
                selectedIndex = CalcMode.entries.indexOf(state.mode),
                onSelect = { index ->
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    vm.updateParams(mode = CalcMode.entries[index])
                }
            )
        }

        item {
            SummaryHeroCard(state = state)
        }

        item {
            InteractiveGrowthChart(points = state.result.schedule)
        }

        item {
            ParamSliderCard(
                title = "Стартовая сумма",
                value = state.initialAmount.toFloat(),
                valueText = formatRub(state.initialAmount),
                isDecimal = false,
                range = 0f..5_000_000f,
                step = 10_000f,
                onValueChange = { vm.updateParams(initialAmount = it.toDouble()) }
            )
        }

        item {
            if (state.mode == CalcMode.GROWTH) {
                ParamSliderCard(
                    title = "Пополнение в месяц",
                    value = state.monthlyDeposit.toFloat(),
                    valueText = formatRub(state.monthlyDeposit),
                    isDecimal = false,
                    range = 0f..500_000f,
                    step = 5_000f,
                    onValueChange = { vm.updateParams(monthlyDeposit = it.toDouble()) }
                )
            } else {
                ParamSliderCard(
                    title = "Желаемая цель",
                    value = state.targetAmount.toFloat(),
                    valueText = formatRub(state.targetAmount),
                    isDecimal = false,
                    range = 50_000f..15_000_000f,
                    step = 50_000f,
                    onValueChange = { vm.updateParams(targetAmount = it.toDouble()) }
                )
            }
        }

        item {
            ParamSliderCard(
                title = "Срок накопления",
                value = state.months.toFloat(),
                valueText = "${state.months} мес.",
                isDecimal = false,
                range = 1f..60f,
                step = 1f,
                onValueChange = { vm.updateParams(months = it.roundToInt().coerceAtLeast(1)) }
            )
        }

        item {
            ParamSliderCard(
                title = "Базовая ставка банка",
                value = state.baseRate.toFloat(),
                valueText = String.format(Locale.US, "%.1f %%", state.baseRate),
                isDecimal = true,
                range = 1f..30f,
                step = 0.5f,
                onValueChange = { vm.updateParams(baseRate = (it * 2f).roundToInt() / 2.0) }
            )
        }

        item {
            BankRulesCard(state = state, vm = vm)
        }
    }
}

@Composable
fun BankRulesCard(state: PlannerUiState, vm: PlannerViewModel) {
    val view = LocalView.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Приветственная промо-ставка",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Повышенный % в первые месяцы",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.hasPromo,
                    onCheckedChange = { enabled ->
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        vm.updateParams(hasPromo = enabled)
                    }
                )
            }

            AnimatedVisibility(visible = state.hasPromo) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = String.format(Locale.US, "Промо-ставка: %.1f %%", state.promoRate),
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = state.promoRate.toFloat().coerceIn(5f, 30f),
                        onValueChange = { raw ->
                            val snapped = (raw * 2f).roundToInt() / 2.0
                            if (snapped != state.promoRate) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                vm.updateParams(promoRate = snapped)
                            }
                        },
                        valueRange = 5f..30f
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Длительность промо: ${state.promoMonths} мес.",
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = state.promoMonths.toFloat().coerceIn(1f, 12f),
                        onValueChange = { raw ->
                            val m = raw.roundToInt().coerceIn(1, 12)
                            if (m != state.promoMonths) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                vm.updateParams(promoMonths = m)
                            }
                        },
                        valueRange = 1f..12f,
                        steps = 10
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "Правило начисления процентов",
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            PillSelector(
                options = BalanceType.entries.map { it.title },
                selectedIndex = BalanceType.entries.indexOf(state.balanceType),
                onSelect = { index ->
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    vm.updateParams(balanceType = BalanceType.entries[index])
                }
            )
        }
    }
}

@Composable
fun PillSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bgColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            }
            val contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SummaryHeroCard(state: PlannerUiState) {
    val profitColor = Color(0xFF10B981)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (state.mode == CalcMode.GROWTH) {
                Text(
                    text = "Итоговый капитал",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = formatRub(state.result.finalBalance),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = "Нужно откладывать в месяц",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = formatRub(state.result.requiredMonthlyDeposit),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Свои деньги",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                    Text(
                        text = formatRub(state.result.totalInvested),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Чистый доход (%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "+${formatRub(state.result.totalInterest)}",
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                }
            }
        }
    }
}

@Composable
fun ParamSliderCard(
    title: String,
    value: Float,
    valueText: String,
    isDecimal: Boolean,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit
) {
    val view = LocalView.current
    var showExactDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.clickable { showExactDialog = true }
                ) {
                    Text(
                        text = valueText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Slider(
                value = value.coerceIn(range.start, range.endInclusive),
                onValueChange = { raw ->
                    val snapped = (raw / step).roundToInt() * step
                    if (snapped != value) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onValueChange(snapped)
                    }
                },
                valueRange = range
            )
        }
    }

    if (showExactDialog) {
        val initialText = if (isDecimal) {
            String.format(Locale.US, "%.1f", value)
        } else {
            value.roundToInt().toString()
        }
        var textInput by remember { mutableStateOf(initialText) }

        AlertDialog(
            onDismissRequest = { showExactDialog = false },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = textInput.replace(',', '.').toFloatOrNull()
                        if (parsed != null && parsed >= 0f) {
                            onValueChange(parsed)
                        }
                        showExactDialog = false
                    }
                ) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun PortfolioScreen(
    accounts: List<AccountPlan>,
    onOpenInCalc: (AccountPlan) -> Unit,
    onDelete: (String) -> Unit
) {
    val summaries = remember(accounts) {
        accounts.map { acc ->
            val calc = FinancialEngine.calculate(
                mode = CalcMode.GROWTH,
                initial = acc.initialAmount,
                monthly = acc.monthlyDeposit,
                target = acc.targetAmount,
                months = acc.months,
                baseRate = acc.baseRate,
                hasPromo = acc.hasPromo,
                promoRate = acc.promoRate,
                promoMonths = acc.promoMonths,
                balanceType = acc.balanceType
            )
            acc to calc
        }
    }

    val totalCurrentCapital = accounts.sumOf { it.initialAmount }
    val totalProjectedCapital = summaries.sumOf { it.second.finalBalance.toDouble() }
    val totalFirstMonthProfit = summaries.sumOf {
        it.second.schedule.getOrNull(1)?.monthlyInterest?.toDouble() ?: 0.0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Сейчас на всех счетах",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = formatRub(totalCurrentCapital),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Доход за ближайший месяц: +${formatRub(totalFirstMonthProfit)}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Прогноз к концу сроков: ${formatRub(totalProjectedCapital)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (summaries.isEmpty()) {
            item {
                Text(
                    text = "Портфель пуст. Настрой параметры во вкладке «Расчет» и нажми «В портфель».",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(items = summaries, key = { it.first.id }) { (acc, calc) ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenInCalc(acc) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = acc.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(acc.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }

                    val activeRateLabel = if (acc.hasPromo) {
                        "Промо ${acc.promoRate}% (${acc.promoMonths} мес.) -> ${acc.baseRate}%"
                    } else {
                        "Ставка ${acc.baseRate}%"
                    }

                    Text(
                        text = "$activeRateLabel • ${acc.balanceType.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val progress = (acc.initialAmount / acc.targetAmount.coerceAtLeast(1.0))
                        .toFloat()
                        .coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Старт: ${formatRub(acc.initialAmount)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Итог (${acc.months} мес.): ${formatRub(calc.finalBalance)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleScreen(state: PlannerUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = state.result.schedule.drop(1), key = { it.monthIndex }) { pt ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Месяц ${pt.monthIndex} (${pt.yearMonth})",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ставка ${pt.appliedRate}% • Свои: ${formatRub(pt.totalInvested)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatRub(pt.endBalance),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+${formatRub(pt.monthlyInterest)} %",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}
