package com.example.smsreader.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smsreader.R
import com.example.smsreader.models.Player
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

data class FeeItem(
    val player: Player,
    val expected: Double,
    val paid: Double,
    val balance: Double
)

class FeeAdapter(private val fees: List<FeeItem>) :
    RecyclerView.Adapter<FeeAdapter.FeeViewHolder>() {

    class FeeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtAvatar: TextView = view.findViewById(R.id.txtAvatar)
        val txtStudentName: TextView = view.findViewById(R.id.txtStudentName)
        val txtPhone: TextView = view.findViewById(R.id.txtPhone)
        val txtExpected: TextView = view.findViewById(R.id.txtExpected)
        val txtPaid: TextView = view.findViewById(R.id.txtPaid)
        val txtBalance: TextView = view.findViewById(R.id.txtBalance)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val cardStatus: MaterialCardView = view.findViewById(R.id.cardStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fee, parent, false)
        return FeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeeViewHolder, position: Int) {
        val feeItem = fees[position]
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        val initials = feeItem.player.name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

        holder.txtAvatar.text = initials.ifEmpty { "?" }
        holder.txtStudentName.text = feeItem.player.name
        holder.txtPhone.text = feeItem.player.phone
        holder.txtExpected.text = formatter.format(feeItem.expected)
        holder.txtPaid.text = formatter.format(feeItem.paid)
        holder.txtBalance.text = formatter.format(feeItem.balance)

        if (feeItem.balance <= 0) {
            holder.txtStatus.text = "Paid"
            holder.txtStatus.setTextColor(Color.parseColor("#388E3C"))
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
        } else {
            holder.txtStatus.text = "Pending"
            holder.txtStatus.setTextColor(Color.parseColor("#D32F2F"))
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
        }
    }

    override fun getItemCount() = fees.size
}
