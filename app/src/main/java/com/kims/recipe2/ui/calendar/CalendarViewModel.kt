// CalendarViewModel.kt
package com.kims.recipe2.ui.calendar

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kims.recipe2.model.MealRecord
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class CalendarViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // LiveData를 사용하여 특정 날짜의 식단 기록을 Fragment에 전달
    private val _mealRecords = MutableLiveData<List<MealRecord>>()
    val mealRecords: LiveData<List<MealRecord>> get() = _mealRecords

    // 식단 기록이 있는 날짜를 저장하기 위한 LiveData
    private val _datesWithMeals = MutableLiveData<Set<LocalDate>>()
    val datesWithMeals: LiveData<Set<LocalDate>> get() = _datesWithMeals

    init {
        // ViewModel 초기화 시 전체 식단 기록을 가져와서 식단이 있는 날짜를 파악
        fetchAllMealRecords()
    }

    private fun fetchAllMealRecords() {
        if (userId == null) {
            Log.e("CalendarViewModel", "User ID is null. Cannot fetch meal records.")
            return
        }
        db.collection("users").document(userId).collection("mealRecords")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val dates = querySnapshot.documents
                    .mapNotNull { it.toObject(MealRecord::class.java) }
                    .mapNotNull { it.date?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() }
                    .toSet()
                _datesWithMeals.postValue(dates)
            }
            .addOnFailureListener { e ->
                Log.e("CalendarViewModel", "Error fetching all meal records for dates", e)
            }
    }

    private var mealRecordsListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun fetchMealRecordsForDate(date: LocalDate) {
        if (userId == null) {
            Log.e("CalendarViewModel", "User ID is null. Cannot fetch meal records.")
            return
        }

        // 기존 리스너가 있다면 제거
        mealRecordsListener?.remove()

        val startOfDay = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val endOfDay = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant())

        // ✨ get() 대신 addSnapshotListener 사용
        mealRecordsListener = db.collection("users").document(userId).collection("mealRecords")
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _mealRecords.postValue(emptyList())
                    Log.e("CalendarViewModel", "Error fetching meal records for $date", error)
                    return@addSnapshotListener
                }

                val records = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(MealRecord::class.java)?.apply {
                        id = document.id // 문서 ID를 MealRecord 객체에 저장
                    }
                } ?: emptyList()

                _mealRecords.postValue(records)
                Log.d("CalendarViewModel", "Fetched ${records.size} real-time records for $date")
            }
    }

    // ✨ ViewModel이 파괴될 때 리스너를 제거하는 함수
    override fun onCleared() {
        super.onCleared()
        mealRecordsListener?.remove()
        Log.d("CalendarViewModel", "Meal records listener removed.")
    }
}