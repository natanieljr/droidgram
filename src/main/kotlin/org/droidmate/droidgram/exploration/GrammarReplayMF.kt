package org.droidmate.droidgram.exploration

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import org.droidmate.deviceInterface.exploration.ActionType
import org.droidmate.deviceInterface.exploration.ExplorationAction
import org.droidmate.deviceInterface.exploration.GlobalAction
import org.droidmate.exploration.ExplorationContext
import org.droidmate.exploration.actions.click
import org.droidmate.exploration.actions.clickEvent
import org.droidmate.exploration.actions.longClick
import org.droidmate.exploration.actions.longClickEvent
import org.droidmate.exploration.actions.pressBack
import org.droidmate.exploration.actions.resetApp
import org.droidmate.exploration.actions.setText
import org.droidmate.exploration.actions.terminateApp
import org.droidmate.exploration.actions.tick
import org.droidmate.exploration.modelFeatures.ModelFeature
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import java.lang.IllegalStateException
import java.nio.file.Files
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class GrammarReplayMF(
    generatedInput: String,
    private val translationTable: Map<String, UUID>,
    private val delay: Long
) : ModelFeature() {
    companion object {
        const val notReachedTerminals = "notReachedTerminals.txt"
        const val reachedTerminals = "reachedTerminals.txt"
    }

    override val coroutineContext: CoroutineContext = CoroutineName("GrammarReplayMF") + Job()

    private var currIndex: Int = -2
    private lateinit var context: ExplorationContext

    private val missedInputs = mutableListOf<GrammarInput>()
    private val reachedInputs = mutableListOf<GrammarInput>()

    private val inputList by lazy {
        generatedInput
            .split(" ")
            .filter { it.isNotEmpty() }
            .flatMap {
                val actionInput = GrammarInput.fromString(it, translationTable)
                val fetchInput = GrammarInput.createFetch(actionInput)

                listOf(fetchInput, actionInput)
            }
    }

    override fun onAppExplorationStarted(context: ExplorationContext) {
        this.context = context
    }

    private fun Widget.toAction(): ExplorationAction {
        val input = inputList[currIndex]

        return when {
            input.isClick() -> this.click(delay, isVisible = true, ignoreClickable = true)
            input.isClickEvent() -> this.clickEvent(delay, ignoreClickable = true)

            input.isLongClick() -> this.longClick(delay, true)
            input.isLongClickEvent() -> this.longClickEvent(delay, ignoreVisibility = true)

            input.isTick() -> this.tick(delay, ignoreVisibility = true)

            input.isTextInsert() -> this.setText(input.textualInput, ignoreVisibility = true)

            else -> throw IllegalStateException("Unsupported action type: $input")
        }
    }

    @JvmOverloads
    fun nextAction(state: State, printLog: Boolean = false): ExplorationAction {
        currIndex++

        val target = if (currIndex >= 0 && currIndex < inputList.size)
            inputList[currIndex]
        else
            GrammarInput.empty

        val targetUID = target.widget
        val targetWidget = state.actionableWidgets.firstOrNull { it.uid == targetUID }

        val action = when {
            currIndex < 0 -> context.resetApp()

            currIndex >= inputList.size -> terminateApp()

            target.isBack() -> {
                reachedInputs.add(target)
                context.pressBack()
            }

            // Sequence of actions is Fetch -> Execute, if the widget is already here, skip the fetch
            (targetWidget != null) && target.isFetch() -> nextAction(state, false)

            // Widget is on screen, interact with it
            (targetWidget != null) -> {
                reachedInputs.add(target)
                targetWidget.toAction()
            }

            // No widget on screen and is fetch... try fetching
            (target.isFetch()) -> GlobalAction(ActionType.FetchGUI)

            // Widget not on screen, skip and see what happens
            else -> {
                log.warn("Widget is ID: $targetUID was not found, proceeding with input")
                missedInputs.add(target)
                nextAction(state, false)
            }
        }

        if (printLog) {
            log.info("Generating action: $action")
        }

        return action
    }

    private fun writeReport(reportName: String, data: List<GrammarInput>) {
        val sb = StringBuilder()

        data.forEach { input ->
            sb.appendln("${input.grammarId};$input")
        }

        val modelDir = context.model.config.baseDir
        val reportFile = modelDir.resolve(reportName).toAbsolutePath()

        log.info("Writing $reportName report to $reportFile")
        Files.write(reportFile, sb.toString().toByteArray())
    }

    override suspend fun dump(context: ExplorationContext) {
        writeReport(notReachedTerminals, missedInputs)
        writeReport(reachedTerminals, reachedInputs)
    }
}