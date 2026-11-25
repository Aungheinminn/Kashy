package com.aungheinmin.kashy.data

enum class Category(val label: String, val emoji: String, val tint: Long, val type: EntryType) {
    FOOD("Food", "🍜", 0xFFFFE0B2, EntryType.OUT),
    TRANSPORT("Transport", "🚕", 0xFFBBDEFB, EntryType.OUT),
    SHOPPING("Shopping", "🛍️", 0xFFF8BBD0, EntryType.OUT),
    BILLS("Bills", "💡", 0xFFD1C4E9, EntryType.OUT),
    HEALTH("Health", "💊", 0xFFC8E6C9, EntryType.OUT),
    GIFTS("Gifts", "🎁", 0xFFFFF9C4, EntryType.OUT),
    STUDY("Study", "📚", 0xFFB2EBF2, EntryType.OUT),
    SALARY("Salary", "💼", 0xFFC8E6C9, EntryType.IN),
    SALES("Sales", "🏷️", 0xFFFFECB3, EntryType.IN),
    GIFT_RECEIVED("Gift received", "🎉", 0xFFF8BBD0, EntryType.IN);

    companion object {
        fun of(key: String?): Category? = key?.let { k -> entries.firstOrNull { it.name == k } }
        fun forType(type: EntryType): List<Category> = entries.filter { it.type == type }
    }
}
