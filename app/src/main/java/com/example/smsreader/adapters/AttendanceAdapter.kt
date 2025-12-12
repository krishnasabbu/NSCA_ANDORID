package com.example.smsreader.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smsreader.R
import com.example.smsreader.models.PlayerWithAttendance

class AttendanceAdapter(
    private var playersWithAttendance: List<PlayerWithAttendance>,
    private val isToday: Boolean,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    class AttendanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtAvatar: TextView = view.findViewById(R.id.txtAvatar)
        val txtPlayerName: TextView = view.findViewById(R.id.txtPlayerName)
        val txtPlayerBatch: TextView = view.findViewById(R.id.txtPlayerBatch)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val checkboxAttendance: CheckBox = view.findViewById(R.id.checkboxAttendance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val playerWithAttendance = playersWithAttendance[position]
        val player = playerWithAttendance.player

        val initials = player.name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

        holder.txtAvatar.text = initials.ifEmpty { "?" }
        holder.txtPlayerName.text = player.name
        holder.txtPlayerBatch.text = "Batch: ${player.batch.ifEmpty { "N/A" }}"

        if (playerWithAttendance.isPresent) {
            holder.txtStatus.text = "Present"
            holder.txtStatus.setTextColor(Color.parseColor("#2E7D32"))
            holder.txtStatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
            holder.checkboxAttendance.visibility = View.GONE
        } else {
            holder.txtStatus.text = "Absent"
            holder.txtStatus.setTextColor(Color.parseColor("#C62828"))
            holder.txtStatus.setBackgroundColor(Color.parseColor("#FFEBEE"))

            if (isToday) {
                holder.checkboxAttendance.visibility = View.VISIBLE
                holder.checkboxAttendance.isChecked = playerWithAttendance.isSelected

                holder.checkboxAttendance.setOnCheckedChangeListener(null)
                holder.checkboxAttendance.setOnCheckedChangeListener { _, isChecked ->
                    playerWithAttendance.isSelected = isChecked
                    onSelectionChanged()
                }

                holder.itemView.setOnClickListener {
                    holder.checkboxAttendance.isChecked = !holder.checkboxAttendance.isChecked
                }
            } else {
                holder.checkboxAttendance.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
            }
        }
    }

    override fun getItemCount() = playersWithAttendance.size

    fun updateData(newData: List<PlayerWithAttendance>) {
        playersWithAttendance = newData
        notifyDataSetChanged()
    }

    fun getSelectedPlayers(): List<PlayerWithAttendance> {
        return playersWithAttendance.filter { it.isSelected && it.isAbsent }
    }

    fun selectAllAbsent() {
        playersWithAttendance.forEach { playerWithAttendance ->
            if (playerWithAttendance.isAbsent) {
                playerWithAttendance.isSelected = true
            }
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun clearSelection() {
        playersWithAttendance.forEach { it.isSelected = false }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun getAbsentCount(): Int {
        return playersWithAttendance.count { it.isAbsent }
    }

    fun getPresentCount(): Int {
        return playersWithAttendance.count { it.isPresent }
    }

    fun getTotalCount(): Int {
        return playersWithAttendance.size
    }
}
