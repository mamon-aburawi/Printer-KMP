package barcode

enum class Barcode1D(val mByte: Byte) {
    /**
     * UPC-A (Retail)
     * Limitation: NUMBERS ONLY.
     * Length: Must be exactly 11 or 12 digits.
     */
    UPC_A(65),

    /**
     * EAN-13 (Global Retail Standard)
     * Limitation: NUMBERS ONLY.
     * Length: Must be exactly 12 or 13 digits.
     */
    EAN_13(67),

    /**
     * EAN-8 (Small Global Retail)
     * Limitation: NUMBERS ONLY.
     * Length: Must be exactly 7 or 8 digits.
     */
    EAN_8(68),

    /**
     * CODE 39 (Inventory / Alphanumeric)
     * Limitation: Numbers, UPPERCASE LETTERS ONLY, and basic symbols (- . $ / + % Space).
     * Length: Variable (but takes up a lot of physical width on the paper).
     */
    CODE_39(69),

    /**
     * ITF (Interleaved 2 of 5 / Shipping Boxes)
     * Limitation: NUMBERS ONLY.
     * Length: Variable, but MUST be an EVEN number of digits (e.g., 2, 4, 6, 8...).
     */
    ITF(70),

    /**
     * CODABAR / NW7 (Libraries / Blood Banks)
     * Limitation: Numbers and symbols (- $ : / . +).
     * Special Rule: The first and last character MUST be A, B, C, or D.
     */
    CODABAR(71),

    /**
     * CODE 93 (Compact Inventory)
     * Limitation: Numbers, UPPERCASE LETTERS ONLY, and basic symbols.
     * Length: Variable (more compact than Code 39).
     */
    CODE_93(72),

    /**
     * CODE 128 (The Ultimate POS Barcode)
     * Limitation: NONE. Supports numbers, UPPERCASE, lowercase, and all keyboard symbols.
     * Length: Variable and highly compressed. Best choice for custom Order IDs!
     */
    CODE_128(73)
}