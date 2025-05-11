package com.warwolf.todolist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val dao: TasksDao
): ViewModel() {


    fun upsertTask(task: Task){
        viewModelScope.launch {
            dao.upsertTask(task)
        }
    }

    fun deleteTask(task: Task){
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }

    fun displayTasks(sortType: SortType): Flow<List<Task>> {
        return when(sortType) {
            SortType.DATE_DESC -> dao.getTasksOrderedByDateDESC()
            SortType.DATE_ASC -> dao.getTasksOrderedByDateASC()
            SortType.TASK_NAME_DESC -> dao.getTasksOrderedByNameDESC()
            SortType.TASK_NAME_ASC -> dao.getTasksOrderedByNameASC()
            SortType.STATUS_DESC -> dao.getTasksOrderedByStatusDESC()
            SortType.STATUS_ASC -> dao.getTasksOrderedByStatusASC()
            SortType.PRIORITY_DESC -> dao.getTasksOrderedByPriorityDESC()
            SortType.PRIORITY_ASC -> dao.getTasksOrderedByPriorityASC()
        }
    }

    fun getLastTask(): Flow<List<Task>> {
        return dao.getMostRecentTask()
    }



}