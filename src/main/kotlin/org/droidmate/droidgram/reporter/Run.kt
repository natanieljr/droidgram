package org.droidmate.droidgram.reporter

data class Run(val _inputs: List<String>, val grammar: Result<String>, val code: Result<Long>) {
    private val inputs: List<List<String>> by lazy {
        _inputs.map {
            it.split(" ")
                .toList()
        }
    }

    val numberOfActions: Int by lazy {
        inputs.sumBy {
            it.size
        }
    }
}