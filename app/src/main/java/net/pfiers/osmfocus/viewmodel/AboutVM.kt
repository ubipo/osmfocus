package net.pfiers.osmfocus.viewmodel

import androidx.lifecycle.ViewModel
import net.pfiers.osmfocus.extensions.createVMFactory

class AboutVM(private val navigator: Navigator) : ViewModel() {
    //https://github.com/android/architecture-samples/blob/todo-mvvm-databinding/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/tasks/TasksNavigator.java
    interface Navigator {
        fun showSourceCode()
        fun showAppInfo()
        fun showDonationOptions()
        fun showIssueTracker()
    }

    fun showSourceCode() = navigator.showSourceCode()
    fun showAppInfo() = navigator.showAppInfo()
    fun showDonationOptions() = navigator.showDonationOptions()
    fun showIssueTracker() = navigator.showIssueTracker()

    companion object {
        fun createFactory(creator: () -> AboutVM) = createVMFactory(creator)
    }
}
