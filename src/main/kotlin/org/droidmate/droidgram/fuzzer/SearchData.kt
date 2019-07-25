package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Production

data class SearchData(
    val node: Node,
    val baseExpansion: Production,
    val currentExpansion: Production,
    val currentDepth: Int
)