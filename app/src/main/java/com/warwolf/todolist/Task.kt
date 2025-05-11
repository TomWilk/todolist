package com.warwolf.todolist

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Calendar
import java.util.Date


@Entity
data class Task(
    var taskName: String,
    var taskDescription: String?,
    var taskDeadlineDate: Date,
    var taskDeadlineTime: Calendar,
    var taskStatus: TaskStatus,
    var taskPriority: Boolean,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
): Serializable
