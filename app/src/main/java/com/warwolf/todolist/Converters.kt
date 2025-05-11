package com.warwolf.todolist

import androidx.room.TypeConverter
import java.util.Calendar
import java.util.Date

class Converters {
    @TypeConverter
    fun timestampToDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }

    @TypeConverter
    fun timestampToCalendar(value: Long?): Calendar? {
        return value?.let {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it
            calendar
        }
    }

    @TypeConverter
    fun calendarToTimestamp(calendar: Calendar?): Long?{
        return calendar?.timeInMillis
    }


}