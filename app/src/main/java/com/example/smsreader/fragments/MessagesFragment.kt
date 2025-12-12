package com.example.smsreader.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.smsreader.R
import com.google.android.material.button.MaterialButton
import com.google.gson.GsonBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SmsTransactionData(
    val smsDate: String,
    val senderAddress: String,
    val transactionType: String,
    val amount: String,
    val upiId: String?,
    val transactionId: String?,
    val partyName: String?,
    val fullMessage: String
)

class MessagesFragment : Fragment() {

    private lateinit var btnSync: MaterialButton
    private lateinit var btnSyncFromDate: MaterialButton
    private lateinit var datePicker: DatePicker
    private lateinit var txtMessages: TextView
    private lateinit var txtLastSyncDate: TextView
    private lateinit var txtLastSyncTime: TextView
    private lateinit var txtLastSyncRecords: TextView

    private var pendingNormalSync = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (pendingNormalSync) startNormalSync()
            else startSyncFromDate()
        } else {
            txtMessages.text = "Permission denied. Cannot read SMS."
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSync = view.findViewById(R.id.btnSync)
        btnSyncFromDate = view.findViewById(R.id.btnSyncFromDate)
        datePicker = view.findViewById(R.id.datePicker)
        txtMessages = view.findViewById(R.id.txtMessages)
        txtLastSyncDate = view.findViewById(R.id.txtLastSyncDate)
        txtLastSyncTime = view.findViewById(R.id.txtLastSyncTime)
        txtLastSyncRecords = view.findViewById(R.id.txtLastSyncRecords)

        btnSync.setOnClickListener {
            checkSmsPermission(normalSync = true)
        }

        btnSyncFromDate.setOnClickListener {
            checkSmsPermission(normalSync = false)
        }
    }

    private fun checkSmsPermission(normalSync: Boolean) {
        pendingNormalSync = normalSync

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
        } else {
            if (normalSync) startNormalSync()
            else startSyncFromDate()
        }
    }

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

    private fun updateSyncUI(list: List<SmsTransactionData>) {
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
        return requireContext().getSharedPreferences("sms_sync_prefs", android.content.Context.MODE_PRIVATE)
            .getLong("last_sync", 0L)
    }

    private fun saveLastSyncTime(ts: Long) {
        requireContext().getSharedPreferences("sms_sync_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putLong("last_sync", ts).apply()
    }

    private fun readSmsTransactions(afterMillis: Long): List<SmsTransactionData> {
        val list = mutableListOf<SmsTransactionData>()

        val cursor = requireContext().contentResolver.query(
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
                    SmsTransactionData(
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

    private fun displaySmsData(transactions: List<SmsTransactionData>) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        txtMessages.text = gson.toJson(transactions)
    }

    private fun uploadAllAtOnce(transactions: List<SmsTransactionData>) {
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
        Volley.newRequestQueue(requireContext()).add(request)
    }
}
