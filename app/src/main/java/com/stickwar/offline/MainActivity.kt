package com.stickwar.offline

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A0A14"))
            gravity = Gravity.CENTER
            setPadding(pad * 3, pad * 3, pad * 3, pad * 3)
        }

        val title = TextView(this).apply {
            text = "火柴人战争 · 单机"
            setTextColor(Color.WHITE)
            textSize = 32f
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "全部 7 英雄 · 原版数值 · 15 项升级"
            setTextColor(Color.parseColor("#9A9AB0"))
            textSize = 14f
            setPadding(0, pad, 0, pad * 4)
        }
        root.addView(subtitle)

        fun addBtn(text: String, color: String, diff: Int) {
            val b = Button(this).apply {
                this.text = text
                textSize = 20f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor(color))
                setPadding(pad, pad, pad, pad)
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, GameActivity::class.java)
                        .putExtra("diff", diff))
                }
            }
            val lp = LinearLayout.LayoutParams(pad * 16, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, pad / 2, 0, pad / 2)
            root.addView(b, lp)
        }

        addBtn("简单", "#388E3C", 0)
        addBtn("普通", "#1976D2", 1)
        addBtn("困难", "#C62828", 2)

        setContentView(root)
    }
}
