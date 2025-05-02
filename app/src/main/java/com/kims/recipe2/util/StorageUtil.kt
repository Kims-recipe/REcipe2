package com.kims.recipe2.util

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri
import java.io.File

suspend fun uploadPhoto(file: File): String {
    val ref = Firebase.storage.reference.child("meals/${file.name}")
    ref.putFile(file.toUri()).await()
    return ref.downloadUrl.await().toString()
}
