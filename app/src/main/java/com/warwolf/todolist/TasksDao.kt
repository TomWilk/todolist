package com.warwolf.todolist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TasksDao {

    @Upsert
    suspend fun upsertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM task ORDER BY taskName ASC")
    fun getTasksOrderedByNameASC(): Flow<List<Task>>

    @Query("SELECT * FROM task ORDER BY taskName DESC")
    fun getTasksOrderedByNameDESC(): Flow<List<Task>>

    @Query("SELECT * FROM task ORDER BY taskDeadlineDate ASC, taskDeadlineTime ASC")
    fun getTasksOrderedByDateASC(): Flow<List<Task>>

    @Query("SELECT * FROM task ORDER BY taskDeadlineDate DESC, taskDeadlineTime DESC")
    fun getTasksOrderedByDateDESC(): Flow<List<Task>>

    @Query("SELECT * FROM task ORDER BY taskStatus ASC")
    fun getTasksOrderedByStatusASC(): Flow<List<Task>>

    @Query("SELECT * FROM task ORDER BY taskStatus DESC")
    fun getTasksOrderedByStatusDESC(): Flow<List<Task>>

    @Query("SELECT * FROM task ORDER BY taskPriority ASC, taskDeadlineDate ASC, taskDeadlineTime ASC")
    fun getTasksOrderedByPriorityASC(): Flow<List<Task>>

    @Query("SELECT * FROM task ORDER BY taskPriority DESC, taskDeadlineDate ASC, taskDeadlineTime ASC")
    fun getTasksOrderedByPriorityDESC(): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE id=(SELECT max(id) FROM task)")
    fun getMostRecentTask(): Flow<List<Task>>

}