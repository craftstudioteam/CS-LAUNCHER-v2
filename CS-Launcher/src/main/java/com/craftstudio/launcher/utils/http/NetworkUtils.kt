package com.craftstudio.launcher.utils.http

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkUtils {
    companion object {
        /**
         * @return 当前网络是否已连接
         */
        @JvmStatic
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            return activeNetwork != null && (
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false ||
                            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false ||
                            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ?: false
                    )
        }
    }
}