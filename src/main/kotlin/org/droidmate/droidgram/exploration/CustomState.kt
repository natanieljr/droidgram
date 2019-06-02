package org.droidmate.droidgram.exploration

import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget

class CustomState(_widgets: Collection<Widget>, isHomeScreen: Boolean) : State(_widgets, isHomeScreen) {
    override fun isRelevantForId(w: Widget): Boolean {
        return super.isRelevantForId(w) && !(w as CustomWidget).isToast()
    }

    override val actionableWidgets: List<Widget>
        get() = super.actionableWidgets
            .filterNot { (it as CustomWidget).isToast() }
            // .filter { (it as CustomWidget).hasValueForId() }

    override val visibleTargets: List<Widget>
        get() = super.visibleTargets
            .filterNot { (it as CustomWidget).isToast() }
            // .filter { (it as CustomWidget).hasValueForId() }
}