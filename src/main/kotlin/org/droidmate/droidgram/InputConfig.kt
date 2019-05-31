package org.droidmate.droidgram

import org.droidmate.configuration.ConfigurationWrapper
import org.droidmate.coverage.INSTRUMENTATION_FILE_METHODS_PROP
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.streams.toList

class InputConfig @Throws(IllegalArgumentException::class) constructor(cfg: ConfigurationWrapper) {
    val inputDir = Paths.get(cfg[CommandLineConfig.inputDir].path).toAbsolutePath()

    init {
        if (!cfg.contains(CommandLineConfig.inputDir)) {
            throw IllegalArgumentException("Input directory not set. Use -i <PATH> to set the path")
        }

        if (!Files.isDirectory(inputDir)) {
            throw IllegalArgumentException("Input directory $inputDir does not exist")
        }
    }

    private val inputFiles: List<Path> by lazy {
        val files = Files.walk(inputDir)
            .filter {
                it.fileName.toString().startsWith("input") &&
                        it.fileName.toString().endsWith(".txt")
            }
            .toList()
            .sorted()

        if (files.isEmpty()) {
            throw IllegalArgumentException("Input directory doesn't contain any input file (inputs*.txt)")
        }

        files
    }

    val inputs: List<List<String>> by lazy {
        inputFiles.map { inputFile ->
            Files.readAllLines(inputFile)
                .filter { it.isNotEmpty() }
        }
    }

    private val translationTableFile: Path by lazy {
        val file = Files.walk(inputDir)
            .filter {
                it.fileName.toString().toLowerCase() == "translationtable.txt"
            }
            .toList()
            .firstOrNull()

        file ?: throw IllegalArgumentException("Input directory doesn't contain a translation table (translationTable.txt)")
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

    private val coverageFile: Path by lazy {
        val file = Files.walk(inputDir)
            .filter {
                it.fileName.toString().endsWith(".json")
            }
            .toList()
            .firstOrNull()

        file ?: throw IllegalArgumentException("Input directory doesn't contain an instrumentatin file (*.json)")
    }

    val coverage: Set<Long> by lazy {
        val jsonData = String(Files.readAllBytes(coverageFile))
        val jObj = JSONObject(jsonData)

        val jMap = jObj.getJSONObject(INSTRUMENTATION_FILE_METHODS_PROP)

        jMap.keys()
            .asSequence()
            .map { it.toLong() }
            .toSet()
    }
}