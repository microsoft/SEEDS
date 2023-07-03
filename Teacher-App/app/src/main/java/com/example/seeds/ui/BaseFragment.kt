package com.example.seeds.ui

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.seeds.MainActivity
import com.example.seeds.utils.Constants.Companion.APP_VERSION
import timber.log.Timber
import java.util.*


abstract class BaseFragment : Fragment(), LifecycleObserver {

    protected open var bottomNavigationViewVisibility = View.GONE
    var sessionId: String = UUID.randomUUID().toString()
    private var firstTime = true

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreated(){
        activity?.lifecycle?.removeObserver(this)
        if (activity is MainActivity) {
            val  mainActivity = activity as MainActivity
            mainActivity.setBottomNavigationVisibility(bottomNavigationViewVisibility)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.lifecycle?.addObserver(this)
    }

    override fun onResume() {
        super.onResume()
        if (activity is MainActivity) {
            val  mainActivity = activity as MainActivity
            mainActivity.setBottomNavigationVisibility(bottomNavigationViewVisibility)
        }
    }

    override fun onStart() {
        super.onStart()
        if (firstTime) {
            firstTime = false
        } else {
            sessionId = UUID.randomUUID().toString()
        }
    }

    fun logMessage(msg: String) {
        if (activity is MainActivity) {
            val mainActivitySessionId = (activity as MainActivity).mainActivitySessionId
            Timber.tag(this.javaClass.simpleName).d("Appv$APP_VERSION $mainActivitySessionId $sessionId $msg")
        }
        else {
            Timber.tag(this.javaClass.simpleName).d("Appv$APP_VERSION $sessionId $msg")
        }
    }
}