package barcode


enum class Barcode2DSize(val dotSize: Byte) {
    SMALL(4),
    MEDIUM(6),
    LARGE(8),
    EXTRA_LARGE(10)
}