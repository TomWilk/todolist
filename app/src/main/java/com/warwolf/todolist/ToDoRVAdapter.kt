package com.warwolf.todolist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class ToDoRVAdapter(private var tasks: List<Task>): RecyclerView.Adapter<ToDoRVAdapter.TaskViewHolder>(){

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        init{
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    (itemView.context as? MainActivity)?.onTaskLongPressed(tasks[position])
                    true
                } else {
                    false
                }
            }
        }

        fun bind(task: Task){
            tvTaskName.text = task.taskName
            tvDescription.text = task.taskDescription

            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(task.taskDeadlineDate)
            tvDate.text = formattedDate

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formattedTime = timeFormat.format(task.taskDeadlineTime.time)
            tvTime.text = formattedTime

            if (task.taskStatus == TaskStatus.COMPLETED){
                itemView.setBackgroundColor(Color.parseColor("#06ba0e"))
            }else{
                itemView.setBackgroundColor(Color.WHITE)
            }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.task_layout, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = tasks[position]
        holder.bind(currentTask)
    }

    fun updateTasks(newTasks: List<Task>){
        tasks = newTasks
        notifyDataSetChanged()
    }

    fun getTasks(): List<Task>{
        return tasks
    }


}