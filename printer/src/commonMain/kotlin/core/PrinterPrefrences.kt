package core
import com.russhwolf.settings.Settings


internal class PrinterPreferences {
    private val settings = Settings()

    // --- Keys ---
  companion object{
        private val PREF_LAST_PRINTER_NAME = "PREF_LAST_PRINTER_NAME" // For Windows
        private val PREF_LAST_VENDOR_ID = "PREF_LAST_VENDOR_ID"       // For Android
        private val PREF_LAST_PRODUCT_ID = "PREF_LAST_PRODUCT_ID"     // For Android
  }

    // ==========================================
    // WINDOWS PREFERENCES (Printer Name)
    // ==========================================
    fun saveLastPrinterName(name: String) {
        settings.putString(PREF_LAST_PRINTER_NAME, name)
    }

    fun getLastPrinterName(): String {
        return settings.getString(PREF_LAST_PRINTER_NAME, "")
    }


    // ==========================================
    // ANDROID PREFERENCES (VID & PID)
    // ==========================================
    fun saveLastUsbTarget(vendorId: String, productId: String) {
        settings.putString(PREF_LAST_VENDOR_ID, vendorId)
        settings.putString(PREF_LAST_PRODUCT_ID, productId)
    }

    fun getLastVendorId(): String {
        return settings.getString(PREF_LAST_VENDOR_ID, "")
    }

    fun getLastProductId(): String {
        return settings.getString(PREF_LAST_PRODUCT_ID, "")
    }

    fun clearLastUsbTarget() {
        settings.remove(PREF_LAST_VENDOR_ID)
        settings.remove(PREF_LAST_PRODUCT_ID)
        settings.remove(PREF_LAST_PRINTER_NAME)
    }
}



