import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.locks.ReentrantLock

class TaskQueue<T : TaskQueue.Task> {
    private val taskList = arrayListOf<T>()
    private val taskLock = ReentrantLock()
    private var threadSize = 1
    private var onTaskDoneListener: ((T) -> Unit)? = null
    private var onTaskStartListener: ((T) -> Unit)? = null
    private var onTaskListDoneListener: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.IO)
    private var queueMonitor: Job? = null
    private var isActive = true

    abstract class Task {
        abstract fun start()
        abstract fun pause()
        abstract fun stop()

        abstract fun isDone(): Boolean
        abstract fun isRunning(): Boolean
        abstract fun getTaskId(): Any
    }

    val taskFlow: Flow<List<T>> by lazy {
        flow {
            while (isActive) {
                if (taskLock.isLocked.not()) {
                    emit(ArrayList(taskList))
                    delay(1000)
                }
            }
        }
    }

    fun onTaskDone(listener: (T) -> Unit): TaskQueue<T> {
        this.onTaskDoneListener = listener
        return this
    }

    /**
     * 任务队列执行完毕
     */
    fun onTaskListDone(listener: () -> Unit): TaskQueue<T> {
        this.onTaskListDoneListener = listener
        return this
    }

    fun onTaskStart(listener: (T) -> Unit): TaskQueue<T> {
        this.onTaskStartListener = listener
        return this
    }

    fun getTaskList() = ArrayList(taskList)

    fun hasRunningTask() = taskList.any { it.isRunning() }

    fun getRunningTaskList() = ArrayList(taskList.filter { it.isRunning() })

    fun addTask(task: T): TaskQueue<T> {
        taskLock.lock()
        taskList.add(task)
        taskLock.unlock()
        return this
    }

    fun addTask(task: List<T>): TaskQueue<T> {
        taskLock.lock()
        taskList.addAll(task)
        taskLock.unlock()
        return this
    }

    fun start() {
        queueMonitor?.cancel()
        launchQueueMonitor()
    }

    fun stopAll() {
        taskLock.lock()
        taskList.forEach { it.stop() }
        taskList.clear()
        taskLock.unlock()
    }

    fun pauseAll() {
        taskList.forEach { it.pause() }
    }

    fun pause(task: T) {
        taskList.find { it.getTaskId() == task.getTaskId() }?.pause()
    }

    fun pause(taskId: Any) {
        taskList.find { it.getTaskId() == taskId }?.pause()
    }

    fun stop(task: T) {
        taskList.find { it.getTaskId() == task.getTaskId() }?.stop()
    }

    fun stop(taskId: Any) {
        taskList.find { it.getTaskId() == taskId }?.stop()
    }

    private fun launchQueueMonitor() {
        if (queueMonitor?.isActive != true) {
            println("启动轮循器...")
            queueMonitor = scope.launch {
                while (isActive) {
                    delay(1000)
                    if (taskLock.isLocked.not() && taskList.isNotEmpty()) {
                        val doneTaskList = taskList.filter { it.isDone() }
                        if (doneTaskList.isNotEmpty()) {
                            doneTaskList.forEach {
                                removeDoneTask(it)
                            }
                            startNextTask()
                        }

                        val runningTaskList = taskList.filter { it.isRunning() }
                        if (runningTaskList.isEmpty()) {
                            startNextTask()
                        }
                    }
                }
            }
        }
    }

    private fun removeDoneTask(task: T) {
        taskLock.lock()
        taskList.remove(task)
        taskLock.unlock()
        println("完成任务：${task.getTaskId()}")
        onTaskDoneListener?.invoke(task)
    }

    private fun startNextTask() {
        if (taskList.isEmpty()) {
            onTaskListDoneListener?.invoke()
        } else {
            taskList.forEachIndexed { index, t ->
                if (index < threadSize) {
                    println("启动任务：${t.getTaskId()}")
                    onTaskStartListener?.invoke(t)
                    t.start()
                }
            }
        }
    }


    fun release() {
        taskLock.lock()
        taskList.clear()
        taskLock.unlock()

        queueMonitor?.cancel()
        queueMonitor = null
        scope.cancel()

        onTaskDoneListener = null
        onTaskListDoneListener = null
        onTaskStartListener = null
        isActive = false
    }
}