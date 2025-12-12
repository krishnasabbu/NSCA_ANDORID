package com.example.smsreader.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smsreader.R
import com.example.smsreader.models.SmsTransaction
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class SmsTransactionAdapter(private val transactions: List<SmsTransaction>) :
    RecyclerView.Adapter<SmsTransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardTransaction: MaterialCardView = view.findViewById(R.id.cardTransaction)
        val txtDate: TextView = view.findViewById(R.id.txtDate)
        val txtSender: TextView = view.findViewById(R.id.txtSender)
        val txtType: TextView = view.findViewById(R.id.txtType)
        val txtAmount: TextView = view.findViewById(R.id.txtAmount)
        val txtPartyName: TextView = view.findViewById(R.id.txtPartyName)
        val txtUpiId: TextView = view.findViewById(R.id.txtUpiId)
        val txtTransactionId: TextView = view.findViewById(R.id.txtTransactionId)
        val txtMessage: TextView = view.findViewById(R.id.txtMessage)
        val cardType: MaterialCardView = view.findViewById(R.id.cardType)
        val expandedDetails: android.widget.LinearLayout = view.findViewById(R.id.expandedDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sms_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(transaction.smsDate)
            holder.txtDate.text = if (date != null) outputFormat.format(date) else transaction.smsDate
        } catch (e: Exception) {
            holder.txtDate.text = transaction.smsDate
        }

        holder.txtSender.text = transaction.senderAddress.ifEmpty { "-" }
        holder.txtType.text = transaction.transactionType
        holder.txtAmount.text = formatter.format(transaction.amount)
        holder.txtPartyName.text = transaction.partyName.ifEmpty { "N/A" }
        holder.txtUpiId.text = transaction.upiId.ifEmpty { "-" }
        holder.txtTransactionId.text = transaction.transactionId.ifEmpty { "-" }
        holder.txtMessage.text = transaction.fullMessage.ifEmpty { "-" }

        if (transaction.transactionType.equals("CREDIT", ignoreCase = true)) {
            holder.txtType.setTextColor(Color.parseColor("#388E3C"))
            holder.cardType.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
        } else {
            holder.txtType.setTextColor(Color.parseColor("#D32F2F"))
            holder.cardType.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
        }

        holder.expandedDetails.visibility = View.GONE

        holder.cardTransaction.setOnClickListener {
            if (holder.expandedDetails.visibility == View.GONE) {
                holder.expandedDetails.visibility = View.VISIBLE
            } else {
                holder.expandedDetails.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = transactions.size
}
