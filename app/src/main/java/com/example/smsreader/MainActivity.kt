package com.example.smsreader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import java.util.*

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

    private lateinit var btnSync: Button
    private lateinit var btnSyncFromDate: Button
    private lateinit var datePicker: DatePicker
    private lateinit var txtMessages: TextView

    private lateinit var txtLastSyncDate: TextView
    private lateinit var txtLastSyncTime: TextView
    private lateinit var txtLastSyncRecords: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        btnSync = findViewById(R.id.btnSync)
        btnSyncFromDate = findViewById(R.id.btnSyncFromDate)
        datePicker = findViewById(R.id.datePicker)
        txtMessages = findViewById(R.id.txtMessages)

        txtLastSyncDate = findViewById(R.id.txtLastSyncDate)
        txtLastSyncTime = findViewById(R.id.txtLastSyncTime)
        txtLastSyncRecords = findViewById(R.id.txtLastSyncRecords)

        btnSync.setOnClickListener {
            checkSmsPermission(normalSync = true)
        }

        btnSyncFromDate.setOnClickListener {
            checkSmsPermission(normalSync = false)
        }
    }

    // --------------------------
    // Permission Handler
    // --------------------------
    private fun checkSmsPermission(normalSync: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE
            )

            // Save type of sync pending
            getSharedPreferences("flags", MODE_PRIVATE)
                .edit().putBoolean("normalSync", normalSync).apply()

        } else {
            if (normalSync) startNormalSync()
            else startSyncFromDate()
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

            val normal = getSharedPreferences("flags", MODE_PRIVATE)
                .getBoolean("normalSync", true)

            if (normal) startNormalSync()
            else startSyncFromDate()

        } else {
            txtMessages.text = "Permission denied. Cannot read SMS."
        }
    }

    // --------------------------
    // Sync Methods
    // --------------------------
    private fun startNormalSync() {
        val lastSync = getLastSyncTime()
        val list = readSmsTransactions(lastSync)

        updateSyncUI(list)
        displaySmsData(list)
        uploadAllAtOnce(list)

        saveLastSyncTime(System.currentTimeMillis())
    }

    private fun startSyncFromDate() {
        val cal = Calendar.getInstance()
        cal.set(datePicker.year, datePicker.month, datePicker.dayOfMonth, 0, 0, 0)
        val startMillis = cal.timeInMillis

        val list = readSmsTransactions(startMillis)

        updateSyncUI(list)
        displaySmsData(list)
        uploadAllAtOnce(list)

        saveLastSyncTime(startMillis)
    }

    // --------------------------
    // UI Updates
    // --------------------------
    private fun updateSyncUI(list: List<SmsTransaction>) {
        val now = System.currentTimeMillis()

        txtLastSyncDate.text = "Last Sync Date: ${formatDate(now)}"
        txtLastSyncTime.text = "Last Sync Time: ${formatTime(now)}"
        txtLastSyncRecords.text = "Last Sync Records: ${list.size}"
    }

    private fun formatDate(ts: Long): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    private fun formatTime(ts: Long): String {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    private fun getLastSyncTime(): Long {
        return getSharedPreferences("sms_sync_prefs", MODE_PRIVATE)
            .getLong("last_sync", 0L)
    }

    private fun saveLastSyncTime(ts: Long) {
        getSharedPreferences("sms_sync_prefs", MODE_PRIVATE)
            .edit().putLong("last_sync", ts).apply()
    }

    // --------------------------
    // Read SMS
    // --------------------------
    private fun readSmsTransactions(afterMillis: Long): List<SmsTransaction> {

        val list = mutableListOf<SmsTransaction>()

        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            null,
            "${Telephony.Sms.DATE} > ?",
            arrayOf(afterMillis.toString()),
            Telephony.Sms.DEFAULT_SORT_ORDER
        )

        val amountRegex = Regex("(?:Rs|INR)[. ]*([0-9,]+(?:.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)

        val dateFormat = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.getDefault())

        if (cursor != null) {
            while (cursor.moveToNext()) {

                val dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))

                val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: ""
                if (!address.contains("HDFCBK", ignoreCase = true)) continue

                val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)).trim()

                val amt = amountRegex.find(body)?.groupValues?.get(1) ?: continue

                val smsDate = dateFormat.format(Date(dateMillis))

                list.add(
                    SmsTransaction(
                        smsDate,
                        address,
                        "CREDIT",
                        amt,
                        "N/A",
                        "N/A",
                        "N/A",
                        body
                    )
                )
            }
            cursor.close()
        }

        return list
    }

    // --------------------------
    // UI JSON Display
    // --------------------------
    private fun displaySmsData(transactions: List<SmsTransaction>) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        txtMessages.text = gson.toJson(transactions)
    }

    // --------------------------
    // API Upload (same as yours)
    // --------------------------
    private fun uploadAllAtOnce(transactions: List<SmsTransaction>) {

        if (transactions.isEmpty()) {
            Log.d("UPLOAD", "Nothing to upload")
            return
        }

        val url = "https://script.google.com/macros/s/AKfycbwrXvBLtU9goiI7lJNPV4ArDzSAMMdqSphH0fXHGmK_v1r2xm1J4Zcuy2favVKBJh4iNQ/exec"

        val items = JSONArray()
        transactions.forEach { sms ->
            val obj = JSONObject()
            obj.put("smsDate", sms.smsDate)
            obj.put("senderAddress", sms.senderAddress)
            obj.put("transactionType", sms.transactionType)
            obj.put("amount", sms.amount)
            obj.put("upiId", sms.upiId)
            obj.put("transactionId", sms.transactionId)
            obj.put("partyName", sms.partyName)
            obj.put("fullMessage", sms.fullMessage)
            items.put(obj)
        }

        val body = JSONObject()
        body.put("action", "batchSms")
        body.put("items", items)

        val request = object : JsonObjectRequest(
            Request.Method.POST, url, body,
            { Log.d("UPLOAD", "Success") },
            { Log.e("UPLOAD", "Error", it) }
        ) {
            override fun getHeaders() = mutableMapOf("Content-Type" to "application/json")
            override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
                val raw = response?.data?.let { String(it) } ?: ""
                return try {
                    Response.success(JSONObject(raw), HttpHeaderParser.parseCacheHeaders(response))
                } catch (ex: Exception) {
                    Response.error(ParseError(ex))
                }
            }
        }

        request.retryPolicy = DefaultRetryPolicy(120000, 0, 1f)
        Volley.newRequestQueue(this).add(request)
    }
}
