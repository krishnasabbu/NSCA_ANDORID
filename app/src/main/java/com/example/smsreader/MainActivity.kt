package com.example.smsreader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.GsonBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

// -----------------------------
// Data Class
// -----------------------------
data class SmsTransaction(
    val smsDate: String,
    val senderAddress: String,
    val transactionType: String,
    val amount: String,
    val upiId: String?,
    val transactionId: String?,
    val partyName: String?,
    val fullMessage: String
)

class MainActivity : AppCompatActivity() {

    private val SMS_PERMISSION_CODE = 101
    private val TAG = "MainActivity"

    private lateinit var btnSync: Button
    private lateinit var txtMessages: TextView

    private lateinit var txtLastSyncDate: TextView
    private lateinit var txtLastSyncTime: TextView
    private lateinit var txtLastSyncRecords: TextView

    private val syncHandler = android.os.Handler(android.os.Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        btnSync = findViewById(R.id.btnSync)
        txtMessages = findViewById(R.id.txtMessages)

        btnSync.setOnClickListener {
            checkSmsPermission()
        }

        resetSyncToToday()

        txtLastSyncDate = findViewById(R.id.txtLastSyncDate)
        txtLastSyncTime = findViewById(R.id.txtLastSyncTime)
        txtLastSyncRecords = findViewById(R.id.txtLastSyncRecords)

        Log.d("SMS_SYNC", "⏱ Auto-sync scheduler started")
        syncHandler.post(syncRunnable)  // Start auto-sync ONCE
    }

    private fun resetSyncToToday() {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.MONTH, java.util.Calendar.OCTOBER)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        val todayStart = calendar.timeInMillis

        val prefs = getSharedPreferences("sms_sync_prefs", MODE_PRIVATE)
        prefs.edit().putLong("last_sync", todayStart).apply()

        Log.d("SMS_SYNC", "🔄 lastSync reset to today's 00:00 = ${Date(todayStart)}")
    }

    override fun onDestroy() {
        super.onDestroy()
        syncHandler.removeCallbacks(syncRunnable)
    }

    private fun saveLastSyncTime(millis: Long) {
        val prefs = getSharedPreferences("sms_sync_prefs", MODE_PRIVATE)
        prefs.edit().putLong("last_sync", millis).apply()
    }

    private fun getLastSyncTime(): Long {
        val prefs = getSharedPreferences("sms_sync_prefs", MODE_PRIVATE)
        return prefs.getLong("last_sync", 0L)
    }


    private val syncRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "⏱ Auto Sync Triggered")

            val transactions = readSmsTransactions()

            // 2️⃣ Now fetch fresh updated time
            val lastSync = getLastSyncTime()
            val date = formatDate(lastSync)
            val time = formatTime(lastSync)

            // 3️⃣ Update UI
            txtLastSyncDate.text = "Last Sync Date: $date"
            txtLastSyncTime.text = "Last Sync Time: $time"
            txtLastSyncRecords.text = "Last Sync Records: ${transactions.size}"

            if (transactions.isNotEmpty()) {
                uploadAllAtOnce(transactions)
            }

            syncHandler.postDelayed(this, 5 * 60 * 1000)
        }
    }

    // -----------------------------
    // Permission
    // -----------------------------
    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE
            )
        } else {
            pushToUIAndUpload()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            pushToUIAndUpload()
        } else {
            txtMessages.text = "Permission denied. Cannot read SMS."
        }
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "Never"
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return "Never"
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // -----------------------------
    // Start
    // -----------------------------
    private fun pushToUIAndUpload() {
        // 1️⃣ Read SMS (this internally saves the new lastSyncTime)
        val transactions = readSmsTransactions()

        // 2️⃣ Now fetch fresh updated time
        val lastSync = getLastSyncTime()
        val date = formatDate(lastSync)
        val time = formatTime(lastSync)

        // 3️⃣ Update UI
        txtLastSyncDate.text = "Last Sync Date: $date"
        txtLastSyncTime.text = "Last Sync Time: $time"
        txtLastSyncRecords.text = "Last Sync Records: ${transactions.size}"

        // 4️⃣ Display + Upload
        displaySmsData(transactions)
        uploadAllAtOnce(transactions)
    }

    // -----------------------------
    // Extract Helpers
    // -----------------------------
    private fun extractTransactionType(message: String): String {
        val lower = message.lowercase()

        return when {
            "credited" in lower || "received" in lower -> "CREDIT"
            "debited" in lower || "sent" in lower || "paid" in lower -> "DEBIT"
            "upi" in lower -> "UPI"
            else -> "OTHER"
        }
    }

    private fun extractPartyName(message: String): String? {
        val toRegex = Regex("""(?i)^\s*to\s+(.+)$""", RegexOption.MULTILINE)
        val matchTo = toRegex.find(message)
        if (matchTo != null) return matchTo.groupValues[1].trim()

        val fromRegex = Regex("""(?i)^\s*from\s+(.+)$""", RegexOption.MULTILINE)
        val matchFrom = fromRegex.find(message)
        if (matchFrom != null) return matchFrom.groupValues[1].trim()

        return "N/A"
    }

    // -----------------------------
    // Read SMS
    // -----------------------------
    private fun readSmsTransactions(): List<SmsTransaction> {

        val lastSync = getLastSyncTime()
        val now = System.currentTimeMillis()

        Log.d(TAG, "⏱ Last sync timestamp: $lastSync (${Date(lastSync)})")

        val list = mutableListOf<SmsTransaction>()

        val amountRegex = Regex("""(?:Rs|INR)[\.\s]*([0-9,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val upiRegex = Regex("""VPA[\s:]*([A-Za-z0-9.\-]+@[A-Za-z]+)""", RegexOption.IGNORE_CASE)
        val txnRegex = Regex("""(?:UPI|Ref|Reference|Txn)[\s:()]*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)

//        val cursor = contentResolver.query(
//            Telephony.Sms.Inbox.CONTENT_URI,
//            null, null, null,
//            Telephony.Sms.DEFAULT_SORT_ORDER
//        )

        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            null,
            "${Telephony.Sms.DATE} > ?",
            arrayOf(lastSync.toString()),
            Telephony.Sms.DEFAULT_SORT_ORDER
        )

        if (cursor != null) {
            val dateFormat = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.getDefault())

            while (cursor.moveToNext()) {

                val dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
                if (dateMillis <= lastSync) continue

                val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""

                // 🔥 ONLY HDFC Bank SMS
                if (!address.contains("HDFCBK", ignoreCase = true)) continue

                val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)).trim()

                // 🔥 Extract transaction type
                val txnType = extractTransactionType(body).uppercase()

                // 🔥 FILTER: Only DEBIT or CREDIT allowed
                if (txnType != "DEBIT" && txnType != "CREDIT") continue

                // 🔥 Extract amount
                val amountText = amountRegex.find(body)?.groupValues?.get(1) ?: ""
                if (amountText.isBlank()) continue

                val amountValue = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
                if (amountValue <= 0) continue

                // val dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val smsDate = dateFormat.format(Date(dateMillis))

                val upiId = upiRegex.find(body)?.groupValues?.get(1) ?: "N/A"
                val transactionId = txnRegex.find(body)?.groupValues?.get(1) ?: "N/A"

                list.add(
                    SmsTransaction(
                        smsDate,
                        address,
                        txnType,       // ❤️ ONLY DEBIT / CREDIT comes here
                        amountText,
                        upiId,
                        transactionId,
                        extractPartyName(body),
                        body
                    )
                )
            }
            cursor.close()
        }

        // Save new sync time
        saveLastSyncTime(now)

        return list
    }




    // -----------------------------
    // Display JSON
    // -----------------------------
    private fun displaySmsData(transactions: List<SmsTransaction>) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        txtMessages.text = gson.toJson(transactions)
    }

    fun convertSmsDateForSheet(dateStr: String): String {
        // Input: dd-MM-yy HH:mm:ss   e.g., "09-11-25 14:21:58"
        val input = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.US)
        input.timeZone = TimeZone.getTimeZone("Asia/Kolkata")

        val date = input.parse(dateStr)

        // Output: ISO 8601 with timezone (Google Sheets compatible)
        val output = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        output.timeZone = TimeZone.getTimeZone("Asia/Kolkata")

        return output.format(date!!)
    }



    // -----------------------------
    // UPLOAD ALL SMS IN ONE SHOT (BATCH)
    // -----------------------------
    private fun uploadAllAtOnce(transactions: List<SmsTransaction>) {

//        val pingUrl = "https://httpbin.org/get"
//        val pingReq = JsonObjectRequest(Request.Method.GET, pingUrl, null,
//            { resp -> Log.d("NET_TEST", "Ping success: $resp") },
//            { err ->
//                Log.e("NET_TEST", "Ping failed: ${err.message}", err)
//            }
//        )
//        Volley.newRequestQueue(this).add(pingReq)

        val url = "https://script.google.com/macros/s/AKfycbyjh8NvByAX2DvtXmHD5X-S8mPL7UT80sgugirqFAYKzH1vlQb88MJk3I8THvyFQQl3Vg/exec"

        val itemsArray = JSONArray()

        Log.d(TAG, "📤 count: ${transactions.size}")

        if (transactions.isEmpty()) {
            Log.d(TAG, "⏭ No transactions to upload — skipping API call")
            return
        }

        transactions.forEach { sms ->
            val obj = JSONObject().apply {
                put("smsDate", convertSmsDateForSheet(sms.smsDate))
                put("senderAddress", sms.senderAddress)
                put("transactionType", sms.transactionType)
                put("amount", sms.amount)
                put("upiId", sms.upiId)
                put("transactionId", sms.transactionId)
                put("partyName", sms.partyName)
                put("fullMessage", sms.fullMessage)
            }
            itemsArray.put(obj)
        }

        val requestBody = JSONObject().apply {
            put("action", "batchSms")
            put("items", itemsArray)
        }

        Log.d(TAG, "📤 Sending Batch: $requestBody")

        val request = object : JsonObjectRequest(
            Request.Method.POST,
            url,
            requestBody,
            { response ->
                Log.d(TAG, "✅ Batch Upload Success: $response")
            },
            { error ->
                Log.e(TAG, "❌ Batch Upload Error: ${error.networkResponse?.statusCode}", error)
            }
        ) {

            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "Content-Type" to "application/json"
                )
            }

            override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
                val raw = response?.data?.let { String(it) } ?: ""
                return try {
                    Response.success(JSONObject(raw), HttpHeaderParser.parseCacheHeaders(response))
                } catch (ex: Exception) {
                    Response.error(ParseError(ex))
                }
            }
        }

        request.retryPolicy = DefaultRetryPolicy(
            120_000,   // ⏳ 60 seconds timeout
            0,        // 🔒 No retries (important to avoid duplicates)
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(request)
    }
}
