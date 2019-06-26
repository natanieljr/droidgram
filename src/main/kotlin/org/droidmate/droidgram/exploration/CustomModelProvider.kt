package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.UiElementPropertiesI
import org.droidmate.explorationModel.ConcreteId
import org.droidmate.explorationModel.config.ModelConfig
import org.droidmate.explorationModel.factory.ModelProvider
import org.droidmate.explorationModel.factory.StateProvider
import org.droidmate.explorationModel.factory.WidgetProvider

class CustomModelProvider : ModelProvider<CustomModel>() {
    private val stateProvider = object : StateProvider<CustomState, CustomWidget>() {
        override fun init(widgets: Collection<CustomWidget>, isHomeScreen: Boolean): CustomState =
            CustomState(widgets, isHomeScreen)
    }

    private val widgetProvider = object : WidgetProvider<CustomWidget>() {
        override fun init(properties: UiElementPropertiesI, parentId: ConcreteId?): CustomWidget =
            CustomWidget(properties, parentId)
    }

    override fun create(config: ModelConfig): CustomModel =
        CustomModel(config = config, stateProvider = stateProvider, widgetProvider = widgetProvider)
}