package com.craftstudio.launcher.ui.activity

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.craftstudio.launcher.databinding.ItemInstallableBinding
import com.craftstudio.launcher.feature.unpack.OnTaskRunningListener

class InstallableAdapter(
    private val items: List<InstallableItem>,
    private val listener: TaskCompletionListener
) : RecyclerView.Adapter<InstallableAdapter.ViewHolder>() {
    @Volatile
    private var completedTasksCount = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val taskProgress = IntArray(items.size) { 0 }

    private var overallProgressListener: OverallProgressListener? = null

    fun setOverallProgressListener(listener: OverallProgressListener) {
        this.overallProgressListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInstallableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun checkAllTask() {
        items.forEachIndexed { index, item ->
            if (!item.task.isNeedUnpack()) {
                item.isFinished = true
                taskProgress[index] = 100
                updateTaskCount(index)
            }
        }
    }

    fun startAllTasks() {
        items.forEachIndexed { index, item ->
            if (!item.isFinished) {
                Thread {
                    item.task.apply {
                        setTaskRunningListener(object : OnTaskRunningListener {
                            override fun onTaskStart() {
                                item.isRunning = true
                                updateUI { notifyItemChanged(index) }
                            }

                            override fun onTaskProgress(progress: Int, fileName: String) {
                                taskProgress[index] = progress
                                val overall = taskProgress.sum() / items.size
                                updateUI {
                                    overallProgressListener?.onOverallProgress(overall, fileName)
                                }
                            }

                            override fun onTaskEnd() {
                                taskProgress[index] = 100
                                item.isRunning = false
                                item.isFinished = true
                                updateTaskCount(index)

                                val overall = taskProgress.sum() / items.size
                                updateUI {
                                    overallProgressListener?.onOverallProgress(overall, "")
                                }
                            }
                        })
                    }
                    item.task.run()
                }.start()
            } else {
                taskProgress[index] = 100
            }
        }
    }

    @Synchronized
    private fun updateTaskCount(index: Int) {
        completedTasksCount++
        updateUI { notifyItemChanged(index) }

        if (completedTasksCount >= itemCount) {
            updateUI { listener.onAllTasksCompleted() }
        }
    }

    private fun updateUI(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post { action() }
        }
    }

    class ViewHolder(
        private val binding: ItemInstallableBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun setData(item: InstallableItem) {
            binding.name.text = item.name

            if (item.summary.isNullOrEmpty()) {
                binding.summary.visibility = View.GONE
            } else {
                binding.summary.text = item.summary
                binding.summary.visibility = View.VISIBLE
            }

            binding.progress.visibility = if (item.isRunning) View.VISIBLE else View.GONE
            binding.finish.visibility = if (item.isFinished) View.VISIBLE else View.GONE
        }
    }

    fun interface TaskCompletionListener {
        fun onAllTasksCompleted()
    }

    fun interface OverallProgressListener {
        fun onOverallProgress(percent: Int, fileName: String)
    }
}
