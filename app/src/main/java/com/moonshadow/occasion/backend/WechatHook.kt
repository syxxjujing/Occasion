package com.moonshadow.occasion.backend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.moonshadow.occasion.Global
import com.moonshadow.occasion.Global.MAGICIAN_PACKAGE_NAME
import com.moonshadow.occasion.Global.tryWithLog
import com.moonshadow.occasion.Global.tryWithThread
import com.moonshadow.occasion.backend.plugins.Developer
import dalvik.system.PathClassLoader
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

class WechatHook : IXposedHookLoadPackage{
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        Global.tryWithLog {
            if (isWechat(lpparam!!)){
                hookApplicationAttach(lpparam.classLoader, { context ->
                    //                        LogTool.d("BuildConfig25--->" + BuildConfig.DEBUG)
                    if (false) {

                        handleLoadWechat(lpparam, context)
                    } else {
                        handleLoadWechatOnFly(lpparam, context)
                    }
                })


            }


        }

    }

    private fun handleLoadWechat(lpparam: XC_LoadPackage.LoadPackageParam, context: Context) {
        //注意：这是kotlin代码，kotlin中"=="相当于java中的"equals"
        if (lpparam.processName=="com.tencent.mm"){

            //在此处注册广播！
            val intentFilter = IntentFilter().apply {
                addAction("action_pull_black")
            }

            context.registerReceiver(myReceiver, intentFilter)
        }

        WechatPackage.init(lpparam, context)
        val pluginDeveloper = Developer
        tryHook(pluginDeveloper::traceDatabase)//HOOK 微信的db
    }

    /**
     * 广播接收器
     * */
    private val myReceiver = object :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action=="action_pull_black"){
                val wxid = intent.getStringExtra("extra_wxid")

                Handle.setBlack(wxid)


            }
        }
    }


    private inline fun hookApplicationAttach(loader: ClassLoader, crossinline callback: (Context) -> Unit) {
        XposedHelpers.findAndHookMethod("android.app.Application", loader, "attach", Context::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                callback(param.thisObject as Context)
            }
        })
    }

    ///data/app/com.keye.operate-1.apk  (apk的路径)
    private fun findAPKPath(context: Context, packageName: String) =
            context.packageManager.getApplicationInfo(packageName, 0).publicSourceDir

    // handleLoadWechatOnFly uses reflection to load updated module without reboot.
    private fun handleLoadWechatOnFly(lpparam: XC_LoadPackage.LoadPackageParam, context: Context) {
        val path = findAPKPath(context, MAGICIAN_PACKAGE_NAME)
        if (!File(path).exists()) {
            XposedBridge.log("Cannot load module on fly: APK not found")
            return
        }
        val pathClassLoader = PathClassLoader(path, ClassLoader.getSystemClassLoader())
        val clazz = Class.forName("$MAGICIAN_PACKAGE_NAME.backend.WechatHook", true, pathClassLoader)
        val method = clazz.getDeclaredMethod("handleLoadWechat", lpparam.javaClass, Context::class.java)
        method.isAccessible = true
        method.invoke(clazz.newInstance(), lpparam, context)
    }

    // isWechat returns true if the current application seems to be Wechat.
    private fun isWechat(lpparam: XC_LoadPackage.LoadPackageParam): Boolean {
        val features = listOf(
                "libwechatcommon.so",
                "libwechatmm.so",
                "libwechatnetwork.so",
                "libwechatsight.so",
                "libwechatxlog.so"
        )

        return try {
            val libraryDir = File(lpparam.appInfo.nativeLibraryDir)
//            val path = libraryDir.path
//            LogTool.d("path63------>" + path)

            val hits = features.filter { filename ->
                File(libraryDir, filename).exists()

            }.size
            (hits.toDouble() / features.size) > 0.5f
        } catch (e: Exception) {
            false
        }


    }


    // NOTE: For Android 7.X or later, multi-thread and lazy initialization
    //       causes unexpected crashes with WeXposed. So I fall back to the
    //       original logic for now.
    private inline fun tryHook(crossinline hook: () -> Unit) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> tryWithLog { hook() }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> tryWithThread { hook() }
            else -> tryWithThread {
                try {
                    hook()
                } catch (t: Throwable) { /* Ignore */
                    XposedBridge.log(t)
                }
            }
        }

    }

}