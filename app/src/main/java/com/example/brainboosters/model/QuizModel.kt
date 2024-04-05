package com.example.brainboosters.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class QuizModel(
    val date: String? = null,
    val mood: String? = null,
    val notes: String? = null,
    val questionsRight: Long? = null,
    val questionsWrong: Long? = null,
    val uid: String? = null,
    var documentId: String? = null,
    ) : Serializable
