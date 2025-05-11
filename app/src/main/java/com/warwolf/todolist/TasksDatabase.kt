package com.warwolf.todolist

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Task::class], version = 1)
@TypeConverters(Converters::class)
abstract class TasksDatabase: RoomDatabase() {

    abstract val dao: TasksDao

}