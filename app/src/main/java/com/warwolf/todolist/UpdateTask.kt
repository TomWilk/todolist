package com.warwolf.todolist

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UpdateTask : AppCompatActivity() {


    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var etTaskName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var btnUpdateTask: Button
    private lateinit var cbPriority: CheckBox
    private lateinit var cbStatus: CheckBox
    private val calendar = Calendar.getInstance()
    private var year = calendar.get(Calendar.YEAR)
    private var month = calendar.get(Calendar.MONTH)
    private var day = calendar.get(Calendar.DAY_OF_MONTH)
    private var hour = calendar.get(Calendar.HOUR_OF_DAY)
    private var minute = calendar.get(Calendar.MINUTE)

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            TasksDatabase::class.java,
            "tasks.db"
        ).build()
    }

    private val viewModel: AppViewModel by viewModels<AppViewModel>(
        factoryProducer = {
            object: ViewModelProvider.Factory{
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(db.dao) as T
                }
            }
        }
    )



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_task)

        toolbar = findViewById(R.id.toolbar)
        toolbar.title = "Edytuj zadanie"

        btnUpdateTask = findViewById(R.id.btnUpdateTask)
        etTaskName = findViewById(R.id.etUpdateTaskName)
        etDescription = findViewById(R.id.etUpdateDescription)

        etDate = findViewById(R.id.etUpdateDate)
        etTime = findViewById(R.id.etUpdateTime)

        cbPriority = findViewById(R.id.cbUpdatePriority)
        cbStatus = findViewById(R.id.cbStatus)

        val task = intent.getSerializableExtra("task") as? Task

        if (task != null) {
            etTaskName.text = Editable.Factory.getInstance().newEditable(task.taskName)
            etDescription.text = Editable.Factory.getInstance().newEditable(task.taskDescription)

            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(task.taskDeadlineDate)
            etDate.text = Editable.Factory.getInstance().newEditable(formattedDate)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formattedTime = timeFormat.format(task.taskDeadlineTime.time)
            etTime.text = Editable.Factory.getInstance().newEditable(formattedTime)

            cbPriority.isChecked = task.taskPriority

            when(task.taskStatus){
                TaskStatus.COMPLETED -> cbStatus.isChecked = true
                TaskStatus.NOT_COMPLETED -> cbStatus.isChecked = false
                TaskStatus.OVERDUE -> cbStatus.isChecked = false
            }

        }




        etDate.setOnClickListener {

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, monthOfYear, dayOfMonth ->
                    val date = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                    etDate.setText(date)
                    this.year = year
                    this.month = monthOfYear
                    this.day = dayOfMonth
                },
                year,
                month,
                day
            )

            datePickerDialog.show()
        }

        etTime.setOnClickListener {

            val timePickerDialog = TimePickerDialog(
                this,
                { _, hourOfDay, minuteOfHour ->
                    val time = ("$hourOfDay:$minuteOfHour")
                    etTime.setText(time)
                    hour = hourOfDay
                    minute = minuteOfHour
                },
                hour,
                minute,
                true
            )

            timePickerDialog.show()
        }

        btnUpdateTask.setOnClickListener {

            val taskName = etTaskName.text.toString()
            val taskDescription = etDescription.text.toString()
            val dateStr = etDate.text.toString()
            val timeStr = etTime.text.toString()

            if(taskName.isEmpty() || taskDescription.isEmpty() || dateStr.isEmpty() || timeStr.isEmpty()){
                Toast.makeText(this@UpdateTask, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val deadlineDate: Date
            try{
                deadlineDate = dateFormat.parse(dateStr)!!
            }catch (e: ParseException){
                Toast.makeText(this@UpdateTask, "Nieprawidłowy format daty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val deadlineTime: Calendar = Calendar.getInstance()
            try{
                deadlineTime.time = timeFormat.parse(timeStr)!!
            }catch (e: ParseException){
                Toast.makeText(this@UpdateTask, "Nieprawidłowy format godziny", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var currentDate = Calendar.getInstance()

            currentDate.set(Calendar.HOUR_OF_DAY, 0)
            currentDate.set(Calendar.MINUTE, 0)
            currentDate.set(Calendar.SECOND, 0)
            currentDate.set(Calendar.MILLISECOND, 0)

            val deadlineDateCalendar: Calendar = Calendar.getInstance()
            deadlineDateCalendar.time = deadlineDate

            if(deadlineDateCalendar.before(currentDate)){
                Toast.makeText(this@UpdateTask, "Wybierz jakiś termin w przyszłości", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (deadlineDateCalendar.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                deadlineDateCalendar.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH) &&
                deadlineDateCalendar.get(Calendar.DAY_OF_MONTH) == currentDate.get(Calendar.DAY_OF_MONTH)){

                currentDate = Calendar.getInstance()

                val currentHour = currentDate.get(Calendar.HOUR_OF_DAY)
                val currentMinute = currentDate.get(Calendar.MINUTE)

                val deadlineHour = deadlineTime.get(Calendar.HOUR_OF_DAY)
                val deadlineMinute = deadlineTime.get(Calendar.MINUTE)


                if (deadlineHour < currentHour || (deadlineHour == currentHour && deadlineMinute <= currentMinute)) {
                    Toast.makeText(this@UpdateTask, "Wybierz jakiś termin w przyszłości", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }



            if (task != null) {
                task.taskName = taskName
                task.taskDescription = taskDescription
                task.taskDeadlineDate = deadlineDate
                task.taskDeadlineTime = deadlineTime
                task.taskPriority = cbPriority.isChecked
                when(cbStatus.isChecked){
                    true -> task.taskStatus = TaskStatus.COMPLETED
                    false -> task.taskStatus = TaskStatus.NOT_COMPLETED
                }
                viewModel.upsertTask(task)
            }

            val resultIntent = Intent()
            resultIntent.putExtra("taskAdded", true)
            resultIntent.putExtra("updatedTask", task)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()

        }


    }

}