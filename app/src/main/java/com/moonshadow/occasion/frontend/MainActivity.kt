package com.moonshadow.occasion.frontend

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import com.moonshadow.occasion.Global.tryWithThread
import com.moonshadow.occasion.R
import com.moonshadow.occasion.utils.ShellUtils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_reboot_wechat.setOnClickListener {
            Toast.makeText(this, "正在重启微信,请等待!!!", Toast.LENGTH_SHORT).show()
            tryWithThread {
                ShellUtils.forceAdb("am force-stop com.tencent.mm")
                SystemClock.sleep(5000)
                ShellUtils.forceAdb("am start -n com.tencent.mm/com.tencent.mm.ui.LauncherUI")
                finish()
            }
        }

        btn_pull_black.setOnClickListener {
            val wxid = et_wxid.text.toString()
            if (wxid==""){
                Toast.makeText(this, "wxid不能为空！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendBroadcast(Intent().setAction("action_pull_black").apply {
                putExtra("extra_wxid", wxid)
            })


        }

    }
}
