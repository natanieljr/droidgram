package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.UiElementPropertiesI
import org.droidmate.explorationModel.ConcreteId
import org.droidmate.explorationModel.Model
import org.droidmate.explorationModel.config.ModelConfig
import org.droidmate.explorationModel.interaction.ActionResult
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget

class CustomModel(config: ModelConfig) : Model(config) {

    override fun createWidget(properties: UiElementPropertiesI, parent: ConcreteId?): Widget {
        return CustomWidget(properties, parent)
    }

    override fun generateState(action: ActionResult, widgets: Collection<Widget>): State {
        return with(action.guiSnapshot) {
            CustomState(widgets, isHomeScreen)
        }
    }
}