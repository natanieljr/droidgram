package org.droidmate.droidgram.mining

import java.nio.file.Files
import java.nio.file.Path

fun coveragePerAction(coverageFiles: List<Path>): Map<Int, Set<Long>> {
    return coverageFiles.map { coverageFile ->
        val fileId = coverageFile.fileName.toString().takeLastWhile { it != '-' }.toInt()
        val data = Files.readAllLines(coverageFile)

        val statements = data.filter { it.isNotEmpty() }
            .map { it.takeWhile { char -> char != ';' } }
            .map { it.toLong() }
            .toSet()

        Pair(fileId, statements)
    }.toMap()
}