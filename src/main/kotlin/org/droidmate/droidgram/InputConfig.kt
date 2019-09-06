package org.droidmate.droidgram

import org.droidmate.configuration.ConfigurationWrapper
import org.droidmate.droidgram.mining.coveragePerAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.streams.toList

class InputConfig constructor(cfg: ConfigurationWrapper) {
    companion object {
        @JvmStatic
        private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    }

    private val inputFilePrefix: String by lazy {
        check(cfg.contains(CommandLineConfig.inputFilePrefix)) {
            "Input directory not set. Use -f <STRING> to set the prefix"
        }

        cfg[CommandLineConfig.inputFilePrefix]
    }

    val inputDir: Path by lazy {
        check(cfg.contains(CommandLineConfig.inputDir)) { "Input directory not set. Use -i <PATH> to set the path" }

        val dir = Paths.get(cfg[CommandLineConfig.inputDir].path).toAbsolutePath()

        check(Files.isDirectory(dir)) { "Input directory $dir does not exist" }

        dir
    }

    val seedNr by lazy {
        if (cfg.contains(CommandLineConfig.seedNr)) {
            cfg[CommandLineConfig.seedNr]
        } else {
            -1
        }
    }

    private fun Path.isInputFile(seedNr: Int): Boolean {
        val seedNrStr = seedNr.toString().padStart(2, '0')

        val candidateName = this.fileName.toString()

        val isInputFile = candidateName.startsWith(inputFilePrefix) &&
                candidateName.endsWith(".txt") &&
                (candidateName == "$inputFilePrefix$seedNrStr.txt" || seedNr == -1)

        if (isInputFile) {
            log.warn("Is input file: $isInputFile: $this")
        }

        return isInputFile
    }

    private val inputFiles: List<Path> by lazy {
        val seedNrStr = seedNr.toString().padStart(2, '0')

        val files = Files.walk(inputDir)
            .filter { it.isInputFile(seedNr) }
            .toList()
            .sorted()

        check(files.isNotEmpty()) {
            "Input directory $inputDir doesn't contain any input file ($inputFilePrefix)"
        }
        check(seedNr == -1 || files.size == 1) {
            "Multiple input files were found for the same seed $seedNrStr"
        }

        files
    }

    val inputs: List<List<String>> by lazy {
        inputFiles.map { inputFile ->
            Files.readAllLines(inputFile)
                .filter { it.isNotEmpty() }
        }
    }

    private val translationTableFile by lazy {
        val translationTableName = "translationTable.txt".toLowerCase()

        Files.walk(inputDir)
            .filter {
                it.fileName.toString().toLowerCase() == translationTableName
            }
            .toList()
            .firstOrNull()
    }

    val translationTable: Map<String, UUID> by lazy {
        val dataFile = translationTableFile

        if (dataFile == null) {
            emptyMap()
        } else {
            Files.readAllLines(dataFile)
                .filter { it.isNotEmpty() }
                .map { line ->
                    val data = line.split(";")

                    assert(data.size == 2) { "Each line in the translation table should have 2 elements (ID;UUID)" }

                    val id = data.first().trim()
                    val uuid = UUID.fromString(data.last().trim())

                    Pair(id, uuid)
                }.toMap()
        }
    }

    private val coverageFiles: List<Path> by lazy {
        log.warn("Searching coverage files in $inputDir")

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