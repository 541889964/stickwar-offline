package com.stickwar.offline

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diffId = intent.getIntExtra("diff", 1)
        val diff = when (diffId) {
            0 -> GameData.DIFF_EASY
            2 -> GameData.DIFF_HARD
            else -> GameData.DIFF_NORM
        }
        val logic = GameLogic(diff)
        setContentView(GameView(this, logic))
    }
}
