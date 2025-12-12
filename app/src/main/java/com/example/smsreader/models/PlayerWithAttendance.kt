package com.example.smsreader.models

data class PlayerWithAttendance(
    val player: Player,
    val attendanceRecord: AttendanceRecord?,
    var isSelected: Boolean = false
) {
    val isPresent: Boolean
        get() = attendanceRecord?.status?.lowercase() == "present"

    val isAbsent: Boolean
        get() = !isPresent
}
