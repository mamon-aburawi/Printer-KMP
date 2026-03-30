package com.printerkmp.data



data class ReceiptData(
    val storeName: String,
    val storeAddress: String,
    val storePhone: String,
    val orderNumber: String,
    val date: String,
    val cashierName: String,
    val items: List<ReceiptItem>,
    val subtotal: Double,
    val tax: Double,
    val discount: Double,
    val total: Double,
    val paymentMethod: String
)

