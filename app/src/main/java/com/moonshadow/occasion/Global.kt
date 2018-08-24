package com.moonshadow.occasion

import android.annotation.SuppressLint
import android.os.Build
import android.os.Environment
import de.robv.android.xposed.XposedBridge
import kotlin.concurrent.thread
object Global {
//    const val XPOSED_CODE = 21807281



    const val XPOSED_PACKAGE_NAME = "de.robv.android.xposed.installer"
    const val XPOSED_FILE_PROVIDER = "de.robv.android.xposed.installer.fileprovider"
    const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
    const val MAGICIAN_PACKAGE_NAME = "com.moonshadow.occasion"

    val storage = Environment.getExternalStorageDirectory().absolutePath + "/WechatMagician"

    val wxCache = Environment.getExternalStorageDirectory().absolutePath+"/tencent/MicroMsg"


    @SuppressLint("SdCardPath")
    private val DATA_DIR = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) "/data/data/" else "/data/user_de/0/"


    fun tryWithLog(func: () -> Unit) {
        try {
            func()
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    fun <T> tryOrNull(func: () -> T): T? =
            try {
                func()
            } catch (t: Throwable) {
                XposedBridge.log(t); null
            }

    fun tryWithThread(func: () -> Unit): Thread {
        return thread(start = true) { func() }.apply {
            setUncaughtExceptionHandler { _, t ->   XposedBridge.log(t);  }
        }
    }
}