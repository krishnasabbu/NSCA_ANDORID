package com.example.smsreader.models

data class AttendanceRecord(
    val id: String = "",
    val userId: String,
    val sessionId: String,
    val date: String,
    val status: String,
    val checkInTime: String = "",
    val checkOutTime: String = "",
    val markedBy: String = ""
)

data class AttendanceRequest(
    val markedBy: String,
    val records: List<AttendanceRecordRequest>
)

data class AttendanceResponse(
    val status: String,
    val message: String
)

