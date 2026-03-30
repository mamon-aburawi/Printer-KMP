package com.printerkmp.data


data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val modifiers: List<String> = emptyList()
)