package com.printerkmp.component


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.printerkmp.data.ReceiptData
import com.printerkmp.data.mockData



data class ArabicReceiptData(
    val storeName: String,
    val address: String,
    val receiptNumber: String,
    val cashierName: String,
    val gregorianDate: String,
    val hijriDate: String,
    val time: String,
    val items: List<ArabicReceiptItem>,
    val totalPieces: Double, // 8.892 in the image
    val subtotal: Double,
    val discount: Double,
    val netTotal: Double,
)

data class ArabicReceiptItem(
    val id: Int,
    val name: String,
    val qty: Double,
    val pack: Int,
    val price: Double,
    val total: Double
)


val sampleDataEn = ArabicReceiptData(
    storeName = "Hassan Al-Hawat Sons",
    address = "Al Khums - Leptis - Hospital Street near the Coastal Road\nBehind Khadija Al-Kubra School",
    receiptNumber = "370007835",
    cashierName = "Mohamed Othman",
    gregorianDate = "2026.03.28",
    hijriDate = "09-Shawwal-1447 AH",
    time = "03:56:45 pm",
    totalPieces = 8.892,
    subtotal = 104.376,
    discount = 0.126,
    netTotal = 520.250,
    items = listOf(
        ArabicReceiptItem(1, "Dijlah Water 500ml", 3.0, 12, 5.5, 16.500),
        ArabicReceiptItem(2, "Lara Sliced Black Olives 720g", 1.0, 1, 13.0, 13.000),
        ArabicReceiptItem(3, "Al-Zahrat Milk 170ml", 1.0, 1, 3.25, 3.250),
        ArabicReceiptItem(4, "Iranian Raisins", 0.421, 1, 49.0, 20.629),
        ArabicReceiptItem(5, "Raw American Almonds", 0.231, 1, 87.0, 20.097)
    )
)


val sampleDataAr = ArabicReceiptData(
    storeName = "أبناء حسن الحوات",
    address = "الخمس - لبدة - شارع المستشفى بالقرب من الطريق\nالساحلي - خلف مدرسة خديجة الكبرى",
    receiptNumber = "370007835",
    cashierName = "محمد عثمان",
    gregorianDate = "2026.03.28",
    hijriDate = "09-شوال-1447 هجري",
    time = "03:56:45 pm",
    totalPieces = 8.892,
    subtotal = 520.250,
    discount = 0.126,
    netTotal = 520.250,
    items = listOf(
        ArabicReceiptItem(1, "مياه دجلة 500 مللي", 3.0, 12, 5.5, 16.500),
        ArabicReceiptItem(2, "زيتون شرائح اسود لارا 720 ج", 1.0, 1, 13.0, 13.000),
        ArabicReceiptItem(3, "حليب الزهرات 170 مللي", 1.0, 1, 3.25, 3.250),
        ArabicReceiptItem(4, "زبيب ايراني", 0.421, 1, 49.0, 20.629),
        ArabicReceiptItem(5, "لوز ني امريكي", 0.231, 1, 87.0, 20.097),
        ArabicReceiptItem(2, "زيتون شرائح اسود لارا 720 ج", 1.0, 1, 13.0, 13.000),
        ArabicReceiptItem(3, "حليب الزهرات 170 مللي", 1.0, 1, 3.25, 3.250),
        ArabicReceiptItem(4, "زبيب ايراني", 0.421, 1, 49.0, 20.629),
        ArabicReceiptItem(5, "لوز ني امريكي", 0.231, 1, 87.0, 20.097),
        )
)



@Composable
fun Receipt(data: ArabicReceiptData, language: String = "en") {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl, // Forces Right-to-Left
        LocalContentColor provides Color.Black,
        LocalTextStyle provides LocalTextStyle.current.copy(color = Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text side (Right in RTL)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = data.storeName,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Logo side Placeholder (Left in RTL)
//                Box(
//                    modifier = Modifier
//                        .size(60.dp)
//                        .border(2.dp, Color.Black),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("LOGO", fontWeight = FontWeight.Bold)
//                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.address,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- GREETING BANNER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "نتشرف بزياراتكم",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- INFO BOX ---
            Column(modifier = Modifier.border(2.dp, Color.Black).padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "إعداد: ${data.cashierName}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = data.receiptNumber, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = data.hijriDate, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = data.time, fontSize = 16.sp, fontWeight = FontWeight.Bold)
               }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "مبيعات نقدية", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = data.gregorianDate, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // --- CHECK NUMBER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black)
                    .padding(4.dp)
            ) {
                Text(text = "رقم التفويض/الصك: 1", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            // --- ITEMS TABLE ---
            // Table Header
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                TableCell("م", weight = 0.08f, isHeader = true)
                TableCell("البيان", weight = 0.35f, isHeader = true)
                TableCell("الكمية", weight = 0.14f, isHeader = true)
                TableCell("العبوة", weight = 0.12f, isHeader = true)
                TableCell("السعر", weight = 0.12f, isHeader = true)
                TableCell("القيمة", weight = 0.18f, isHeader = true)
            }

            // Table Body
            data.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth()
                    .height(IntrinsicSize.Min)) {
                    TableCell(item.id.toString(), weight = 0.08f)
                    TableCell(item.name, weight = 0.35f, alignRight = true)
                    TableCell(item.qty.toString().removeSuffix(".0"), weight = 0.14f)
                    TableCell(item.pack.toString(), weight = 0.12f)
                    TableCell(item.price.toString(), weight = 0.12f)
                    TableCell(String.format("%.3f", item.total), weight = 0.18f)
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Row(modifier = Modifier
                .fillMaxWidth()
                .height(35.dp),
                horizontalArrangement = Arrangement.Start) {
                Box(
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = data.totalPieces.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Box(
                    modifier = Modifier.border(1.dp, Color.Black).padding(horizontal = 16.dp, vertical = 2.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center) {
                    Text(text = "عدد القطع بالفاتورة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- TOTALS GRID ---
            Row(modifier = Modifier.fillMaxWidth()) {
                // Right side text (Cashier & Date)
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = data.gregorianDate, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = data.time, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "أعدها لك ${data.cashierName}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // Left side totals box
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        TableCell("الإجمالي", weight = 0.4f, isHeader = true)
                        TableCell(String.format("%.3f", data.subtotal), weight = 0.6f)
                    }
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        TableCell("خصم", weight = 0.4f, isHeader = true)
                        TableCell(String.format("%.3f", data.discount), weight = 0.6f)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(Color.LightGray.copy(alpha = 0.3f)) // Optional shading
                    ) {
                        TableCell("الصافي", weight = 0.4f, isHeader = true)
                        TableCell(String.format("%.3f", data.netTotal), weight = 0.6f, isHeader = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- FOOTER ---
            Text(
                text = "المواد الغذائية لا ترد ولا تستبدل",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "أبناء حسن الحوات للبقوليات والمكسرات والتوابل والبن بأنواعه",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Cut spacing
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- HELPER COMPOSABLE FOR TABLE BORDERS ---
@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    alignRight: Boolean = false
) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.Black)
            .weight(weight)
            .fillMaxHeight()
            .padding(4.dp),
        contentAlignment = if (alignRight) Alignment.CenterStart else Alignment.Center // Start is Right in RTL
    ) {
        Text(
            text = text,
            fontSize = if (isHeader) 14.sp else 16.sp, // Match the image hierarchy
            fontWeight = if (isHeader) FontWeight.ExtraBold else FontWeight.Normal,
            textAlign = if (alignRight) TextAlign.Start else TextAlign.Center
        )
    }
}


///////////////////////////////



//@Composable
//fun Receipt(data: ReceiptData) {
//    CompositionLocalProvider(
//        LocalContentColor provides Color.Black,
//        LocalTextStyle provides LocalTextStyle.current.copy(
//            fontFamily = FontFamily.Default,
//            color = Color.Black
//        )
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(Color.White) // Pure white background
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//
//            // --- HEADER ---
//            Text(
//                text = data.storeName,
//                fontSize = 28.sp, // Increased from 26.sp to maintain hierarchy
//                fontWeight = FontWeight.Bold,
//                textAlign = TextAlign.Center
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(text = data.storeAddress, fontSize = 16.sp, textAlign = TextAlign.Center) // Increased from 14.sp
//            Text(text = "Tel: ${data.storePhone}", fontSize = 16.sp, textAlign = TextAlign.Center) // Increased from 14.sp
//
//            Spacer(modifier = Modifier.height(12.dp))
//            ReceiptDashedDivider()
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // --- META DATA ---
//            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//                Text(text = "Order: #${data.orderNumber}", fontSize = 16.sp) // Increased from 14.sp
//                Text(text = data.date, fontSize = 16.sp) // Increased from 14.sp
//            }
//            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//                Text(text = "Cashier: ${data.cashierName}", fontSize = 16.sp) // Increased from 14.sp
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//            ReceiptDashedDivider()
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // --- TABLE HEADERS ---
//            Row(modifier = Modifier.fillMaxWidth()) {
//                Text(text = "Qty", modifier = Modifier.weight(0.15f), fontSize = 16.sp, fontWeight = FontWeight.Bold) // Increased from 14.sp
//                Text(text = "Item", modifier = Modifier.weight(0.55f), fontSize = 16.sp, fontWeight = FontWeight.Bold) // Increased from 14.sp
//                Text(text = "Total", modifier = Modifier.weight(0.30f), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End) // Increased from 14.sp
//            }
//
//            Spacer(modifier = Modifier.height(4.dp))
//
//            // --- ITEMS LOOP ---
//            data.items.forEach { item ->
//                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
//                    Text(text = "${item.quantity}x", modifier = Modifier.weight(0.15f), fontSize = 16.sp) // Increased from 14.sp
//
//                    Column(modifier = Modifier.weight(0.55f)) {
//                        Text(text = item.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) // Increased from 14.sp
//                        // Render modifiers if they exist (e.g., "- Extra Cheese")
//                        item.modifiers.forEach { mod ->
//                            Text(text = "- $mod", fontSize = 14.sp) // Increased from 12.sp
//                        }
//                    }
//
//                    Text(
//                        text = "$${"%.2f".format(item.totalPrice)}",
//                        modifier = Modifier.weight(0.30f),
//                        fontSize = 16.sp, // Increased from 14.sp
//                        textAlign = TextAlign.End
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//            ReceiptDashedDivider()
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // --- TOTALS ---
//            ReceiptTotalRow("Subtotal", data.subtotal, isBold = false, fontSize = 16.sp)
//            if (data.discount > 0) {
//                ReceiptTotalRow("Discount", -data.discount, isBold = false, fontSize = 16.sp)
//            }
//            ReceiptTotalRow("Tax", data.tax, isBold = false, fontSize = 16.sp)
//
//            Spacer(modifier = Modifier.height(4.dp))
//
//            ReceiptTotalRow("GRAND TOTAL", data.total, isBold = true, fontSize = 24.sp) // Increased from 22.sp
//
//            Spacer(modifier = Modifier.height(8.dp))
//            ReceiptDashedDivider()
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // --- PAYMENT INFO ---
//            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//                Text(text = "Paid via:", fontSize = 16.sp) // Increased from 14.sp
//                Text(text = data.paymentMethod, fontSize = 16.sp, fontWeight = FontWeight.Bold) // Increased from 14.sp
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // --- FOOTER & BARCODE PLACEHOLDER ---
//            Text(text = "Thank you for your business!", fontSize = 18.sp, fontWeight = FontWeight.Bold) // Increased from 16.sp
//            Text(text = "Please retain this receipt for returns.", fontSize = 14.sp) // Increased from 12.sp
//
//            Spacer(modifier = Modifier.height(30.dp))
//
//            Text(text = data.orderNumber, fontSize = 14.sp) // Increased from 12.sp
//
//            // Give the printer some space before tearing
//            Spacer(modifier = Modifier.height(32.dp))
//        }
//    }
//}
//
//// Ensure the helper function matches the new baseline size!
//@Composable
//private fun ReceiptTotalRow(label: String, amount: Double, isBold: Boolean, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp) { // Default to 16.sp now
//    Row(
//        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(
//            text = label,
//            fontSize = fontSize,
//            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
//        )
//        Text(
//            text = "$${"%.2f".format(amount)}",
//            fontSize = fontSize,
//            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
//        )
//    }
//}
//

@Preview
@Composable
private fun ReceiptPreview(){
//    Receipt(data = mockData)
    Receipt(data = sampleDataEn, language = "en")
}


@Preview
@Composable
private fun ReceiptArPreview(){
//    Receipt(data = mockData)
    Receipt(data = sampleDataAr, language = "ar")
}



