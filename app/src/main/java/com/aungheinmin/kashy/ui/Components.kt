package com.aungheinmin.kashy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aungheinmin.kashy.data.Category
import com.aungheinmin.kashy.data.Currency
import com.aungheinmin.kashy.data.EntryType
import com.aungheinmin.kashy.ui.theme.LocalCashColors

val LocalShortAmounts = staticCompositionLocalOf { true }

/** Shows [short] when short amounts are on; tapping it reveals [full]. */
@Composable
fun AmountText(
    full: String,
    short: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
) {
    val shortened = LocalShortAmounts.current && short != full
    var expanded by remember(full) { mutableStateOf(false) }
    Text(
        if (shortened && !expanded) short else full,
        modifier = if (shortened) modifier.clickable { expanded = !expanded } else modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize,
        maxLines = 1,
    )
}

@Composable
fun categoryBackground(category: Category): Color {
    val tint = Color(category.tint)
    // The pastel tints glare on a dark surface, so mute them there.
    return if (LocalCashColors.current.isDark) {
        tint.copy(alpha = 0.22f).compositeOver(MaterialTheme.colorScheme.surface)
    } else {
        tint
    }
}

@Composable
fun EntryIcon(type: EntryType, category: Category?, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val cash = LocalCashColors.current
    if (category != null) {
        Box(
            modifier.size(size).background(categoryBackground(category), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(category.emoji, fontSize = (size.value * 0.5f).sp) }
    } else {
        val isIn = type == EntryType.IN
        Box(
            modifier.size(size).background(if (isIn) cash.cashInBg else cash.cashOutBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isIn) "↓" else "↑",
                color = if (isIn) cash.cashIn else cash.cashOut,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.5f).sp,
            )
        }
    }
}

@Composable
fun WalletIcon(emoji: String, currency: Currency, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier.size(size).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji.isNotEmpty()) {
            Text(emoji, fontSize = (size.value * 0.5f).sp)
        } else {
            val sym = currency.symbol()
            Text(
                sym,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * if (sym.length > 1) 0.28f else 0.42f).sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {},
            ) { Text(label(option), maxLines = 1) }
        }
    }
}
