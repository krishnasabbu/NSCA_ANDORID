package com.example.smsreader.models

data class AttendanceRecordRequest(
    val userId: String,
    val sessionId: String,
    val date: String,
    val status: String
)

data class AttendanceRequest(
    val markedBy: String,
    val records: List<AttendanceRecordRequest>
)
