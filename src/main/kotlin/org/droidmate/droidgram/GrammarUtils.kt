package org.droidmate.droidgram

val defaultNonTerminalRegex = "(<[^<> ]*>)".toPattern().toRegex()

/**
 * In later chapters, we allow expansions to be tuples, with the expansion being the first element
 */
fun String.nonTerminals(): Set<String> {
    return defaultNonTerminalRegex.findAll(this)
        .map { it.value }
        .toSet()
}

fun String.splitByNonTerminals(): List<String> {
    val matcher = defaultNonTerminalRegex.toPattern().matcher(this)

    if (!matcher.find()) {
        return emptyList()
    }

    val result = mutableListOf<String>()
    var lastStart = matcher.end()
    do {
        if (matcher.start() > (lastStart + 1)) {
            result.add(this.substring(lastStart, matcher.start()))
        }

        result.add(this.substring(matcher.start(), matcher.end()))
        lastStart = matcher.end()
    } while (matcher.find())

    return result
}

fun String.isNonTerminal(): Boolean {
    return defaultNonTerminalRegex.matches(this)
}
