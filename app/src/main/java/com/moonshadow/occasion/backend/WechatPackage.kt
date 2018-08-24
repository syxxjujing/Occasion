package com.moonshadow.occasion.backend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.BaseAdapter
import com.moonshadow.occasion.BuildConfig
import com.moonshadow.occasion.Global.tryOrNull
import com.moonshadow.occasion.Global.tryWithLog
import com.moonshadow.occasion.Global.tryWithThread
import com.moonshadow.occasion.utils.PackageUtil
import com.moonshadow.occasion.utils.PackageUtil.findClassIfExists
import com.moonshadow.occasion.utils.PackageUtil.findClassesFromPackage
import com.moonshadow.occasion.utils.PackageUtil.findFieldsWithGenericType
import com.moonshadow.occasion.utils.PackageUtil.findFieldsWithType
import com.moonshadow.occasion.utils.Version
import com.moonshadow.occasion.utils.WaitChannel
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import net.dongliu.apk.parser.ApkFile
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// WechatPackage analyzes and stores critical classes and objects in Wechat application.
// These classes and objects will be used for hooking and tampering with runtime data.
object WechatPackage {

    // initializeChannel resumes all the thread waiting for the WechatPackage initialization.
    private val initializeChannel = WaitChannel()

    // status stores the working status of all the hooks.
    private val statusLock = ReentrantReadWriteLock()
    private val status: HashMap<String, Boolean> = hashMapOf()

    // These stores necessary information to match signatures.
    @Volatile
    var packageName: String = ""
    @Volatile
    var loader: ClassLoader? = null
    @Volatile
    var version: Version? = null
    @Volatile
    var classes: List<String>? = null


    private fun <T> innerLazy(name: String, initializer: () -> T?): Lazy<T> = lazy {
        initializeChannel.wait()
        initializer() ?: throw Error("Failed to evaluate $name")
    }

    private val WECHAT_PACKAGE_SQLITE: String by innerLazy("WECHAT_PACKAGE_SQLITE") {
        when {
            version!! >= Version("6.5.8") -> "com.tencent.wcdb"
            else -> "com.tencent.mmdb"
        }
    }

    val SQLiteDatabase: Class<*> by innerLazy("SQLiteDatabase") {
        findClassIfExists("$WECHAT_PACKAGE_SQLITE.database.SQLiteDatabase", loader)
    }
    // init initializes necessary information for static analysis.
    fun init(lpparam: XC_LoadPackage.LoadPackageParam, context: Context) {
        tryWithThread {
            try {
                packageName = lpparam.packageName
                loader = lpparam.classLoader
                version = getVersion(lpparam)
                //在此处可以做版本适配

                var apkFile: ApkFile? = null
                try {
                    apkFile = ApkFile(lpparam.appInfo.sourceDir)
                    classes = apkFile.dexClasses.map { clazz ->
                        PackageUtil.getClassName(clazz)
                    }
                } finally {
                    apkFile?.close()
                }
            } catch (t: Throwable) {
                // Ignore this one
            } finally {
                initializeChannel.done()
            }
        }
    }



    @Volatile
    var mmContext: WeakReference<Context?> = WeakReference(null)

    // getVersion returns the version of current package / application
    private fun getVersion(lpparam: XC_LoadPackage.LoadPackageParam): Version {
        val activityThreadClass = findClass("android.app.ActivityThread", null)
        val activityThread = callStaticMethod(activityThreadClass, "currentActivityThread")
        mmContext = WeakReference(callMethod(activityThread, "getSystemContext") as Context?)
        val versionName = mmContext.get()?.packageManager?.getPackageInfo(lpparam.packageName, 0)?.versionName
        return Version(versionName ?: throw Error("Cannot get Wechat version"))
    }


    override fun toString(): String {
        val body = tryOrNull {
            this.javaClass.declaredFields.filter { field ->
                when (field.name) {
                    "INSTANCE", "\$\$delegatedProperties",
                    "initializeChannel",
                    "status", "statusLock",
                    "packageName", "loader", "version", "classes",
                    "WECHAT_PACKAGE_SQLITE",
                    "WECHAT_PACKAGE_UI",
                    "WECHAT_PACKAGE_SNS_UI",
                    "WECHAT_PACKAGE_GALLERY_UI" -> false
                    else -> true
                }
            }.joinToString("\n") {
                it.isAccessible = true
                val key = it.name.removeSuffix("\$delegate")
                var value = it.get(this)
                if (value is WeakReference<*>) {
                    value = value.get()
                }
                "$key = $value"
            }
        }

        return """====================================================
Wechat Package: ${packageName}
Wechat Version: ${version}
Module Version: ${BuildConfig.VERSION_NAME}
${body?.removeSuffix("\n") ?: "Failed to generate report."}
===================================================="""
    }
}