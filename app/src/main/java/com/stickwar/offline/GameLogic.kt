package com.stickwar.offline

import kotlin.math.abs
import kotlin.random.Random

data class Unit(
    var id: Int, var owner: Int, var type: String,
    var x: Float, var hp: Float, var maxHp: Float,
    var cd: Float = 0f,
    var state: String = "",   // 矿工用
    var mt: Float = 0f,       // 挖矿进度
    var mine: Int = -1,       // 目标矿点
    var carry: Float = 0f,
    var mineSpeed: Float = 1f,
    var bag: Float = 25f
)

class GameLogic(val diff: GameData.Difficulty) {
    var units = ArrayList<Unit>()
    var statue = floatArrayOf(1000f, 1000f)
    var gold = floatArrayOf(200f, 200f)
    var ups = arrayOf(HashMap<String, Int>(), HashMap<String, Int>())
    var combo = intArrayOf(0, 0)
    var winner = -1
    var aiTimer = 0f
    private var nid = 0

    fun popOf(p: Int) = units.filter { it.owner == p }.sumOf { GameData.UNITS[it.type]!!.pop }

    fun unitStats(p: Int, kind: String): GameData.UnitDef {
        val base = GameData.UNITS[kind]!!
        val u = ups[p]
        val atkM = when (kind) {
            "sword"        -> 1f + 0.2f * (u["sword_dmg"] ?: 0)
            "archer"       -> 1f + 0.25f * (u["archer_dmg"] ?: 0)
            "spearton"     -> 1f + 0.2f * (u["spearton_dmg"] ?: 0)
            "castlearcher" -> 1f + 0.2f * (u["castle_dmg"] ?: 0)
            "magikill"     -> 1f + 0.2f * (u["mage_dmg"] ?: 0)
            "giant"        -> 1f + 0.2f * (u["giant_dmg"] ?: 0)
            else -> 1f
        }
        val hpM = when (kind) {
            "sword"        -> 1f + 0.2f * (u["sword_hp"] ?: 0)
            "archer"       -> 1f + 0.2f * (u["archer_hp"] ?: 0)
            "spearton"     -> 1f + 0.2f * (u["spearton_hp"] ?: 0)
            "castlearcher" -> 1f + 0.2f * (u["castle_hp"] ?: 0)
            "magikill"     -> 1f + 0.2f * (u["mage_hp"] ?: 0)
            "giant"        -> 1f + 0.3f * (u["giant_hp"] ?: 0)
            else -> 1f
        }
        return base.copy(atk = (base.atk * atkM).toInt(), hp = (base.hp * hpM).toInt())
    }

    fun spawn(p: Int, kind: String): Boolean {
        if (winner != -1) return false
        val def = GameData.UNITS[kind] ?: return false
        if (gold[p] < def.cost) return false
        if (popOf(p) + def.pop > GameData.POP_LIMIT) return false
        gold[p] -= def.cost
        nid += 1
        val st = unitStats(p, kind)
        val u = Unit(nid, p, kind, GameData.HOME_X[p], st.hp.toFloat(), st.hp.toFloat())
        if (kind == "miner") {
            u.state = "go"
            u.mineSpeed = 1f + 0.2f * (ups[p]["miner_pick"] ?: 0)
            u.bag = 25f * (1f + 0.2f * (ups[p]["miner_bag"] ?: 0))
        }
        units.add(u)
        return true
    }

    fun upgrade(p: Int, id: String): Boolean {
        val info = GameData.UPGRADES.find { it.id == id } ?: return false
        val cur = ups[p][id] ?: 0
        if (cur >= info.maxLvl) return false
        if (gold[p] < info.cost) return false
        gold[p] -= info.cost
        ups[p][id] = cur + 1
        return true
    }

    fun reset() {
        units.clear()
        statue = floatArrayOf(1000f, 1000f)
        gold = floatArrayOf(200f, 200f)
        ups = arrayOf(HashMap(), HashMap())
        combo = intArrayOf(0, 0)
        winner = -1
        nid = 0
        aiTimer = diff.aiStartDelay
    }

    private fun mineOcc(mi: Int) = units.count { it.type == "miner" && it.mine == mi }

    private fun pickMine(p: Int): Int {
        var best = -1; var bd = Float.MAX_VALUE
        for (i in (if (p == 0) intArrayOf(0, 1) else intArrayOf(2, 3))) {
            if (mineOcc(i) >= GameData.MINE_CAP) continue
            val d = abs(GameData.MINES[i] - GameData.HOME_X[p])
            if (d < bd) { bd = d; best = i }
        }
        return best
    }

    private fun tickMiner(u: Unit, dt: Float) {
        val sp = GameData.UNITS["miner"]!!.speed.toFloat()
        when (u.state) {
            "go" -> {
                if (u.mine < 0 || mineOcc(u.mine) > GameData.MINE_CAP) u.mine = pickMine(u.owner)
                if (u.mine < 0) return
                val t = GameData.MINES[u.mine].toFloat()
                val d = t - u.x
                if (abs(d) < 6f) { u.state = "mine"; u.mt = 2.5f / u.mineSpeed }
                else u.x += (if (d > 0) 1 else -1) * sp * dt
            }
            "mine" -> {
                u.mt -= dt
                if (u.mt <= 0f) { u.state = "back"; u.carry = u.bag }
            }
            "back" -> {
                val t = GameData.HOME_X[u.owner]
                val d = t - u.x
                if (abs(d) < 6f) {
                    gold[u.owner] = (gold[u.owner] + u.carry).coerceAtMost(99999f)
                    u.carry = 0f; u.mine = -1; u.state = "go"
                } else u.x += (if (d > 0) 1 else -1) * sp * dt
            }
        }
    }

    private fun tickFighter(u: Unit, dt: Float) {
        val st = unitStats(u.owner, u.type)
        val enemy = 1 - u.owner
        var best: Unit? = null; var bd = Float.MAX_VALUE
        for (o in units) {
            if (o === u || o.owner == u.owner || o.hp <= 0f || o.type == "miner") continue
            val d = abs(o.x - u.x)
            if (d < bd) { bd = d; best = o }
        }
        val ds = abs(GameData.HOME_X[enemy] - u.x)
        val useStatue = best == null || ds < bd
        val td = if (useStatue) ds else bd
        u.cd -= dt
        if (td <= st.range) {
            if (u.cd <= 0f) {
                u.cd = st.rate
                if (useStatue) statue[enemy] -= st.atk
                else best!!.hp -= st.atk
            }
        } else {
            if (st.speed <= 0) return
            val dir = if (u.owner == 0) 1 else -1
            u.x += dir * st.speed * dt
        }
    }

    private fun onKill(p: Int) {
        combo[p] += 1
        gold[p] = (gold[p] + 10f * (1f + combo[p] * 0.1f)).coerceAtMost(99999f)
    }

    private fun tickAI(dt: Float) {
        aiTimer -= dt
        if (aiTimer > 0f) return
        aiTimer = diff.aiTickInterval
        // 保持 3 个矿工
        val miners = units.count { it.owner == 1 && it.type == "miner" }
        if (miners < 3 && gold[1] >= 125f) { spawn(1, "miner"); return }
        // 随机出兵
        val kind = diff.aiUnitPool.random()
        spawn(1, kind)
        // 随机升级
        if (Random.nextFloat() < diff.aiUpgradeChance) {
            val u = GameData.UPGRADES.random()
            upgrade(1, u.id)
        }
    }

    fun tick(dt: Float) {
        if (winner != -1) return
        for (i in 0..1) gold[i] = (gold[i] + GameData.GOLD_REGEN * dt).coerceAtMost(99999f)
        for (u in units) {
            if (u.hp <= 0f) continue
            if (u.type == "miner") tickMiner(u, dt)
            else tickFighter(u, dt)
        }
        // 处理击杀
        val dead = units.filter { it.hp <= 0f && it.type != "miner" }
        for (d in dead) {
            // 找出谁杀的(近似: 攻击方最后一个击中者未知，简单统计)
        }
        units.removeAll { it.hp <= 0f }
        if (statue[0] <= 0f) { winner = 1; statue[0] = 0f }
        else if (statue[1] <= 0f) { winner = 0; statue[1] = 0f }
        tickAI(dt)
    }

    // 供 UI 显示用(玩家0看自己)
    fun myUpgradesMap(): Map<String, Int> = ups[0]
    fun myGold(): Float = gold[0]
    fun myPop(): Int = popOf(0)
    fun enemyPop(): Int = popOf(1)
}
