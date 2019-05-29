package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.ActionType
import org.droidmate.deviceInterface.exploration.Click
import org.droidmate.deviceInterface.exploration.ClickEvent
import org.droidmate.deviceInterface.exploration.LongClick
import org.droidmate.deviceInterface.exploration.LongClickEvent
import org.droidmate.deviceInterface.exploration.Tick
import java.util.UUID

data class GrammarInput(val widget: UUID, val action: String, val textualInput: String) {
    companion object {
        private fun Map<String, UUID>.getUID(key: String): UUID {
            return this[key] ?: throw IllegalArgumentException("Key $key not found in the translation table")
        }

        fun fromString(input: String, translationTable: Map<String, UUID>): GrammarInput {
            val action = input.split("(").first()
            val stateWidget = input.removePrefix("$action(").removeSuffix(")").split(".")
            assert(stateWidget.size <= 2) { "Invalid target action: $input. Expecting at most 1 '.'" }

            val target = when (stateWidget.size) {
                1 -> stateWidget[0]
                2 -> stateWidget[1]
                else -> throw IllegalArgumentException("Unknown target in payload $stateWidget")
            }

            val textualData = input.removeSuffix(")").split(",").last()

            return GrammarInput(translationTable.getUID(target), action, textualData)
        }

        fun createFetch(target: GrammarInput): GrammarInput {
            return GrammarInput(target.widget, ActionType.FetchGUI.name, "")
        }
    }

    fun isFetch(): Boolean = action == ActionType.FetchGUI.name

    fun isClick(): Boolean = action == Click.name
    fun isClickEvent(): Boolean = action == ClickEvent.name

    fun isLongClick(): Boolean = action == LongClick.name
    fun isLongClickEvent(): Boolean = action == LongClickEvent.name

    fun isTick(): Boolean = action == Tick.name
}