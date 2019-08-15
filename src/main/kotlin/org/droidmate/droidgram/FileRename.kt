package org.droidmate.droidgram

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.streams.asSequence

class FileRename {
    companion object {
        private fun Path.moveTo(dst: Path) {
            println("$this, $dst")
            Files.move(this, dst, StandardCopyOption.REPLACE_EXISTING)
        }

        @Suppress("unused")
        private fun moveInputFiles(appDir: Path, index: String) {
            val inputDir = appDir
                .resolve("input_rq3")
                .resolve("apks")

            if (!Files.exists(inputDir)) {
                return
            }

            // Input Files
            val inputFiles = Files.list(inputDir)
                .asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().startsWith("coverageInputs") }
                .filter { it.fileName.toString().endsWith(".txt") }
                // Already processed
                .filterNot { it.fileName.toString().contains("_") }
                .toList()
                .sorted()

            inputFiles.forEach { inputFile ->
                val fileName = inputFile.fileName.toString()
                val newInputFileName = fileName.replace("coverageInputs", "coverageInputs_${index}_")
                val newInputFile = inputFile.resolveSibling(newInputFileName)

                inputFile.moveTo(newInputFile)
            }

            // Grammar
            val grammarFile = inputDir.resolve("grammarWithCoverage.txt")
            val newGrammarFile = inputDir.resolve("grammarWithCoverage_$index.txt")

            if (Files.exists(grammarFile)) {
                grammarFile.moveTo(newGrammarFile)
            }

            // Translation Table
            // val translationTableFile = inputDir.resolve("translationTable.txt")
            // val newTranslationTableFile = inputDir.resolve("translationTable_$index.txt")

            // if (Files.exists(translationTableFile)) {
            //     translationTableFile.moveTo(newTranslationTableFile)
            // }

            // Initial Exploration Dir
            // val droidmateDir = inputDir.resolve("droidMate")
            // val newDroidmateDir = inputDir.resolve("droidmate_$index")

            // if (Files.exists(droidmateDir)) {
            //     droidmateDir.moveTo(newDroidmateDir)
            // }
        }

        @Suppress("unused")
        private fun moveOutputFiles(appDir: Path, index: String) {
            val outputDir = appDir
                .resolve("output_rq3")

            if (!Files.exists(outputDir)) {
                return
            }

            // Seed Directories
            val seedDirs = Files.list(outputDir)
                .asSequence()
                .filter { Files.isDirectory(it) }
                .filter { it.fileName.toString().startsWith("seed") }
                // Already processed
                .filterNot { it.fileName.toString().contains("_") }
                .toList()
                .sorted()

            seedDirs.forEach { seedDir ->
                val fileName = seedDir.fileName.toString()
                // seed2 -> seed_0_02
                val newSeedDirName = if (fileName.length == 5) {
                    fileName.replace("seed", "seed_${index}_0")
                } else {
                    fileName.replace("seed", "seed_${index}_")
                }
                val newSeedDir = seedDir.resolveSibling(newSeedDirName)

                seedDir.moveTo(newSeedDir)
            }

            // Summary File
            val summaryFile = outputDir.resolve("summary.txt")
            val newSummaryFile = outputDir.resolve("summary_$index.txt")

            if (Files.exists(summaryFile)) {
                summaryFile.moveTo(newSummaryFile)
            }
        }

        @Suppress("unused")
        private fun fixSeedDirName(appDir: Path) {
            val outputDir = appDir
                .resolve("output")

            // Seed Directories
            val seedDirs = Files.list(outputDir)
                .asSequence()
                .filter { Files.isDirectory(it) }
                .filter { it.fileName.toString().startsWith("seed") }
                // Already processed
                .filter { it.fileName.toString().contains("_") }
                .toList()
                .sorted()

            seedDirs.forEach { seedDir ->
                val fileName = seedDir.fileName.toString()
                // seed2 -> seed_00_02
                var newSeedDirName = fileName
                if (newSeedDirName.endsWith("_0")) newSeedDirName = newSeedDirName.replace("_0", "_00")
                if (newSeedDirName.endsWith("_1")) newSeedDirName = newSeedDirName.replace("_1", "_01")
                if (newSeedDirName.endsWith("_2")) newSeedDirName = newSeedDirName.replace("_2", "_02")
                if (newSeedDirName.endsWith("_3")) newSeedDirName = newSeedDirName.replace("_3", "_03")
                if (newSeedDirName.endsWith("_4")) newSeedDirName = newSeedDirName.replace("_4", "_04")
                if (newSeedDirName.endsWith("_5")) newSeedDirName = newSeedDirName.replace("_5", "_05")
                if (newSeedDirName.endsWith("_6")) newSeedDirName = newSeedDirName.replace("_6", "_06")
                if (newSeedDirName.endsWith("_7")) newSeedDirName = newSeedDirName.replace("_7", "_07")
                if (newSeedDirName.endsWith("_8")) newSeedDirName = newSeedDirName.replace("_8", "_08")
                if (newSeedDirName.endsWith("_9")) newSeedDirName = newSeedDirName.replace("_9", "_09")
                if (newSeedDirName.endsWith("_010")) newSeedDirName = newSeedDirName.replace("_010", "_10")

                newSeedDirName = newSeedDirName.replace("_000_", "_00_")
                newSeedDirName = newSeedDirName.replace("_001_", "_01_")

                val newSeedDir = seedDir.resolveSibling(newSeedDirName)

                if (seedDir.toString() != newSeedDir.toString()) {
                    seedDir.moveTo(newSeedDir)
                }
            }
        }

        @Suppress("unused")
        private fun moveToTargetDir(appDir: Path, targetDir: Path, index: String) {
            val appDirApks = appDir.resolve("apks")
            val appDirInput = appDir.resolve("input_rq3").resolve("apks")
            val appDirOutput = appDir.resolve("output_rq3")

            val appName = appDir.fileName.toString()
            val appDirTarget = targetDir.resolve(appName)

            val appDirTargetApks = appDirTarget.resolve("apks")
            val appDirTargetInput = appDirTarget.resolve("input")
            val appDirTargetOutput = appDirTarget.resolve("rq3")

            Files.createDirectories(appDirTargetApks)
            Files.createDirectories(appDirTargetInput)
            Files.createDirectories(appDirTargetOutput)

            if (!Files.exists(appDirApks)) {
                return
            }

            if (Files.exists(appDirInput)) {
                Files.list(appDirInput)
                    .filter { Files.isRegularFile(it) || Files.isDirectory(it) }
                    .forEach { inputFile ->
                        val fileName = inputFile.fileName.toString()
                        var newInputFile = appDirTargetInput.resolve(fileName)

                        if (Files.exists(newInputFile)) {
                            newInputFile = newInputFile.resolveSibling("${fileName}_$index")
                        }

                        inputFile.moveTo(newInputFile)
                    }
            }

            if (Files.exists(appDirOutput)) {
                Files.list(appDirOutput)
                    .filter { Files.isRegularFile(it) || Files.isDirectory(it) }
                    .forEach { outputFile ->
                        val fileName = outputFile.fileName.toString()
                        var newOutputFile = appDirTargetOutput.resolve(fileName)

                        if (Files.exists(newOutputFile)) {
                            newOutputFile = newOutputFile.resolveSibling("${fileName}_$index")
                        }

                        outputFile.moveTo(newOutputFile)
                    }
            }

            if (Files.exists(appDirApks)) {
                Files.list(appDirApks)
                    .filter { Files.isRegularFile(it) }
                    .forEach { apkFile ->
                        val fileName = apkFile.fileName.toString()
                        var newApkFile = appDirTargetApks.resolve(fileName)

                        if (Files.exists(newApkFile)) {
                            newApkFile = newApkFile.resolveSibling("${fileName}_$index")
                        }

                        apkFile.moveTo(newApkFile)
                    }
            }
        }

        @Suppress("unused")
        private fun Path.hash(): Int {
            val str = Files.readAllLines(this).joinToString(" ") { it.trim() }

            return str.hashCode()
        }

        /*
        @ Suppress("unused")
        private fun remove(appDir: Path, expDir: Path) {
            if (expDir.fileName.toString().contains("rq3_")) {
                if (Files.exists(appDir.resolve("input"))) {
                    Files.walk(appDir.resolve("input"))
                        .sorted(Comparator.reverseOrder())
                        .peek { println(it) }
                        .forEach { Files.delete(it) }
                }

                if (Files.exists(appDir.resolve("output"))) {
                    Files.walk(appDir.resolve("output"))
                        .sorted(Comparator.reverseOrder())
                        .peek { println(it) }
                        .forEach { Files.delete(it) }
                }

                if (Files.exists(appDir.resolve("apks"))) {
                    Files.walk(appDir.resolve("apks"))
                        .sorted(Comparator.reverseOrder())
                        .peek { println(it) }
                        .forEach { Files.delete(it) }
                }
            }
        }
        */

        /*
        @Suppress("unused")
        private fun removeInvalidInputFiles(appDir: Path) {
            val inputDir = appDir
                .resolve("input_rq3")
                .resolve("apks")

            // Input Files
            if (Files.exists(inputDir)) {
                val inputFiles = Files.list(inputDir)
                    .asSequence()
                    .filter { Files.isRegularFile(it) }
                    .filter { it.fileName.toString().startsWith("coverageInputs") }
                    .filterNot { it.fileName.toString().contains(".") }
                    .toList()
                    .sorted()

                inputFiles.forEach {
                    Files.delete(it)
                }

                // Grammar
                val grammarFile = inputDir.resolve("grammar.txt")
                Files.deleteIfExists(grammarFile)

                // Translation Table
                val translationTableFile = inputDir.resolve("translationTable.txt")
                Files.deleteIfExists(translationTableFile)

                val translationTableFileCov = inputDir.resolve("translationTableWithCoverage.txt")
                Files.deleteIfExists(translationTableFileCov)

                // Initial Exploration Dir
                val droidmateDir = inputDir.resolve("droidMate")
                if (Files.exists(droidmateDir)) {
                    Files.walk(droidmateDir)
                        .sorted(Comparator.reverseOrder())
                        .peek { println(it) }
                        .forEach { Files.delete(it) }
                }
            }
        }
        */

        @JvmStatic
        fun main(args: Array<String>) {
            // val dir = Paths.get("/Volumes/Experiments/20-icse-regression-with-grammars/")
            val dir = Paths.get("/Users/nataniel/Downloads/exp/")

            // val targetDir = dir.resolve("experiments")
            val targetDir = dir.resolve("rq_2_3").resolve("renamed")
            val expDirs = listOf(
                // dir.resolve("experimentsrq1_1"),
                // dir.resolve("experimentsrq1_2"),

                // dir.resolve("experimentsrq2_1").resolve("experimentsrq2_1"),
                // dir.resolve("experimentsrq2_2").resolve("experimentsrq2_2"),
                // dir.resolve("experimentsrq2_3").resolve("experimentsrq2_3")

                dir.resolve("rq2_3").resolve("rq3")
            )

            expDirs.forEachIndexed { index, expDir ->
                Files.list(expDir)
                    .filter { Files.isDirectory(it) }
                    .forEach { appDir ->
                        val indexStr = index.toString().padStart(2, '0')
                        // removeInvalidInputFiles(appDir)
                        moveInputFiles(appDir, indexStr)
                        moveOutputFiles(appDir, indexStr)
                        // fixSeedDirName(appDir)

                        moveToTargetDir(appDir, targetDir, indexStr)

                        // remove(appDir, expDir)
                    }
            }
        }
    }
}