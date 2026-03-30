package barcode

enum class QrErrorCorrection(val byteValue: Byte) {
    LEVEL_L(0x30), // Recovers ~7% of damaged data (Smallest size)
    LEVEL_M(0x31), // Recovers ~15% of damaged data (Standard)
    LEVEL_Q(0x32), // Recovers ~25% of damaged data (High reliability)
    LEVEL_H(0x33)  // Recovers ~30% of damaged data (Largest size, max reliability)
}