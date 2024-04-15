package com.example.seeds.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.fragment.findNavController
import com.example.seeds.MainActivity
import com.example.seeds.R
import com.example.seeds.utils.Constants.Companion.APP_VERSION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*


abstract class BaseFragment : Fragment(), LifecycleObserver {

    protected open var bottomNavigationViewVisibility = View.GONE
    var sessionId: String = UUID.randomUUID().toString()
    private var firstTime = true
    private var connectivityManager: ConnectivityManager? = null
    private var isNetworkAvailable = true

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d("", "onLost: ")
            isNetworkAvailable = false
            navigateToNoInternetFragment()
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("", "onAvailable: ")
            if (!isNetworkAvailable) {
                isNetworkAvailable = true
                onInternetRestored()
            }
        }
    }



    //private val networkCallback = object : ConnectivityManager.NetworkCallback() {
//        override fun onLost(network: Network) {
//            super.onLost(network)
//            if (isAdded) {
//                navigateToNoInternetFragment()
//            }
//        }z
//
//        override fun onAvailable(network: Network) {
//            super.onAvailable(network)
//            if (isAdded) {
//                onInternetRestored()
//            }
//        }
//    }

    private fun onInternetRestored() {
        if (isAdded && !isNetworkAvailable) {
            CoroutineScope(Dispatchers.Main).launch {
                findNavController().popBackStack(R.id.noInternetFragment, true)
            }
        }
    }

    private fun navigateToNoInternetFragment() {
        if (isAdded && isNetworkAvailable) {
            CoroutineScope(Dispatchers.Main).launch {
                findNavController().navigate(R.id.action_global_noInternetFragment)
            }
        }
    }

//    private fun onInternetRestored() {
//        if (isAdded) {
//            CoroutineScope(Dispatchers.Main).launch {
//                findNavController().popBackStack()
//            }
//        }
//    }

//    private fun navigateToNoInternetFragment() {
//        if (isAdded) {
//            //findNavController().navigate(R.id.action_global_noInternetFragment)
//        }
//    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //registerNetworkCallback()
    }

    override fun onDestroy() {
        super.onDestroy()
        //unregisterNetworkCallback()
    }

    private fun registerNetworkCallback() {
        connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun unregisterNetworkCallback() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    override fun onStart() {
        //registerNetworkCallback()
        super.onStart()
        if (firstTime) {
            firstTime = false
        } else {
            sessionId = UUID.randomUUID().toString()
        }
    }

    override fun onStop() {
        super.onStop()
        //unregisterNetworkCallback()
    }

//    private fun registerNetworkCallback() {
//        connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
//        val networkRequest = NetworkRequest.Builder().build()
//        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
//    }
//
//    private fun unregisterNetworkCallback() {
//        connectivityManager?.unregisterNetworkCallback(networkCallback)
//    }

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