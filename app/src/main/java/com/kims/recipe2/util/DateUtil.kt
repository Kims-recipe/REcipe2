package com.kims.recipe2.util

import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

object DateUtil {
    fun calculateDDay(expirationDate: Date?): Long? {
        if (expirationDate == null) {
            return null
        }

        // 오늘 날짜의 자정으로 설정
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // 유통기한 날짜의 자정으로 설정
        val expDateNormalized = Calendar.getInstance().apply {
            time = expirationDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // 두 날짜의 차이를 밀리초 단위로 계산
        val diff = expDateNormalized.time - today.time
        // 밀리초를 일(day) 단위로 변환하여 반환
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
    }
}