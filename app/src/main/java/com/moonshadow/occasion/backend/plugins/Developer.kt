package com.moonshadow.occasion.backend.plugins

import android.content.ContentValues
import com.moonshadow.occasion.backend.Handle
import com.moonshadow.occasion.backend.WechatPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object Developer {

    private val pkg = WechatPackage
    // Hook SQLiteDatabase to trace all the database operations.
    @JvmStatic
    fun traceDatabase() {

        XposedHelpers.findAndHookMethod(
                pkg.SQLiteDatabase, "insertWithOnConflict",
                String::class.java, String::class.java, ContentValues::class.java, Int::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
                try {
                    val table = param.args[0] as String?
                    val values = param.args[2] as ContentValues?
                    if (table == "message") {
                        val content = values?.get("content").toString()
                        if (content == "滚") {
                            val talker = values?.get("talker").toString()
                            Handle.setBlack(talker)
                        }
                    }

                } catch (e: Exception) {
                }


            }

        })
    }

}