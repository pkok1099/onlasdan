package com.onlasdan.comic.ui.components.pulltorefresh

import androidx.compose.ui.graphics.Color
import com.onlasdan.comic.ui.theme.ComicSfxCyan
import com.onlasdan.comic.ui.theme.ComicSfxOrange
import com.onlasdan.comic.ui.theme.ComicSfxPink
import com.onlasdan.comic.ui.theme.ComicSfxPurple
import com.onlasdan.comic.ui.theme.ComicSfxRose
import com.onlasdan.comic.ui.theme.ComicSfxYellow
import kotlin.random.Random

enum class ComicSfxAccent(val color: Color) {
    PINK(ComicSfxPink),
    ROSE(ComicSfxRose),
    CYAN(ComicSfxCyan),
    YELLOW(ComicSfxYellow),
    PURPLE(ComicSfxPurple),
    ORANGE(ComicSfxOrange),
}

data class ComicSfx(
    val id: String,
    val origin: String,
    val chars: List<String>,
    val sublabel: String,
    val burstTitle: String,
    val burstMeaning: String,
    val accent: ComicSfxAccent = ComicSfxAccent.PINK,
    val stamps: List<String> = emptyList(),
)

object ComicSfxCatalog {

    val MANGA_VARIANTS = listOf(
        ComicSfx(
            id = "jp-dododo",
            origin = "🇯🇵 Manga",
            chars = listOf("ド", "ド", "ド"),
            sublabel = "DODODO • GEMURUH MENARIK",
            burstTitle = "ドンッ!",
            burstMeaning = "DON! • IMPACT STRIKE",
            accent = ComicSfxAccent.PINK,
            stamps = listOf("⚡ DON!", "✦ MENACING", "💥 BAM!", "🔥 HYPE"),
        ),
        ComicSfx(
            id = "jp-gogogo",
            origin = "🇯🇵 Manga",
            chars = listOf("ゴ", "ゴ", "ゴ"),
            sublabel = "GOGOGO • AURA MENGINTIMIDASI",
            burstTitle = "ズバッ!",
            burstMeaning = "ZUBAT! • TEBASAN KILAT",
            accent = ComicSfxAccent.PURPLE,
            stamps = listOf("👁️ AURA", "⚔️ SLASH!", "✦ ZUBAT", "⚡ DARK"),
        ),
        ComicSfx(
            id = "jp-biriri",
            origin = "🇯🇵 Manga",
            chars = listOf("ビ", "リ", "リ"),
            sublabel = "BIRIRI • SENGATAN LISTRIK",
            burstTitle = "バァン!",
            burstMeaning = "BAAN! • LEDAKAN PETIR",
            accent = ComicSfxAccent.CYAN,
            stamps = listOf("⚡ VOLT!", "✦ SPARK", "💥 BAAN!", "🔋 999%"),
        ),
        ComicSfx(
            id = "jp-dokidoki",
            origin = "🇯🇵 Manga",
            chars = listOf("ド", "キ", "ッ"),
            sublabel = "DOKI! • DETAK JANTUNG",
            burstTitle = "キュン!",
            burstMeaning = "KYUN! • CRITICAL BLUSH",
            accent = ComicSfxAccent.ROSE,
            stamps = listOf("💖 DOKI!", "🌸 KYUN", "✨ BLUSH", "💘 100%"),
        ),
        ComicSfx(
            id = "jp-zaza",
            origin = "🇯🇵 Manga",
            chars = listOf("ザ", "ワ", "ッ"),
            sublabel = "ZAWA... • KETEGANGAN PUNCAK",
            burstTitle = "ガシャン!",
            burstMeaning = "GASHAN! • SHATTER BREAK",
            accent = ComicSfxAccent.YELLOW,
            stamps = listOf("🎲 ZAWA", "⚡ TENSION", "💥 BREAK!", "🃏 ALL-IN"),
        ),
        ComicSfx(
            id = "jp-meramera",
            origin = "🇯🇵 Manga",
            chars = listOf("メ", "ラ", "ッ"),
            sublabel = "MERA-MERA • KOBARAN API",
            burstTitle = "ドカァン!",
            burstMeaning = "DOKAAN! • INFERNO BLAST",
            accent = ComicSfxAccent.ORANGE,
            stamps = listOf("🔥 MERA!", "💥 BLAZE", "⚡ ULTRA", "🌋 BOOM!"),
        ),
        ComicSfx(
            id = "jp-shun",
            origin = "🇯🇵 Manga",
            chars = listOf("シ", "ュ", "ン"),
            sublabel = "SHUN! • LANGKAH BAYANGAN",
            burstTitle = "ピキーン!",
            burstMeaning = "PIKIIN! • INSTING TAJAM",
            accent = ComicSfxAccent.CYAN,
            stamps = listOf("💨 STEP!", "⚡ FLASH", "🗡️ SHUN", "✨ SENSE"),
        ),
        ComicSfx(
            id = "jp-baki",
            origin = "🇯🇵 Manga",
            chars = listOf("バ", "キ", "ッ"),
            sublabel = "BAKI! • RETAKAN KEKUATAN",
            burstTitle = "ドゴォン!",
            burstMeaning = "DOGOON! • PUKULAN MAUT",
            accent = ComicSfxAccent.PINK,
            stamps = listOf("👊 SMASH!", "💥 BAKI!", "⚡ HEAVY", "🔥 KO!"),
        ),
    )

    val MANHWA_VARIANTS = listOf(
        ComicSfx(
            id = "kr-dudung",
            origin = "🇰🇷 Manhwa",
            chars = listOf("두", "둥", "!"),
            sublabel = "DU-DUNG • AWAKENING HUNTER",
            burstTitle = "콰아아!",
            burstMeaning = "KWAAA! • MANA ERUPTION",
            accent = ComicSfxAccent.PINK,
            stamps = listOf("👑 S-RANK", "⚡ MANA!", "🔥 AWAKEN", "✦ SYSTEM"),
        ),
        ComicSfx(
            id = "kr-kungkung",
            origin = "🇰🇷 Manhwa",
            chars = listOf("쿵", "쿵", "쿵"),
            sublabel = "KUNG-KUNG • LANGKAH BOSS",
            burstTitle = "번쩍!",
            burstMeaning = "BEON-JJEOK! • CRITICAL FLASH",
            accent = ComicSfxAccent.CYAN,
            stamps = listOf("⚡ CRIT!", "💀 BOSS", "💥 KUNG!", "🗡️ RAID"),
        ),
        ComicSfx(
            id = "kr-syuuk",
            origin = "🇰🇷 Manhwa",
            chars = listOf("슈", "우", "욱"),
            sublabel = "SYU-UK • GERAKAN KILAT",
            burstTitle = "파앗!",
            burstMeaning = "PA-AT! • AURA SHOCKWAVE",
            accent = ComicSfxAccent.YELLOW,
            stamps = listOf("💨 DASH!", "⚡ PA-AT!", "✦ SPEED", "🔥 COMBO"),
        ),
        ComicSfx(
            id = "kr-jijik",
            origin = "🇰🇷 Manhwa",
            chars = listOf("찌", "지", "직"),
            sublabel = "JJI-JIK • PETIR HITAM",
            burstTitle = "콰직!",
            burstMeaning = "KWA-JIK! • ARMOR CRUSH",
            accent = ComicSfxAccent.PURPLE,
            stamps = listOf("⚡ SPARK", "💥 CRUSH!", "🌑 SHADOW", "👑 ARISE!"),
        ),
        ComicSfx(
            id = "kr-ugeul",
            origin = "🇰🇷 Manhwa",
            chars = listOf("우", "르", "릉"),
            sublabel = "U-REU-REUNG • DUNGEON GEMPA",
            burstTitle = "쾅!",
            burstMeaning = "KWANG! • METEOR SLAM",
            accent = ComicSfxAccent.ORANGE,
            stamps = listOf("☄️ KWANG!", "🌋 TREMOR", "⚡ GATE", "🔥 SSS!"),
        ),
        ComicSfx(
            id = "kr-beonjjeok",
            origin = "🇰🇷 Manhwa",
            chars = listOf("번", "쩍", "!"),
            sublabel = "BEON-JJEOK • MATA BERSINAR",
            burstTitle = "챙!",
            burstMeaning = "CHAENG! • PARRY SEMPURNA",
            accent = ComicSfxAccent.CYAN,
            stamps = listOf("👁️ GLOW!", "⚔️ PARRY!", "✦ CLASH", "⚡ SKILL"),
        ),
        ComicSfx(
            id = "kr-dugeun",
            origin = "🇰🇷 Manhwa",
            chars = listOf("두", "근", "♡"),
            sublabel = "DU-GEUN • PESONA VILLAINESS",
            burstTitle = "샤랄라~",
            burstMeaning = "SYA-RAL-LA! • ROYAL CHARM",
            accent = ComicSfxAccent.ROSE,
            stamps = listOf("🌹 CHARM", "💖 DUGEUN", "✨ CROWN", "💎 VIP"),
        ),
        ComicSfx(
            id = "kr-hwaryeok",
            origin = "🇰🇷 Manhwa",
            chars = listOf("화", "르", "륵"),
            sublabel = "HWA-REU-REUK • API MURIM",
            burstTitle = "퍼엉!",
            burstMeaning = "PEO-EONG! • DEMONIC BLAST",
            accent = ComicSfxAccent.YELLOW,
            stamps = listOf("🌸 MURIM", "🔥 FLAME!", "💥 BLAST", "⚔️ SECT"),
        ),
    )

    val MANHUA_VARIANTS = listOf(
        ComicSfx(
            id = "cn-honglong",
            origin = "🇨🇳 Manhua",
            chars = listOf("轰", "隆", "隆"),
            sublabel = "HŌNG LÓNG • PETIR KESENGSARAAN",
            burstTitle = "破!",
            burstMeaning = "PÒ! • REALM BREAKTHROUGH",
            accent = ComicSfxAccent.YELLOW,
            stamps = listOf("⚡ TRIBULATION", "🐉 PÒ!", "✨ IMMORTAL", "🔥 LEVEL UP"),
        ),
        ComicSfx(
            id = "cn-sousou",
            origin = "🇨🇳 Manhua",
            chars = listOf("嗖", "嗖", "嗖"),
            sublabel = "SŌU SŌU • SERIBU PEDANG TERBANG",
            burstTitle = "斩!",
            burstMeaning = "ZHǍN! • HEAVENLY SLASH",
            accent = ComicSfxAccent.CYAN,
            stamps = listOf("🗡️ SWORD!", "⚡ ZHǍN!", "💨 SWIFT", "☯️ DAO"),
        ),
        ComicSfx(
            id = "cn-dongdong",
            origin = "🇨🇳 Manhua",
            chars = listOf("咚", "咚", "咚"),
            sublabel = "DŌNG DŌNG • DETAK JANTUNG QI",
            burstTitle = "爆!",
            burstMeaning = "BÀO! • QI EXPLOSION",
            accent = ComicSfxAccent.PINK,
            stamps = listOf("💥 BÀO!", "🔥 QI SURGE", "⚡ DANTIAN", "🐉 DRAGON"),
        ),
        ComicSfx(
            id = "cn-ang",
            origin = "🇨🇳 Manhua",
            chars = listOf("昂", "昂", "〜"),
            sublabel = "ÁNG! • AUMAN NAGA EMAS",
            burstTitle = "灭!",
            burstMeaning = "MIÈ! • ANNIHILATION SEAL",
            accent = ComicSfxAccent.ORANGE,
            stamps = listOf("🐉 ROAR!", "☯️ SEAL!", "💥 MIÈ!", "👑 EMPEROR"),
        ),
        ComicSfx(
            id = "cn-weng",
            origin = "🇨🇳 Manhua",
            chars = listOf("嗡", "嗡", "嗡"),
            sublabel = "WĒNG WĒNG • RESONANSI ARTEFAK",
            burstTitle = "凝!",
            burstMeaning = "NÍNG! • SPIRIT CONDENSE",
            accent = ComicSfxAccent.PURPLE,
            stamps = listOf("✨ RELIC", "☯️ NÍNG!", "🔮 SOUL", "⚡ DIVINE"),
        ),
        ComicSfx(
            id = "cn-ka",
            origin = "🇨🇳 Manhua",
            chars = listOf("咔", "嚓", "!"),
            sublabel = "KĀ CHĀ • LANGIT TERBELAH",
            burstTitle = "碎!",
            burstMeaning = "SUÌ! • VOID SHATTER",
            accent = ComicSfxAccent.CYAN,
            stamps = listOf("⚡ CRACK!", "🌌 VOID", "💥 SUÌ!", "🗡️ SUPREME"),
        ),
        ComicSfx(
            id = "cn-hu",
            origin = "🇨🇳 Manhua",
            chars = listOf("呼", "啸", "!"),
            sublabel = "HŪ XIÀO • ANGIN BADAI GUNUNG",
            burstTitle = "镇!",
            burstMeaning = "ZHÈN! • HEAVENLY SUPPRESS",
            accent = ComicSfxAccent.YELLOW,
            stamps = listOf("🌪️ GALE!", "⛰️ MOUNT", "💥 ZHÈN!", "☯️ SECT"),
        ),
        ComicSfx(
            id = "cn-chi",
            origin = "🇨🇳 Manhua",
            chars = listOf("嗤", "嗤", "嗤"),
            sublabel = "CHĪ CHĪ • API ALKIMIA SUCI",
            burstTitle = "成!",
            burstMeaning = "CHÉNG! • ELIXIR PERFECTED",
            accent = ComicSfxAccent.PINK,
            stamps = listOf("🔥 ALCHEMY", "💊 PILL!", "✨ CHÉNG!", "🐉 9-STAR"),
        ),
    )

    val ALL: List<ComicSfx> = MANGA_VARIANTS + MANHWA_VARIANTS + MANHUA_VARIANTS

    fun random(): ComicSfx = ALL[Random.nextInt(ALL.size)]

    fun pickNext(currentId: String?): ComicSfx {
        val candidates = ALL.filter { it.id != currentId }.ifEmpty { ALL }
        return candidates[Random.nextInt(candidates.size)]
    }
}
