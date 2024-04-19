package com.example.seeds.utils
import android.app.Application
import android.content.Intent
import android.util.Log
import com.example.seeds.ErrorActivity

class CustomExceptionHandler(private val application: Application) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        // Log the name of the thread and the exception class
        Log.e("UncaughtException", "Uncaught exception in thread: ${thread.name}")
        Log.e("UncaughtException", "Exception Type: ${ex.javaClass.name}")

        // Log the exception message
        val message = ex.message ?: "No message provided"
        Log.e("UncaughtException", "Message: $message")

        // Log the stack trace
        val stackTrace = Log.getStackTraceString(ex)
        if (stackTrace.isNotEmpty()) {
            Log.e("UncaughtException", stackTrace)
        } else {
            Log.e("UncaughtException", "No stack trace available.")
        }

        // Log the cause of the exception, if available
        val cause = ex.cause
        if (cause != null) {
            Log.e("UncaughtException", "Caused by: ${cause.javaClass.name} - ${cause.message}")
        }

        // Log any suppressed exceptions
        ex.suppressed.forEach { suppressedEx ->
            Log.e("UncaughtException", "Suppressed: ${suppressedEx.javaClass.name} - ${suppressedEx.message}")
        }

        // Create an intent to start the ErrorActivity
        val intent = Intent(application, ErrorActivity::class.java).apply {
            putExtra("error", "Thread: ${thread.name}\nException: ${ex.javaClass.name}\nMessage: $message Cause ${ex.cause} \nDetails: ${stackTrace.takeIf { it.isNotEmpty() } ?: "No stack trace available."}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        application.startActivity(intent)

    }
}