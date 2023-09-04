package tasklist
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import java.io.File
import java.lang.reflect.ParameterizedType
import java.time.LocalTime
import kotlin.Exception

val actions = listOf("add","print","edit","delete","end")
val editActions = listOf("priority","date","time","task")
val priorities = listOf("C","H","N","L","")
lateinit var inputDate:LocalDate
lateinit var inputTime:LocalTime
lateinit var inputAction:String
lateinit var inputEditField:String
lateinit var inputPriority:String
var inputTaskNo: Int? = null
var i:Int = 1
val AllTask = mutableListOf<TList>()
data class TList(
    var date:String,
    var time: String,
    var priority: String,
    var due:String,
    var list: MutableList<String>
)
val jsonFile  = File("tasklist.json")
val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
val type: ParameterizedType = Types.newParameterizedType(List::class.java, TList::class.java)
val TListAdapter: JsonAdapter<List<TList?>> = moshi.adapter(type)
interface TaskList {
    fun startMachine():String {
        println("Input an action (add, print, edit, delete, end):")
        inputAction= readln()
        if (inputAction.lowercase() !in actions || inputAction.isEmpty()) {
            println("The input action is invalid")
            startMachine()
        }
        return inputAction
    }
    private fun getPriority():String {
        println("Input the task priority (C, H, N, L):")
        inputPriority = readln()
        if (inputPriority.uppercase() !in priorities) getPriority()
        return when(inputPriority.uppercase()) {
            "C" -> "\u001B[101m \u001B[0m"
            "H" -> "\u001B[103m \u001B[0m"
            "N" -> "\u001B[102m \u001B[0m"
            "L" -> "\u001B[104m \u001B[0m"
            else -> ""
        }
    }

    private fun getDate():String {
        println("Input the date (yyyy-mm-dd):")
        try {
            inputDate = readln().split("-").map { it.toInt() }
                .let { LocalDate(it[0], it[1], it[2]) }
        } catch (e:Exception) {
            println("The input date is invalid")
            getDate()
        }
        return inputDate.toString()
    }
    private fun getTime():String {
        println("Input the time (hh:mm):")
        try {
            inputTime = readln().split(":").map { it.toInt() }
                .let { LocalTime.of(it[0],it[1])}
        } catch (e:Exception) {
            println("The input time is invalid")
            getTime()
        }
        return inputTime.toString()
    }
    private fun getList():MutableList<String> {
        println("Input a new task (enter a blank line to end):")
        val mList = mutableListOf<String>()
        do {
            val input = readln()
            mList.add(input.trim())
        } while(input.isNotBlank())
        if (mList.first().isEmpty()) println("The task is blank")
        return mList
    }
    private fun getDue(taskDate: LocalDate):String {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
        val due = currentDate.daysUntil(taskDate)
        return when(due) {
            0 -> "\u001B[103m \u001B[0m"
            in 1..Int.MAX_VALUE -> "\u001B[102m \u001B[0m"
            in Int.MIN_VALUE .. -1 -> "\u001B[101m \u001B[0m"
            else -> ""
        }
    }
    private fun getTaskNumber():Int{
        val max = AllTask.size
        println("Input the task number (1-$max):")
        try {
            inputTaskNo = readln().toInt().also { if (it > max || it<=0) throw java.lang.Exception() }
        } catch (e:Exception) {
            invalidTaskNo()
            getTaskNumber()
        }
        return inputTaskNo!!
    }
    private fun getEditField():String {
        println("Input a field to edit (priority, date, time, task):")
        inputEditField = readln()
        if (inputEditField.lowercase() !in editActions || inputEditField.isEmpty()){
            println("Invalid field")
            getEditField()
        }
        return inputEditField
    }
    fun addAction() {
        val p = getPriority()
        val d1 = getDate()
        val t = getTime()
        val l = getList()
        val d2 = getDue(d1.toLocalDate())
        if (l.isNotEmpty()) AllTask.add(TList(d1,t,p,d2,l))
    }
    fun editAction() {
        printAction()
        val tn = getTaskNumber()-1//tn=task Number
        when (getEditField()) {
            "priority" -> {
                AllTask[tn].priority = getPriority()
                taskChanged()
            }
            "date" -> {
                val date = getDate()
                AllTask[tn].date = date
                AllTask[tn].due = getDue(date.toLocalDate())
                taskChanged()
            }
            "time" -> {
                AllTask[tn].time = getTime()
                taskChanged()
            }
            "task" -> {
                AllTask[tn].list = getList()
                taskChanged()
            }
        }
    }
    fun printAction() {
        taskBar()
        val cList = mutableListOf<String>()
        for (i in AllTask) {
            i.list.forEach { item -> item.split("\n").forEach { cList.addAll(it.chunked(44)) }}
            for (ele in cList.indices) {
                if (ele == 0) println(
                    String.format(
                        "| %1\$d  | %2\$s | %3\$s | %4\$s | %5\$s |%6\$-44s".padEnd(40) + "|",
                        AllTask.indexOf(i)+1,i.date ,i.time,i.priority,i.due, cList[ele]
                    )
                )
                else println("|    |            |       |   |   |${cList[ele]}".padEnd(79) + "|")
            }
            cList.clear()
            endLine()
        }
    }

    fun deleteAction() {
        printAction()
        val tn = getTaskNumber() - 1
        AllTask.removeAt(tn)
        taskDeleted()
    }
    fun storeToFile() {
       jsonFile.writeText(TListAdapter.toJson(AllTask))
    }
    private fun taskBar() {
        println("+----+------------+-------+---+---+--------------------------------------------+\n" +
                "| N  |    Date    | Time  | P | D |                   Task                     |\n" +
                "+----+------------+-------+---+---+--------------------------------------------+")
    }
    private fun endLine() {
        println("+----+------------+-------+---+---+--------------------------------------------+")
    }
}
private fun exitMessage() {
    println("TaskList Exiting!")
}
private fun taskChanged() {
    println("The task is changed")
}
private fun taskDeleted() {
    println("The task is deleted")
}
private fun isClear(l:MutableList<TList>):Boolean {
    return l.size==0
}
private fun noTaskMessage() {
    println("No tasks have been input")
}
private fun invalidTaskNo() {
    println("Invalid task number")
}


class TaskMachine:TaskList
fun main() {
    val t = TaskMachine()
    if (jsonFile.canRead()) TListAdapter.fromJson(jsonFile.readText().trim())!!.forEach { AllTask.add(it!!) }
    do {
        val input = t.startMachine()
        when (input) {
            "add" -> t.addAction()
            "print" ->{
                if (isClear(AllTask)) noTaskMessage() else t.printAction()

            }
            "edit" -> {
                if (isClear(AllTask)) noTaskMessage() else t.editAction()

            }
            "delete" -> {
                if (isClear(AllTask)) noTaskMessage() else t.deleteAction()

            }
            "end" -> {
                t.storeToFile()
                exitMessage()
            }
        }
    } while (input!="end")
}