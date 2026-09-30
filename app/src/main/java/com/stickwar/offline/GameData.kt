package com.stickwar.offline

object GameData {
    const val W = 1920f
    const val POP_LIMIT = 50
    const val GOLD_REGEN = 2.0f
    val HOME_X = floatArrayOf(130f, W - 130f)
    val MINES = intArrayOf(260, 640, 1280, 1660)
    const val MINE_CAP = 3

    data class UnitDef(
        val cost: Int, val hp: Int, val atk: Int, val speed: Int,
        val range: Int, val rate: Float, val pop: Int,
        val icon: String, val name: String
    )

    val UNITS = mapOf(
        "miner"        to UnitDef(125,  100,  5, 130,   0, 0f,   1, "⛏️", "矿工"),
        "sword"        to UnitDef(125,   80, 12, 110,  60, 1.0f, 1, "🗡️", "剑士"),
        "archer"       to UnitDef(300,   60, 10,  90, 380, 2.0f, 1, "🏹", "弓手"),
        "spearton"     to UnitDef(500,  760, 26,  85,  75, 1.5f, 2, "🔱", "矛兵"),
        "castlearcher" to UnitDef(800,  200, 35,   0, 420, 1.5f, 2, "🏰", "城弓"),
        "magikill"     to UnitDef(1200, 140, 35,  80, 280, 2.5f, 5, "🔮", "法师"),
        "giant"        to UnitDef(1500,1000, 25,  65,  90, 2.5f, 5, "👹", "巨人"),
    )

    data class Upgrade(val id: String, val name: String, val maxLvl: Int, val cost: Int)
    val UPGRADES = listOf(
        Upgrade("miner_pick", "矿工镐", 3, 300),
        Upgrade("miner_bag",  "矿工袋", 3, 300),
        Upgrade("sword_dmg",  "剑士剑", 4, 250),
        Upgrade("sword_hp",   "剑士盔", 3, 250),
        Upgrade("archer_dmg", "弓手箭", 3, 400),
        Upgrade("archer_hp",  "弓手甲", 3, 400),
        Upgrade("spearton_dmg","矛兵矛",3, 600),
        Upgrade("spearton_hp", "矛兵盔",3, 600),
        Upgrade("spearton_shield","矛兵盾",3,600),
        Upgrade("castle_dmg", "城弓箭", 3, 800),
        Upgrade("castle_hp",  "城弓甲", 3, 800),
        Upgrade("mage_dmg",   "法杖",   3, 1200),
        Upgrade("mage_hp",    "法袍",   3, 1200),
        Upgrade("giant_hp",   "巨人血", 3, 1500),
        Upgrade("giant_dmg",  "巨人拳", 3, 1500),
    )

    // 难度参数
    data class Difficulty(
        val aiTickInterval: Float, // AI 决策间隔(秒)
        val aiStartDelay: Float,   // 开局延迟
        val aiUnitPool: List<String>,
        val aiUpgradeChance: Float
    )

    val DIFF_EASY = Difficulty(2.5f, 4.0f, listOf("sword","sword","archer","miner"), 0.05f)
    val DIFF_NORM = Difficulty(1.5f, 2.0f, listOf("sword","sword","archer","spearton","castlearcher","miner"), 0.20f)
    val DIFF_HARD = Difficulty(0.9f, 0.8f, listOf("sword","archer","spearton","castlearcher","magikill","giant","miner"), 0.40f)
}
