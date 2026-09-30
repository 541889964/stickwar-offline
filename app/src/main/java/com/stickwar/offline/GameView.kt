package com.stickwar.offline

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

@SuppressLint("ViewConstructor")
class GameView(ctx: Context, val logic: GameLogic) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 32f
    }
    private var lastTime = System.currentTimeMillis()
    private var btnRects: List<RectF> = emptyList()
    private var btnKeys: List<String> = emptyList()
    private var upRects: List<RectF> = emptyList()
    private var upIdx: List<Int> = emptyList()
    private var showUpPanel = false

    init {
        // 60fps 定时器
        post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val dt = ((now - lastTime) / 1000f).coerceAtMost(0.1f)
                lastTime = now
                logic.tick(dt)
                invalidate()
                postDelayed(this, 16)
            }
        })
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val scale = w / GameData.W
        val groundY = h * 0.78f

        // 天空
        paint.shader = LinearGradient(0f, 0f, 0f, groundY,
            Color.parseColor("#0D1B3E"), Color.parseColor("#2C4A7C"), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, groundY, paint)
        paint.shader = null
        paint.color = Color.parseColor("#D9E4F5")
        canvas.drawCircle(w * 0.5f, h * 0.15f, min(w, h) * 0.08f, paint)

        // 地面
        paint.shader = LinearGradient(0f, groundY, 0f, h,
            Color.parseColor("#2D5016"), Color.parseColor("#1A3009"), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, groundY, w, h, paint)
        paint.shader = null

        // 矿点
        for (mx in GameData.MINES) drawMine(canvas, mx * scale, groundY, scale)

        // 雕像
        drawStatue(canvas, 60f, groundY, scale, Color.parseColor("#6C63FF"), logic.statue[0] / 1000f)
        drawStatue(canvas, w - 60f, groundY, scale, Color.parseColor("#FF6584"), logic.statue[1] / 1000f)

        // 单位
        for (u in logic.units.sortedBy { it.x }) drawUnit(canvas, u, groundY, scale)

        drawHud(canvas, w)
        drawButtons(canvas, w, h)
        if (showUpPanel) drawUpgradePanel(canvas, w, h)
        if (logic.winner != -1) {
            textPaint.textSize = 80f
            textPaint.color = Color.WHITE
            textPaint.textAlign = Paint.Align.CENTER
            val t = if (logic.winner == 0) "🏆 胜利" else "💀 失败"
            canvas.drawText(t, w / 2f, h / 2f, textPaint)
        }
    }

    private fun drawMine(c: Canvas, x: Float, baseY: Float, s: Float) {
        paint.color = Color.parseColor("#5D4A26")
        val p = Path()
        p.moveTo(x - 42 * s, baseY)
        p.lineTo(x - 17 * s, baseY - 40 * s)
        p.lineTo(x + 15 * s, baseY - 36 * s)
        p.lineTo(x + 42 * s, baseY)
        p.close()
        c.drawPath(p, paint)
        paint.color = Color.parseColor("#F1C40F")
        for ((dx, dy) in listOf(-0.5f to -0.25f, 0.05f to -0.5f, 0.45f to -0.2f, -0.1f to -0.7f)) {
            c.drawCircle(x + dx * 42 * s, baseY + dy * 42 * s, 5 * s, paint)
        }
    }

    private fun drawStatue(c: Canvas, x: Float, baseY: Float, s: Float, color: Int, ratio: Float) {
        paint.color = color
        val headR = 34 * s
        val headY = baseY - 130 * s * 0.72f
        c.drawCircle(x, headY, headR, paint)
        c.drawRect(x - 34 * s * 0.55f, headY, x + 34 * s * 0.55f, baseY, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5 * s
        paint.color = if (ratio > 0.3f) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        val r = RectF(x - headR - 10 * s, headY - headR - 10 * s, x + headR + 10 * s, headY + headR + 10 * s)
        c.drawArc(r, -90f, 360f * ratio.coerceIn(0f, 1f), false, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawUnit(c: Canvas, u: Unit, groundY: Float, s: Float) {
        val size = when (u.type) {
            "giant" -> 2.2f; "spearton" -> 1.3f; "magikill" -> 1.2f; "sword" -> 1.1f; else -> 1.0f
        }
        val h = 34f * s * size
        val x = u.x * s
        paint.color = 0x66000000
        c.drawOval(RectF(x - h * 0.55f, groundY - 4 * s, x + h * 0.55f, groundY + 6 * s), paint)

        val headR = h * 0.18f
        val headY = groundY - h
        val bodyTop = headY + headR
        val bodyBot = groundY - h * 0.35f
        val shY = bodyTop + h * 0.12f
        val hipY = bodyBot

        val col = when (u.type) {
            "miner" -> Color.parseColor("#C9A227")
            "sword" -> Color.parseColor("#C0392B")
            "archer" -> Color.parseColor("#2874A6")
            "spearton" -> Color.parseColor("#7B241C")
            "castlearcher" -> Color.parseColor("#1E8449")
            "magikill" -> Color.parseColor("#6C3483")
            "giant" -> Color.parseColor("#117A65")
            else -> Color.WHITE
        }
        paint.color = col
        paint.strokeWidth = maxOf(2f, h * 0.08f)
        paint.style = Paint.Style.FILL
        c.drawCircle(x, headY, headR, paint)
        paint.style = Paint.Style.STROKE
        c.drawLine(x, bodyTop, x, bodyBot, paint)
        c.drawLine(x, hipY, x - h * 0.15f, groundY, paint)
        c.drawLine(x, hipY, x + h * 0.15f, groundY, paint)
        val dir = if (u.owner == 0) 1f else -1f
        c.drawLine(x, shY, x - dir * h * 0.22f, shY + h * 0.16f, paint)
        c.drawLine(x, shY, x + dir * h * 0.22f, shY + h * 0.19f, paint)
        paint.style = Paint.Style.FILL

        if (u.hp < u.maxHp) {
            val bw = h * 1.2f
            paint.color = 0xAA000000.toInt()
            c.drawRect(x - bw / 2, groundY - h * 2.2f, x + bw / 2, groundY - h * 2.0f, paint)
            paint.color = when {
                u.hp / u.maxHp > 0.5f -> Color.parseColor("#4CAF50")
                u.hp / u.maxHp > 0.25f -> Color.parseColor("#FFC107")
                else -> Color.parseColor("#F44336")
            }
            c.drawRect(x - bw / 2, groundY - h * 2.2f, x - bw / 2 + bw * (u.hp / u.maxHp), groundY - h * 2.0f, paint)
        }
        if (u.owner == 0) {
            paint.style = Paint.Style.STROKE
            paint.color = 0xCCFFFFFF.toInt()
            paint.strokeWidth = 3f
            c.drawCircle(x, groundY - h, h * 0.55f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawHud(c: Canvas, w: Float) {
        textPaint.textSize = 34f
        textPaint.textAlign = Paint.Align.LEFT
        paint.color = 0x99000000.toInt()
        c.drawRect(0f, 0f, w, 80f, paint)
        textPaint.color = Color.parseColor("#FFD54F")
        c.drawText("💰 ${logic.myGold().toInt()}  👥 ${logic.myPop()}/50", 20f, 50f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        c.drawText("敌方 👥 ${logic.enemyPop()}/50", w - 20f, 50f, textPaint)
        paint.color = Color.parseColor("#4CAF50")
        c.drawRect(20f, 62f, 20f + (w / 2f - 40f) * (logic.statue[0] / 1000f), 70f, paint)
        paint.color = Color.parseColor("#F44336")
        c.drawRect(w / 2f + 20f, 62f, w / 2f + 20f + (w / 2f - 40f) * (logic.statue[1] / 1000f), 70f, paint)
    }

    private fun drawButtons(c: Canvas, w: Float, h: Float) {
        val keys = listOf("miner","sword","archer","spearton","castlearcher","magikill","giant")
        val n = keys.size + 1
        val bw = w / n
        val bh = 130f
        val top = h - bh - 10f
        btnRects = ArrayList()
        btnKeys = keys + "upgrade"
        for ((i, k) in btnKeys.withIndex()) {
            val r = RectF(i * bw + 4f, top, (i + 1) * bw - 4f, top + bh)
            btnRects.add(r)
            paint.color = 0xEE1E1E30.toInt()
            c.drawRoundRect(r, 16f, 16f, paint)
            textPaint.textAlign = Paint.Align.CENTER
            if (k == "upgrade") {
                textPaint.color = Color.parseColor("#FFD54F")
                textPaint.textSize = 40f
                c.drawText("⬆️", r.centerX(), r.top + 56f, textPaint)
                textPaint.textSize = 24f
                c.drawText("升级", r.centerX(), r.top + 100f, textPaint)
            } else {
                val d = GameData.UNITS[k]!!
                textPaint.color = Color.WHITE
                textPaint.textSize = 42f
                c.drawText(d.icon, r.centerX(), r.top + 52f, textPaint)
                textPaint.textSize = 22f
                c.drawText(d.name, r.centerX(), r.top + 88f, textPaint)
                textPaint.textSize = 22f
                textPaint.color = if (logic.myGold() >= d.cost) Color.parseColor("#FFD54F")
                                  else Color.parseColor("#666666")
                c.drawText("${d.cost}", r.centerX(), r.top + 118f, textPaint)
            }
        }
    }

    private fun drawUpgradePanel(c: Canvas, w: Float, h: Float) {
        paint.color = 0xEE0A0A14.toInt()
        val pt = h * 0.10f; val pb = h * 0.90f
        c.drawRect(0f, pt, w, pb, paint)
        textPaint.color = Color.WHITE
        textPaint.textSize = 40f
        textPaint.textAlign = Paint.Align.CENTER
        c.drawText("升级英雄 (金币: ${logic.myGold().toInt()})", w / 2f, pt + 55f, textPaint)
        val cols = 3
        val rows = (GameData.UPGRADES.size + cols - 1) / cols
        val cw = w / cols
        val ch = (pb - pt - 80f) / rows
        upRects = ArrayList()
        upIdx = ArrayList()
        val myUps = logic.myUpgradesMap()
        for ((i, up) in GameData.UPGRADES.withIndex()) {
            val col = i % cols; val row = i / cols
            val r = RectF(col * cw + 8f, pt + 80f + row * ch,
                          (col + 1) * cw - 8f, pt + 80f + (row + 1) * ch - 8f)
            upRects.add(r); upIdx.add(i)
            val lvl = myUps[up.id] ?: 0
            val maxed = lvl >= up.maxLvl
            val can = !maxed && logic.myGold() >= up.cost
            paint.color = when {
                maxed -> 0xEE2E7D32.toInt()
                can -> 0xEE3A3A5A.toInt()
                else -> 0xEE1A1A2E.toInt()
            }
            c.drawRoundRect(r, 12f, 12f, paint)
            textPaint.color = Color.WHITE
            textPaint.textSize = 26f
            c.drawText(up.name, r.centerX(), r.top + 36f, textPaint)
            textPaint.textSize = 22f
            textPaint.color = Color.parseColor("#B0B0C3")
            c.drawText("Lv $lvl / ${up.maxLvl}", r.centerX(), r.top + 66f, textPaint)
            if (!maxed) {
                textPaint.color = if (can) Color.parseColor("#FFD54F") else Color.parseColor("#666666")
                c.drawText("💰 ${up.cost}", r.centerX(), r.top + 96f, textPaint)
            } else {
                textPaint.color = Color.parseColor("#81C784")
                c.drawText("已满级", r.centerX(), r.top + 96f, textPaint)
            }
        }
        textPaint.color = Color.WHITE
        textPaint.textSize = 40f
        textPaint.textAlign = Paint.Align.RIGHT
        c.drawText("✕", w - 30f, pt + 55f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        val x = event.x; val y = event.y
        val w = width.toFloat(); val h = height.toFloat()
        if (showUpPanel) {
            if (x > w - 80f && y < h * 0.10f + 60f) { showUpPanel = false; return true }
            for ((i, r) in upRects.withIndex()) {
                if (r.contains(x, y)) {
                    val up = GameData.UPGRADES[upIdx[i]]
                    logic.upgrade(0, up.id)
                    return true
                }
            }
            if (y < h * 0.10f || y > h * 0.90f) showUpPanel = false
            return true
        }
        for ((i, r) in btnRects.withIndex()) {
            if (r.contains(x, y)) {
                val k = btnKeys[i]
                if (k == "upgrade") showUpPanel = true
                else logic.spawn(0, k)
                return true
            }
        }
        return true
    }
}
