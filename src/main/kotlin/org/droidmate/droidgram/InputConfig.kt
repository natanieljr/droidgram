package org.droidmate.droidgram

import org.droidmate.configuration.ConfigurationWrapper
import org.droidmate.droidgram.mining.coveragePerAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.streams.toList

class InputConfig constructor(cfg: ConfigurationWrapper) {
    val inputDir: Path by lazy {
        check(cfg.contains(CommandLineConfig.inputDir)) { "Input directory not set. Use -i <PATH> to set the path" }
        check(Files.isDirectory(inputDir)) { "Input directory $inputDir does not exist" }

        Paths.get(cfg[CommandLineConfig.inputDir].path).toAbsolutePath()
    }

    val seedNr by lazy {
        if (cfg.contains(CommandLineConfig.seedNr)) {
            cfg[CommandLineConfig.seedNr]
        } else {
            -1
        }
    }

    val useCoverage by lazy {
        if (cfg.contains(CommandLineConfig.useCoverageGrammar)) {
            cfg[CommandLineConfig.useCoverageGrammar]
        } else {
            false
        }
    }

    private fun Path.isInputFile(seedNr: Int): Boolean {
        val seedNrStr = seedNr.toString().padStart(2, '0')
        val inputFileName = if (useCoverage) {
            "coverageInputs"
        } else {
            "inputs"
        }

        val candidateName = this.fileName.toString()

        return candidateName.startsWith(inputFileName) &&
                candidateName.endsWith(".txt") &&
                (candidateName == "inputs$seedNrStr.txt" || seedNr == -1)
    }

    private val inputFiles: List<Path> by lazy {
        val seedNrStr = seedNr.toString().padStart(2, '0')

        val files = Files.walk(inputDir)
            .filter { it.isInputFile(seedNr) }
            .toList()
            .sorted()

        check(files.isNotEmpty()) { "Input directory $inputDir doesn't contain any input file (inputs*.txt)" }
        check(seedNr == -1 || files.size == 1) { "Multiple input files were found for the same seed $seedNrStr" }

        files
    }

    val inputs: List<List<String>> by lazy {
        inputFiles.map { inputFile ->
            Files.readAllLines(inputFile)
                .filter { it.isNotEmpty() }
        }
    }

    private val translationTableFile by lazy {
        val translationTableName = if (useCoverage) {
            "translationTableWithCoverage.txt"
        } else {
            "translationTable.txt"
        }.toLowerCase()

        val file = Files.walk(inputDir)
            .filter {
                it.fileName.toString().toLowerCase() == translationTableName
            }
            .toList()
            .firstOrNull()

        check(file != null) { "Input directory doesn't contain a translation table ($translationTableName)" }
        file
    }

    val translationTable: Map<String, UUID> by lazy {
        Files.readAllLines(translationTableFile)
            .filter { it.isNotEmpty() }
            .map { line ->
                val data = line.split(";")

                assert(data.size == 2) { "Each line in the translation table should have 2 elements (ID;UUID)" }

                val id = data.first().trim()
                val uuid = UUID.fromString(data.last().trim())

                Pair(id, uuid)
            }.toMap()
    }

    private val coverageFiles: List<Path> by lazy {
        val files = Files.walk(inputDir)
                .filter { it.fileName.toString().contains("-statements-") }
                .filter { it.fileName.toString().takeLastWhile { p -> p != '-' }.toLongOrNull() != null }
                .toList()

        check(files.isNotEmpty()) { "No instrumentation files found" }

        files
    }

    val coverage: Set<Long> by lazy {
        coveragePerAction(coverageFiles).values
            .flatten()
            .toSet()
    }
}