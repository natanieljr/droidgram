package org.droidmate.droidgram.reporter

class AppData(
    private val apk: String,
    private val translationTable: List<String>,
    private val totalLOC: Set<Long>,
    private val originalLOC: Set<Long>,
    private val runs: MutableList<Run> = mutableListOf()
) {
    private val originalCoverage: Double by lazy {
        val coverage = originalLOC.size.toDouble() / totalLOC.size

        check(originalLOC.all { totalLOC.contains(it) }) {
            "Found some statements in original coverage which are not in the instrumentation file (JSON)"
        }

        check(coverage in 0.0..1.0) { "Expected code coverage between 0 and 1. Found $coverage" }

        coverage
    }

    private val numberWidgets: Int by lazy {
        translationTable.count { it.startsWith("w") }
    }

    private val numberStates: Int by lazy {
        translationTable.count { it.startsWith("s") }
    }

    fun addRun(inputs: List<String>, grammar: Result<String>, code: Result<Long>) {
        runs.add(Run(inputs, grammar, code))
    }

    override fun toString(): String {
        val sb = StringBuilder()

        sb.append("\t")
            .append(this.apk)
            .append("\t")
            .append("Total LOC")
            .append("\t")
            .append(this.totalLOC.size)
            .append("\t")
            .append("Test Case LOC")
            .append("\t")
            .append(this.originalLOC.size)
            .append("\t")
            .append("Test Case Coverage")
            .append("\t")
            .append(this.originalCoverage)
            .append("\t")
            .append("Grammar")
            .append("\t")
            .append("States")
            .append("\t")
            .append(this.numberStates)
            .append("\t")
            .append("Widgets")
            .append("\t")
            .append(this.numberWidgets)
            .append("\t")
            .append("Runs")
            .append("\t")
            .append("Input size")
            .append("\t")
            .append(this.runs.map { it.numberOfActions }.joinToString("\t"))
            .append("\t")
            .append("Grammar (Terminals)")
            .append("\t")
            .append(this.runs.map { it.grammar.coverage.toString().replace(".", ",") }.joinToString("\t"))
            .append("\t")
            .append("Code")
            .append("\t")
            .append(this.runs.map { it.code.coverage.toString().replace(".", ",") }.joinToString("\t"))

        return sb.toString()
    }
}