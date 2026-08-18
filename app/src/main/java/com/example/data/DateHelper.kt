package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateHelper {
    fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getYesterdayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun getDaysDifference(dateStr1: String, dateStr2: String): Int {
        if (dateStr1.isEmpty() || dateStr2.isEmpty()) return -1
        if (dateStr1 == dateStr2) return 0
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d1 = sdf.parse(dateStr1) ?: return -1
            val d2 = sdf.parse(dateStr2) ?: return -1
            
            // Normalize dates to midnight to prevent time-of-day offsets
            val c1 = Calendar.getInstance().apply { time = d1; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val c2 = Calendar.getInstance().apply { time = d2; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            
            val diffMs = Math.abs(c1.timeInMillis - c2.timeInMillis)
            (diffMs / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            -1
        }
    }
}
