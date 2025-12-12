package com.example.smsreader.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smsreader.R
import com.example.smsreader.models.Player
import com.google.android.material.switchmaterial.SwitchMaterial

class AttendanceAdapter(private val players: List<Player>) :
    RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    private val attendanceMap = mutableMapOf<String, Boolean>()

    class AttendanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtAvatar: TextView = view.findViewById(R.id.txtAvatar)
        val txtPlayerName: TextView = view.findViewById(R.id.txtPlayerName)
        val txtPlayerBatch: TextView = view.findViewById(R.id.txtPlayerBatch)
        val attendanceSwitch: SwitchMaterial = view.findViewById(R.id.attendanceSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val player = players[position]

        val initials = player.name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

        holder.txtAvatar.text = initials.ifEmpty { "?" }
        holder.txtPlayerName.text = player.name
        holder.txtPlayerBatch.text = "Batch: ${player.batch.ifEmpty { "N/A" }}"

        holder.attendanceSwitch.isChecked = attendanceMap[player.id] ?: false
        holder.attendanceSwitch.setOnCheckedChangeListener { _, isChecked ->
            attendanceMap[player.id] = isChecked
        }
    }

    override fun getItemCount() = players.size

    fun getPresentPlayers(): List<Player> {
        return players.filter { attendanceMap[it.id] == true }
    }

    fun clearSelection() {
        attendanceMap.clear()
        notifyDataSetChanged()
    }
}
