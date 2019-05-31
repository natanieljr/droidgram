package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.ActionType
import org.droidmate.deviceInterface.exploration.Click
import org.droidmate.deviceInterface.exploration.ClickEvent
import org.droidmate.deviceInterface.exploration.LongClick
import org.droidmate.deviceInterface.exploration.LongClickEvent
import org.droidmate.deviceInterface.exploration.TextInsert
import org.droidmate.deviceInterface.exploration.Tick
import java.util.UUID

data class GrammarInput(val grammarId: String, val widget: UUID, val action: String, val textualInput: String) {
    companion object {
        private fun Map<String, UUID>.getUID(key: String): UUID {
            return this[key] ?: throw IllegalArgumentException("Key $key not found in the translation table")
        }

        fun fromString(input: String, translationTable: Map<String, UUID>): GrammarInput {
            val action = input.split("(").first()
            val widget = input.removePrefix("$action(").removeSuffix(")").split(",").first()

            assert(widget != "null") { "Widget must be not null" }

            val textualData = if (input.contains(",")) {
                input.removeSuffix(")").split(",").last()
            } else {
                ""
            }

            return GrammarInput(input, translationTable.getUID(widget), action, textualData)
        }

        fun createFetch(target: GrammarInput): GrammarInput {
            return GrammarInput(target.grammarId, target.widget, ActionType.FetchGUI.name, "")
        }
    }

    fun isFetch(): Boolean = action == ActionType.FetchGUI.name

    fun isClick(): Boolean = action == Click.name
    fun isClickEvent(): Boolean = action == ClickEvent.name

    fun isLongClick(): Boolean = action == LongClick.name
    fun isLongClickEvent(): Boolean = action == LongClickEvent.name

    fun isTick(): Boolean = action == Tick.name

    fun isBack(): Boolean = action == ActionType.PressBack.name

    fun isTextInsert(): Boolean = action == TextInsert.name
}