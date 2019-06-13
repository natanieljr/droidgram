package org.droidmate.droidgram

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

data class Result<T>(val originalInputs: List<T>, private val _reached: Set<T>, private val _missed: Set<T>) {
    companion object {
        @JvmStatic
        private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    }

    private val reached: Set<T> by lazy {
        val res = _reached
            .filter { originalInputs.contains(it) }
            .toSet()

        val diff = _reached - res

        if (diff.isNotEmpty()) {
            log.debug("Found ${diff.size} elements which were not on the originalInputs exploration")
        }

        res
    }

    private val missed: Set<T> by lazy {
        val res = _missed
            .filter { originalInputs.contains(it) }
            .toSet()

        val diff = _missed - res

        if (diff.isNotEmpty()) {
            log.debug("Found ${diff.size} elements which were not on the originalInputs exploration")
        }

        res
    }

    val coverage: Double by lazy {
            val total = reached + missed

            if (missed.size > total.size) {
                throw IllegalArgumentException("Missed more than total arguments. Missed: $missed Reached: $reached")
            }

            1 - (missed.size.toDouble() / total.size)
    }

    fun save(outputFile: Path) {
        val sb = StringBuilder()
        sb.appendln("Coverage: $coverage")
        sb.appendln("Reached: $reached")
        sb.appendln("Missed: $missed")

        Files.write(outputFile, sb.toString().toByteArray())
    }

    override fun toString(): String {
        return "Coverage $coverage\tReached ${reached.size}\tMissed ${missed.size}"
    }
}