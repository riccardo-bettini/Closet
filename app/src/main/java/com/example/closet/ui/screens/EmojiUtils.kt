package com.example.closet.ui.screens

/**
 * Mappatura per il ripristino di vecchie stringhe emoji danneggiate da problemi di codifica mojibake
 * (ad esempio caratteri interpretati erroneamente come Windows-1252 invece che UTF-8).
 */
val corruptedToCleanEmojiMap: Map<String, String> = mapOf(
    "ðŸ‘•" to "👕",
    "ðŸ‘”" to "👔",
    "ðŸ‘—" to "👗",
    "ðŸ§¥" to "🧥",
    "ðŸ©²" to "🩲",
    "ðŸ©³" to "🩳",
    "ðŸ‘–" to "👖",
    "ðŸ‘š" to "👚",
    "ðŸ¥¼" to "🥼",
    "ðŸ¥»" to "🥻",
    "ðŸ‘˜" to "👘",
    "ðŸŽ½" to "🎽",
    "ðŸ¦º" to "🦺",
    "ðŸ¥‹" to "🥋",
    "ðŸ¤µ" to "🤵",
    "ðŸ‘°" to "👰",
    "ðŸ‘™" to "👙",
    "ðŸ©±" to "🩱",

    "ðŸ‘Ÿ" to "👟",
    "ðŸ‘ž" to "👞",
    "ðŸ¥¾" to "🥾",
    "ðŸ‘¡" to "👠",
    "ðŸ‘ " to "👢",
    "ðŸ‘¢" to "👡",
    "ðŸ¥¿" to "🥿",
    "ðŸ©°" to "🩰",
    "ðŸ©" to "🩴",
    "ðŸ§¦" to "🧦",
    "â›¸ï¸ " to "⛸️",
    "ðŸŽ¿" to "🎿",

    "ðŸ§¢" to "🧢",
    "ðŸ‘’" to "👒",
    "ðŸŽ©" to "🎩",
    "ðŸŽ“" to "🎓",
    "ðŸ‘‘" to "👑",
    "â›‘ï¸ " to "⛑️",
    "ðŸª–" to "🪖",
    "ðŸ§£" to "🧣",
    "ðŸ§¤" to "🧤",
    "ðŸ•¶ï¸ " to "🕶️",
    "ðŸ‘“" to "👓",
    "ðŸŒ‚" to "🌂",
    "â˜‚ï¸ " to "☂️",
    "ðŸ‘œ" to "👜",
    "ðŸŽ’" to "🎒",
    "ðŸ‘›" to "👛",
    "ðŸ’¼" to "💼",
    "ðŸ§³" to "🧳",

    "ðŸ’ " to "💍",
    "ðŸ’Ž" to "💎",
    "âŒš" to "⌚",
    "ðŸ•°ï¸ " to "🕰️",
    "ðŸª¡" to "🪡",
    "ðŸ§µ" to "🧵",
    "ðŸ§¶" to "🧶",
    "ðŸ§·" to "🧷",
    "ðŸ ·ï¸ " to "🏷️",
    "ðŸŽ€" to "🎀",

    "â˜€ï¸ " to "☀️",
    "â „ï¸ " to "❄️",
    "ðŸŒ§ï¸ " to "🌧️",
    "â›…" to "⛅",
    "ðŸŒŠ" to "🌊",
    "ðŸ ‚" to "🍂",
    "ðŸŒ¸" to "🌸",
    "âš¡" to "⚡",
    "âœ¨" to "✨",
    "ðŸ”¥" to "🔥",
    "â­ " to "⭐",
    "ðŸ–¤" to "🖤",
    "ðŸ¤ " to "🤍",
    "ðŸ§¸" to "🧸",
    "ðŸ’¯" to "💯",
    "ðŸŽ¯" to "🎯",
    "ðŸš€" to "🚀",
    "ðŸ †" to "🏆"
)

/**
 * Normalizza qualsiasi stringa emoji:
 * 1. Converte eventuali stringhe legacy danneggiate da mojibake nel corrispettivo emoji UTF-8 corretto.
 * 2. Tenta un recupero automatico qualora la stringa contenga sequenze di byte mal codificate.
 * 3. Restituisce il valore pulito o null se la stringa è vuota/blank.
 */
fun normalizeEmoji(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()

    // 1. Mappatura diretta conosciuta
    corruptedToCleanEmojiMap[trimmed]?.let { return it }

    // 2. Se non ha indicatori di mojibake, è già valida
    if (!trimmed.contains('ð') && !trimmed.contains('â') && !trimmed.contains('Ã') && !trimmed.contains('Ÿ')) {
        return trimmed
    }

    // 3. Tentativo di recupero tramite decodifica charset
    try {
        val bytes = trimmed.toByteArray(charset("windows-1252"))
        val candidate = String(bytes, Charsets.UTF_8)
        if (candidate.isNotBlank() && !candidate.contains('\uFFFD') && !candidate.contains('ð') && !candidate.contains('â')) {
            return candidate
        }
    } catch (_: Exception) {}

    try {
        val bytes = trimmed.toByteArray(Charsets.ISO_8859_1)
        val candidate = String(bytes, Charsets.UTF_8)
        if (candidate.isNotBlank() && !candidate.contains('\uFFFD') && !candidate.contains('ð') && !candidate.contains('â')) {
            return candidate
        }
    } catch (_: Exception) {}

    return trimmed
}

data class EmojiCategory(
    val title: String,
    val emojis: List<String>
)

val wardrobeEmojiCategories: List<EmojiCategory> = listOf(
    EmojiCategory(
        title = "Abbigliamento",
        emojis = listOf(
            "👕", "👔", "👗", "🧥", "🩲", "🩳", "👖", "👚",
            "🥼", "🥻", "👘", "🎽", "🦺", "🥋", "🤵", "👰",
            "👙", "🩱"
        )
    ),
    EmojiCategory(
        title = "Scarpe",
        emojis = listOf(
            "👟", "👞", "🥾", "👡", "👠", "👢", "🥿", "🩰",
            "🩴", "🧦", "⛸️", "🎿"
        )
    ),
    EmojiCategory(
        title = "Accessori",
        emojis = listOf(
            "🧢", "👒", "🎩", "🎓", "👑", "⛑️", "🪖", "🧣",
            "🧤", "🕶️", "👓", "🌂", "☂️", "👜", "🎒", "👛",
            "💼", "🧳", "💍", "💎", "⌚", "🕰️", "🪡", "🧵",
            "🧶", "🧷", "🏷️", "🎀"
        )
    ),
    EmojiCategory(
        title = "Meteo & Stile",
        emojis = listOf(
            "☀️", "❄️", "🌧️", "⛅", "🌊", "🍂", "🌸", "⚡",
            "✨", "🔥", "⭐", "🖤", "🤍", "🧸", "💯", "🎯",
            "🚀", "🏆"
        )
    )
)

val allWardrobeEmojis: List<String> = wardrobeEmojiCategories.flatMap { it.emojis }.distinct()
