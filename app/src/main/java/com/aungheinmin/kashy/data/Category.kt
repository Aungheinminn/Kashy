package com.aungheinmin.kashy.data

import androidx.annotation.StringRes
import com.aungheinmin.kashy.R

enum class Category(@StringRes val label: Int, val emoji: String, val tint: Long, val type: EntryType) {
    FOOD(R.string.category_food, "🍜", 0xFFFFE0B2, EntryType.OUT),
    TRANSPORT(R.string.category_transport, "🚕", 0xFFBBDEFB, EntryType.OUT),
    SHOPPING(R.string.category_shopping, "🛍️", 0xFFF8BBD0, EntryType.OUT),
    BILLS(R.string.category_bills, "💡", 0xFFD1C4E9, EntryType.OUT),
    HEALTH(R.string.category_health, "💊", 0xFFC8E6C9, EntryType.OUT),
    GIFTS(R.string.category_gifts, "🎁", 0xFFFFF9C4, EntryType.OUT),
    STUDY(R.string.category_study, "📚", 0xFFB2EBF2, EntryType.OUT),
    SALARY(R.string.category_salary, "💼", 0xFFC8E6C9, EntryType.IN),
    SALES(R.string.category_sales, "🏷️", 0xFFFFECB3, EntryType.IN),
    GIFT_RECEIVED(R.string.category_gift_received, "🎉", 0xFFF8BBD0, EntryType.IN);

    companion object {
        fun of(key: String?): Category? = key?.let { k -> entries.firstOrNull { it.name == k } }
        fun forType(type: EntryType): List<Category> = entries.filter { it.type == type }
    }
}
