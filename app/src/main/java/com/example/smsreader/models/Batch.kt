package com.example.smsreader.models

data class Batch(
    val id: String,
    val name: String,
    val description: String,
    val coach: String,
    val coachId: String,
    val schedule: String,
    val status: String
) {
    override fun toString(): String {
        return name
    }
}

data class BatchesResponse(
    val status: String,
    val message: String,
    val batches: List<Batch>
)
