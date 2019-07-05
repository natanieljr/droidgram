package org.droidmate.droidgram.reporter

import org.droidmate.droidgram.grammar.reachedTerminals
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

object ResultBuilder {
    @JvmStatic
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    private const val nrSeeds = 11

    /**
     * Pattern: inputs00.txt, inputs01.txt, ..., inputs10.txt
     */
    private fun getFilesInputs(inputDir: Path): List<Path> {
        return Files.walk(inputDir)
            .filter { it.fileName.toString().startsWith("inputs") }
            .filter { it.fileName.toString().endsWith(".txt") }
            .filter { it.fileName.toString().length == 12 }
            .toList()
    }

    /**
     * Pattern: reachedTerminals.txt
     */
    private fun getFilesReachedWidgets(explorationResultDir: Path): List<Path> {
        return Files.walk(explorationResultDir)
            .filter { it.fileName.toString() == reachedTerminals }
            .toList()
    }

    /**
     * Pattern *\coverage\*-statements-*
     */
    private fun getFilesReachedStatements(explorationResultDir: Path): List<Path> {
        return Files.walk(explorationResultDir)
            .filter { it.toAbsolutePath().toString().contains("/coverage/") }
            .filter { it.fileName.toString().contains("-statements-") }
            .toList()
    }

    private fun getReachedTerminals(baseDir: Path): Set<String> {
        return getFilesReachedWidgets(baseDir)
            .flatMap { Files.readAllLines(it) }
            .filter { it.isNotEmpty() }
            .map { stmt ->
                val res = stmt.substringBefore(");").trim() + ")"

                check(res.contains('(')) { "Invalid reached statement $stmt" }
                check(res.endsWith(')')) { "Invalid reached statement $stmt" }

                res
            }.toSet()
    }

    private fun getReachedStatements(baseDir: Path): Set<Long> {
        val files = getFilesReachedStatements(baseDir)
        val fileCount = files.size
        val coverages = files.mapIndexed { index, file ->
            val lineSet = mutableSetOf<Long>()
            log.debug("Processing file $file ($index/$fileCount)")

            if (index % 100 == 0) {
                log.info("Processing file $file ($index/$fileCount)")
            }

            Files.newBufferedReader(file).useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotEmpty()) {
                        val id = line.takeWhile { it != ';' }.toLong()
                        lineSet.add(id)
                    }
                }
            }
            lineSet
        }

        return if (coverages.isNotEmpty())
            coverages.reduce { acc, mutableSet ->
            acc.addAll(mutableSet)
            acc
        } else {
            emptySet()
        }
    }

    private fun getCodeCoverage(allStatements: Set<Long>, dir: Path): Result<Long> {
        return calculateCodeCoverage(allStatements, dir)
    }

    fun generateCodeCoverage(allStatements: Set<Long>, dir: Path) {
        val coverage = getCodeCoverage(allStatements, dir)
        val coverageFile = dir.resolve("codeCoverage.txt")

        coverage.save(coverageFile)
    }

    private fun calculateCodeCoverage(allStatements: Set<Long>, dir: Path): Result<Long> {
        val reached = getReachedStatements(dir)
        val missing = allStatements - reached

        val res = Result(allStatements.toList(), reached, missing)

        localCheck(res.coverage in 0.0..1.0) { "Expected code coverage between 0 and 1. Found ${res.coverage}" }

        return res
    }

    fun generateGrammarCoverage(input: String, dir: Path) {
        generateGrammarCoverage(listOf(input), dir)
    }

    private fun getGrammarCoverage(allTerminals: List<String>, dir: Path): Result<String> {
        return calculateGrammarCoverage(allTerminals, dir)
    }

    fun generateGrammarCoverage(allTerminals: List<String>, dir: Path) {
        val coverage = getGrammarCoverage(allTerminals, dir)
        val coverageFile = dir.resolve("grammarCoverage.txt")

        coverage.save(coverageFile)
    }

    private fun getInputSize(allTerminals: List<String>): List<Int> {
        return allTerminals.map { input ->
            val inputs = input
                .split(" ")
                .filter { it.isNotEmpty() }

            inputs.size
        }
    }

    fun generateInputSize(allTerminals: List<String>, dir: Path) {
        val inputSize = getInputSize(allTerminals)
        val totalSize = inputSize.sum()

        val sb = StringBuilder()
        sb.appendln("Total input size: $totalSize")

        inputSize.forEachIndexed { idx, size ->
            sb.appendln("$idx\t$size")
        }

        val inputSizeFile = dir.resolve("inputSize.txt")
        Files.write(inputSizeFile, sb.toString().toByteArray())
    }

    fun generateSummary(inputs: List<List<String>>, allStatements: Set<Long>, dir: Path, seedNr: Int) {
        val sb = StringBuilder()
        sb.appendln("Seed\tInput Size\tGrammarReached\tGrammarMissed\tGrammarCov\tCodeReached\tCodeMissed\tCodeCov")

        val allTerminals = inputs.flatten()

        Files.list(dir)
            .filter { Files.isDirectory(it) }
            .filter { it.fileName.toString().startsWith("seed") }
            .sorted()
            .forEach { seedDir ->
                log.info("Writing seed ${seedDir.fileName} into summary")

                val grammarCoverage =
                    getGrammarCoverage(allTerminals, seedDir)
                val codeCoverage =
                    getCodeCoverage(allStatements, seedDir)

                val index = seedDir.fileName.toString().removePrefix("seed").toInt()
                val inputSize = if (seedNr >= 0) {
                    getInputSize(inputs[0])
                } else {
                    getInputSize(inputs[index])
                }

                sb.append(index)
                    .append("\t")
                    .append(inputSize.sum())
                    .append("\t")
                    .append(grammarCoverage.reached.size)
                    .append("\t")
                    .append(grammarCoverage.missed.size)
                    .append("\t")
                    .append(grammarCoverage.coverage)
                    .append("\t")
                    .append(codeCoverage.reached.size)
                    .append("\t")
                    .append(codeCoverage.missed.size)
                    .append("\t")
                    .append(codeCoverage.coverage)
                    .appendln()
            }

        val summaryFile = dir.resolve("summary.txt")
        Files.write(summaryFile, sb.toString().toByteArray())
    }

    private fun calculateGrammarCoverage(inputs: List<String>, dir: Path): Result<String> {
        val allTerminals = inputs
            .flatMap {
            it.split(" ")
                .toList()
                .filter { p -> p.isNotEmpty() }
        }.toSet()

        val reached = getReachedTerminals(dir)
        val missing = allTerminals - reached

        val res = Result(allTerminals.toList(), reached, missing)

        localCheck(res.coverage in 0.0..1.0) { "Expected terminal coverage between 0 and 1. Found ${res.coverage}" }

        return res
    }

    private fun Path.getInputs(): List<List<String>> {
        val files = getFilesInputs(this)

        localCheck(files.isNotEmpty()) { "Input directory $this doesn't contain any input file (inputs*.txt)" }

        val data = files.map { inputFile ->
            Files.readAllLines(inputFile)
                .filter { it.isNotEmpty() }
        }

        localCheck(data.size == nrSeeds) { "Expecting $nrSeeds seeds per app. Found ${data.size}" }

        return data
    }

    private fun Path.getSeedDirs(): List<Path> {
        val seedDirs = Files.list(this)
            .sorted()
            .filter { Files.isDirectory(it) }
            .filter { it.fileName.toString().startsWith("seed") }
            .toList()

        localCheck(seedDirs.size == nrSeeds) { "Expecting $nrSeeds seed results. Found ${seedDirs.size}" }

        return seedDirs
    }

    private fun Path.findApkJSON(apk: String): Path {
        val jsonFile = Files.walk(this)
            .toList()
            .firstOrNull { it.fileName.toString().startsWith(apk) &&
                    it.fileName.toString().endsWith(".apk.json")
            }

        check(jsonFile != null && Files.exists(jsonFile)) { "Instrumentation file $jsonFile not found" }

        return jsonFile
    }

    private fun getTotalLOC(jsonFile: Path): Set<Long> {
        val jsonData = String(Files.readAllBytes(jsonFile))
        val jObj = JSONObject(jsonData)

        val jMap = jObj.getJSONObject("allMethods")

        return jMap.keys()
            .asSequence()
            .map { it.toLong() }
            .toSet()
    }

    private fun localCheck(value: Boolean, lazyMessage: () -> String): Boolean {
        if (!value) {
            log.warn(lazyMessage())
        }

        return value
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val experimentRootDir = if (args.isNotEmpty()) {
            Paths.get(args.first())
        } else {
            Paths.get("/Users/nataniel/Documents/saarland/repositories/test/droidgram/out/colossus")
        }

        val sb = StringBuilder()

        Files.list(experimentRootDir)
            .forEach { rootDir ->
                val inputDir = rootDir.resolve("input").resolve("apks")
                val resultDir = rootDir.resolve("output")
                val apksDir = rootDir.resolve("apks")
                val modelDir = inputDir.resolve("droidMate")
                try {
                    log.debug("Processing input dir: $inputDir")
                    log.debug("Processing output dir: $resultDir")

                    localCheck(Files.exists(modelDir)) {
                        "Droidmate dir not found in $modelDir"
                    }

                    val jsonFile = apksDir.findApkJSON(rootDir.fileName.toString())
                    log.debug("Processing instrumentation file: $jsonFile")
                    val loc = getTotalLOC(jsonFile)

                    val translationTableFile = inputDir.resolve("translationTable.txt")
                    if (localCheck(Files.exists(translationTableFile)) {
                            "Input directory $inputDir missing translation table file"
                        }) {
                        log.debug("Processing translation table file: $translationTableFile")

                        val translationTable = Files.readAllLines(translationTableFile)
                            .filter { it.isNotEmpty() }

                        val inputs = inputDir.getInputs()
                        val originalStatements =
                            getReachedStatements(inputDir)

                        val seedDirs = resultDir.getSeedDirs()

                        val result =
                            AppData(
                                rootDir.fileName.toString(),
                                translationTable,
                                loc,
                                originalStatements
                            )

                        seedDirs.forEachIndexed { idx, seedDir ->
                            val input = inputs[idx]
                            val code = calculateCodeCoverage(
                                originalStatements,
                                seedDir
                            )
                            val grammar =
                                calculateGrammarCoverage(input, seedDir)

                            result.addRun(input, grammar, code)
                        }

                        log.debug("Processed $inputDir generating result")
                        val str = result.toString()
                        sb.appendln(str)
                        println(str)
                    }
                } catch (e: IllegalStateException) {
                    log.error("${rootDir.fileName} - ${e.message}")
                }
            }

        sb.lineSequence().forEach { line ->
            println(line)
        }
    }
}