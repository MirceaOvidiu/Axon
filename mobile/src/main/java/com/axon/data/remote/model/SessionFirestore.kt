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
    @ServerTimestamp
    val uploadedAt: Date? = null,
)
