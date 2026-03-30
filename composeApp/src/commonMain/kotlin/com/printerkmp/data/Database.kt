package com.printerkmp.data



val mockData = ReceiptData(
    storeName = "KMP Coffee Roasters",
    storeAddress = "123 Multiplatform Way\nTech City, TC 90210",
    storePhone = "(555) 123-4567",
    orderNumber = "ORD-89432",
    date = "2026-03-26 12:35 PM",
    cashierName = "Alex",
    items = listOf(
        ReceiptItem("Venti Latte", 1, 4.50, 4.50, listOf("Oat Milk", "Extra Hot")),
        ReceiptItem("Blueberry Muffin", 2, 3.00, 6.00),
        ReceiptItem("Espresso Shot", 1, 1.50, 1.50)
    ),
    subtotal = 12.00,
    tax = 1.08,
    discount = 0.00,
    total = 13.08,
    paymentMethod = "Visa ending in 4242"
)