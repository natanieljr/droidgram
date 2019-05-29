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
import org.droidmate.exploration.actions.resetApp
import org.droidmate.exploration.actions.terminateApp
import org.droidmate.exploration.actions.tick
import org.droidmate.exploration.modelFeatures.ModelFeature
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import java.lang.IllegalStateException
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class GrammarReplayMF(generatedInput: String, val grammarMapping: Map<String, String>) : ModelFeature() {
    override val coroutineContext: CoroutineContext = CoroutineName("GrammarReplayMF") + Job()

    private var currIndex: Int = -2
    private lateinit var context: ExplorationContext

    private val translationTable: Map<String, UUID> by lazy {
        grammarMapping
            .mapValues { UUID.fromString(it.value) }
    }

    private val inputList by lazy {
        generatedInput
            .split(" ")
            .filter { it.isNotEmpty() }
            .flatMap {
                val action = it.split("(").first()
                val data = it.removePrefix("$action(") .removeSuffix(")") .split(".")
                assert(data.size <= 3) { "Invalid target action: $it. Expecting at most 2 '.'" }

                val target = when (data.size) {
                    1 -> data[0]
                    2 -> data[1]
                    3 -> data[1]
                    else -> throw IllegalArgumentException("Unknown target in payload $data")
                }

                listOf(Pair(getUID(target), "FetchGUI"), Pair(getUID(target), action))
            }
    }

    private fun getUID(key: String): UUID {
        return translationTable[key] ?: throw IllegalArgumentException("Key $key not found in the translation table")
    }

    override fun onAppExplorationStarted(context: ExplorationContext) {
        this.context = context
    }

    private fun Widget.toAction(): ExplorationAction {
        val actionStr = inputList[currIndex].second

        return when (actionStr) {
            "Click" -> this.click(0, true)
            "ClickEvent" -> this.clickEvent(0, true)

            "LongClick" -> this.longClick(0, true)
            "LongClickEvent" -> this.longClickEvent(0, true)

            "Tick" -> this.tick(0, true)

            else -> throw IllegalStateException("Unsupported action type: $actionStr")
        }
    }

    @JvmOverloads
    fun nextAction(state: State, printLog: Boolean = false): ExplorationAction {
        currIndex++
        val action = when {
            currIndex >= inputList.size -> terminateApp()

            currIndex < 0 -> context.resetApp()

            else -> {
                val target = inputList[currIndex]
                val targetUID = target.first
                val targetWidget = state.actionableWidgets.firstOrNull { it.uid == targetUID }

                when {
                    // Sequence of actions is Fetch -> Execute, if the widget is already here, skip the fetch
                    (targetWidget != null) && (target.second == "FetchGUI") -> nextAction(state, false)
                    // Widget is on screen, interact with it
                    (targetWidget != null) -> targetWidget.toAction()
                    // No widget on screen and is fetch... try fetching
                    (target.second == "FetchGUI") -> GlobalAction(ActionType.FetchGUI)
                    // Widget not on screen, skip and see what happens
                    else -> {
                        log.warn("Widget is ID: $targetUID was not found, proceeding with input")
                        nextAction(state, false)
                    }
                }
            }
        }

        if (printLog) {
            log.info("Generating action: $action")
        }

        return action
    }
}