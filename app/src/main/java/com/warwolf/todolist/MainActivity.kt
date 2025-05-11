package com.warwolf.todolist

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var btnFloatAddNewTask: FloatingActionButton
    private lateinit var rvTaskList: RecyclerView

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

    private val sortingOptions = arrayOf(
        "Data (Rosnąco)",
        "Data (Malejąco)",
        "Nazwa (Rosnąco)",
        "Nazwa (Malejąco)",
        "Status (Najpierw ukończone)",
        "Status (Najpierw zaległe)",
        "Ważność (Najpierw ważne)",
        "Ważność (Najpierw mniej ważne)"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        NotificationUtils.createNotificationChannel(this)

        toolbar = findViewById(R.id.toolbar)
        btnFloatAddNewTask = findViewById(R.id.btnFloatAddNewTask)
        rvTaskList = findViewById(R.id.rvTaskList)
        rvSetup()

        toolbar.title = "Do zrobienia"
        setSupportActionBar(toolbar)


        itemTouchHelper.attachToRecyclerView(rvTaskList)



        btnFloatAddNewTask.setOnClickListener {
            val intent = Intent(this@MainActivity, AddNewTask::class.java)
            addTaskLauncher.launch(intent)
        }

    }




    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.action_sort -> {
                showSortingPopupMenu(findViewById(R.id.action_sort))
                true
            }
            R.id.action_download -> {
                exportTasksToTxt()
                true
            }
            R.id.action_upload -> {
                uploadFileLauncher.launch("text/plain")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val uploadFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){uri ->
        uri?.let {
            lifecycleScope.launch{
                try {
                    val inputStream = contentResolver.openInputStream(it)
                    val tasks = parseTasksFromInputStream(inputStream)
                    inputStream?.close()
                    tasks.forEach {
                        task -> viewModel.upsertTask(task)
                        Log.e("id", task.id.toString())
                    }
                    Toast.makeText(this@MainActivity, "Zadania wgrane", Toast.LENGTH_SHORT).show()
                }catch (e: IllegalArgumentException){
                    Toast.makeText(this@MainActivity, "Plik w nieprawidłowym formacie", Toast.LENGTH_SHORT).show()
                }catch (e: Exception){
                    Toast.makeText(this@MainActivity, "Błąd wgrywania zadań", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parseTasksFromInputStream(inputStream: InputStream?): List<Task>{
        val tasks = mutableListOf<Task>()
        var isValidFormat = true
        inputStream?.bufferedReader()?.useLines { lines ->
            val taskDetails = mutableListOf<String>()
            lines.forEach { line ->
                if(line.isNotBlank()){
                    taskDetails.add(line)
                }else if (taskDetails.isNotEmpty()) {
                    if (taskDetails.size != 6 || !isTaskDetailsValid(taskDetails)){
                        isValidFormat = false
                        return@useLines
                    }
                    tasks.add(createTaskFromDetails(taskDetails))
                    taskDetails.clear()
                }
            }
            if(taskDetails.isNotEmpty()){
                if (taskDetails.size != 6 || !isTaskDetailsValid(taskDetails)) {
                    isValidFormat = false
                }
                tasks.add(createTaskFromDetails(taskDetails))
            }
        }
        if(!isValidFormat) throw IllegalArgumentException("Niedozwolony plik")
        return tasks
    }

    private fun createTaskFromDetails(details: List<String>): Task{
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return Task(
            taskName = details[0],
            taskDescription = details[1],
            taskStatus = TaskStatus.valueOf(details[2]),
            taskPriority = details[3].toBoolean(),
            taskDeadlineDate = dateFormat.parse(details[4])!!,
            taskDeadlineTime = Calendar.getInstance().apply {
                time = timeFormat.parse(details[5])!!
            }
        )
    }

    private fun isTaskDetailsValid(details: List<String>): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            TaskStatus.valueOf(details[2])
            dateFormat.parse(details[4])
            timeFormat.parse(details[5])
            true
        } catch (e: Exception) {
            false
        }
    }



    private fun exportTasksToTxt(){
        lifecycleScope.launch{
            val sb = StringBuilder()
            var export = true
            viewModel.displayTasks(SortType.DATE_ASC).collect { tasks ->
                if (export) {
                    tasks.forEach { task ->
                        sb.append("${task.taskName}\n")
                        sb.append("${task.taskDescription}\n")
                        sb.append("${task.taskStatus}\n")
                        sb.append("${task.taskPriority}\n")
                        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        sb.append("${dateFormat.format(task.taskDeadlineDate)}\n")
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        sb.append("${timeFormat.format(task.taskDeadlineTime.time)}\n")
                        sb.append("\n")
                    }

                    try {
                        val directory =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        var fileName = "lista_zadan"
                        fileName = getUniqueFileName(directory, fileName)
                        val file = File(directory, "$fileName.txt")
                        file.writeText(sb.toString())
                        Toast.makeText(this@MainActivity, "Plik zapisany", Toast.LENGTH_SHORT)
                            .show()

                    } catch (e: IOException) {
                        Toast.makeText(
                            this@MainActivity,
                            "Nie udało się zapisać pliku",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                export = false
            }

        }
    }

    private fun getUniqueFileName(directory: File, baseName: String): String {
        var fileName = baseName
        var counter = 1
        while (File(directory, "$fileName.txt").exists()) {
            fileName = "$baseName($counter)"
            counter++
        }
        return fileName
    }


    private fun rvSetup(){
        rvTaskList.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.HORIZONTAL
            )
        )

        val layoutManager = LinearLayoutManager(this)
        rvTaskList.layoutManager = layoutManager

        displayTasks(SortType.DATE_ASC)


    }

    private fun displayTasks(sortType: SortType){
        val taskAdapter = ToDoRVAdapter(emptyList())
        rvTaskList.adapter = taskAdapter
        lifecycleScope.launch {
            viewModel.displayTasks(sortType).collect{tasks ->
                taskAdapter.updateTasks(tasks)

            }
        }
    }

    private fun showSortingPopupMenu(view: View){
        val popupMenu = PopupMenu(this, view)
        for (i in sortingOptions.indices) {
            popupMenu.menu.add(Menu.NONE, i, i, sortingOptions[i])
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            val selectedOption = sortingOptions[menuItem.itemId]
            handleSortingOption(selectedOption)
            true
        }
        popupMenu.show()
    }

    private fun handleSortingOption(selectedOption: String){
        when(selectedOption){
            "Data (Rosnąco)" -> displayTasks(SortType.DATE_ASC)
            "Data (Malejąco)" -> displayTasks(SortType.DATE_DESC)
            "Nazwa (Rosnąco)" -> displayTasks(SortType.TASK_NAME_ASC)
            "Nazwa (Malejąco)" -> displayTasks(SortType.TASK_NAME_DESC)
            "Status (Najpierw ukończone)" -> displayTasks(SortType.STATUS_ASC)
            "Status (Najpierw zaległe)" -> displayTasks(SortType.DATE_DESC)
            "Ważność (Najpierw ważne)" -> displayTasks(SortType.PRIORITY_DESC)
            "Ważność (Najpierw mniej ważne)" -> displayTasks(SortType.PRIORITY_ASC)
        }
    }

    private val addTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
        if(result.resultCode == Activity.RESULT_OK){
            val taskAdded = result.data?.getBooleanExtra("taskAdded", false) ?: false
            if (taskAdded){
                displayTasks(SortType.DATE_ASC)
                lifecycleScope.launch {
                    viewModel.getLastTask().collect { tasks ->
                        tasks.forEach { task ->
                            if(task.taskStatus != TaskStatus.COMPLETED) {
                                NotificationUtils.scheduleNotification(
                                    this@MainActivity,
                                    task,
                                    "Upcoming"
                                )
                                NotificationUtils.scheduleNotification(
                                    this@MainActivity,
                                    task,
                                    "Overdue"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private val updateTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
        if(result.resultCode == Activity.RESULT_OK){
            val taskAdded = result.data?.getBooleanExtra("taskAdded", false) ?: false
            if (taskAdded){
                displayTasks(SortType.DATE_ASC)
                val updatedTask = result.data?.getSerializableExtra("updatedTask") as? Task
                if (updatedTask != null) {
                    if(updatedTask.taskStatus != TaskStatus.COMPLETED){
                        NotificationUtils.scheduleNotification(
                            this@MainActivity,
                            updatedTask,
                            "Upcoming"
                        )
                        NotificationUtils.scheduleNotification(
                            this@MainActivity,
                            updatedTask,
                            "Overdue"
                        )
                    }
                }
            }
        }
    }

    fun onTaskLongPressed(task: Task) {
        val intent = Intent(this, UpdateTask::class.java)
        intent.putExtra("task", task)
        updateTaskLauncher.launch(intent)
    }



    private val itemTouchHelper = ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT){
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            val task = (rvTaskList.adapter as ToDoRVAdapter).getTasks()[position]
            NotificationUtils.deleteNotifications(this@MainActivity, task.id)
            when (direction) {
                ItemTouchHelper.LEFT -> {
                    viewModel.deleteTask(task)
                    (rvTaskList.adapter as ToDoRVAdapter).notifyItemRemoved(position)

                }

                ItemTouchHelper.RIGHT -> {
                    if (task.taskStatus != TaskStatus.COMPLETED) {
                        task.taskStatus = TaskStatus.COMPLETED
                        viewModel.upsertTask(task)
                    }
                    (rvTaskList.adapter as ToDoRVAdapter).notifyItemChanged(position)

                }
            }


        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView
            val background = ColorDrawable()


            if(dX < 0){
                background.color = Color.RED
                background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)

            }else if(dX > 0){
                background.color = Color.parseColor("#06ba0e")
                background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
            }

            background.draw(c)

            super.onChildDraw(
                c,
                recyclerView,
                viewHolder,
                dX,
                dY,
                actionState,
                isCurrentlyActive
            )
        }

    })




}