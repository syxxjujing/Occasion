package com.moonshadow.occasion.backend

import de.robv.android.xposed.XposedHelpers

object Handle{

    /**
     * 拉黑(微信6.6.5)
     * */
    fun setBlack(username: String) {
        val clazz = XposedHelpers.findClass("com.tencent.mm.storage.x", WechatPackage.loader)

        val obj = XposedHelpers.newInstance(clazz, "")
        XposedHelpers.callMethod(obj, "setUsername", username)


        val clazz2 = XposedHelpers.findClass("com.tencent.mm.y.s", WechatPackage.loader)
        XposedHelpers.callStaticMethod(clazz2, "h", obj)
    }

}