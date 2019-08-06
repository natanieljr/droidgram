package org.droidmate.droidgram.runner

import org.droidmate.deviceInterface.exploration.ActionType
import org.droidmate.deviceInterface.exploration.Click
import org.droidmate.deviceInterface.exploration.ClickEvent
import org.droidmate.deviceInterface.exploration.LongClick
import org.droidmate.deviceInterface.exploration.LongClickEvent
import org.droidmate.deviceInterface.exploration.TextInsert
import org.droidmate.deviceInterface.exploration.Tick
import org.droidmate.explorationModel.toUUID
import java.util.UUID

data class GrammarInput(val grammarId: String, val widget: UUID, val action: String, val textualInput: String) {
    companion object {
        private fun Map<String, UUID>.getUID(key: String): UUID {
            return this[key] ?: throw IllegalArgumentException("Key $key not found in the translation table")
        }

        val empty: GrammarInput
            get() = GrammarInput("empty", "empty".toUUID(), "empty", "empty")

        fun fromString(oiginalInput: String, translationTable: Map<String, UUID>): GrammarInput {
            val input = oiginalInput
                .replace("<", "")
                .replace(">", "")
            val action = input.split("(").first()
            var widget = input
                .removePrefix("$action(")
                .removeSuffix(")")
                .split(",")
                .first()

            if (widget.contains(".")) {
                widget = widget.split(".").last()
            }

            assert(widget != "null") { "Widget must be not null" }

            val textualData = if (input.contains(",")) {
                input.removeSuffix(")")
                    .split(",")
                    .last()
            } else {
                ""
            }

            return GrammarInput(
                input,
                translationTable.getUID(widget),
                action,
                textualData
            )
        }

        fun createFetch(target: GrammarInput): GrammarInput {
            return GrammarInput(
                target.grammarId,
                target.widget,
                ActionType.FetchGUI.name,
                ""
            )
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

    fun isSwipe(): Boolean = action == "Swipe"
}