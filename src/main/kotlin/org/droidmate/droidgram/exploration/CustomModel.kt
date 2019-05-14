package org.droidmate.droidgram.exploration

import kotlinx.coroutines.runBlocking
import org.droidmate.deviceInterface.exploration.UiElementPropertiesI
import org.droidmate.explorationModel.ConcreteId
import org.droidmate.explorationModel.Model
import org.droidmate.explorationModel.config.ModelConfig
import org.droidmate.explorationModel.interaction.Widget

class CustomModel(config: ModelConfig) : Model(config) {
    companion object {
        @JvmStatic
        fun create(config: ModelConfig): CustomModel {
            return CustomModel(config).apply {
                runBlocking { addState(emptyState) }
            }
        }
    }

    override fun createWidget(properties: UiElementPropertiesI, parent: ConcreteId?): Widget {
        return CustomWidget(properties, parent)
    }
}