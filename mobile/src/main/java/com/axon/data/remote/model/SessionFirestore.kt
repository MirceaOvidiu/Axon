package com.axon.data.remote.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SessionFirestore(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val localId: Long = 0,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val receivedAt: Long = 0,
    val dataPointCount: Int = 0,
    val sparcScore: Double? = null,
    val ldljScore: Double? = null,
    val sparc_results: List<Map<String, Any>>? = null,
    val ldlj_results: List<Map<String, Any>>? = null,
    val sparc_plot_url: String? = null,
    val ldlj_plot_url: String? = null,
    val status: String = "pending",
    @ServerTimestamp
    val uploadedAt: Date? = null,
)
