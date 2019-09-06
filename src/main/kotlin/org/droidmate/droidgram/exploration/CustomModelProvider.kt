package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.UiElementPropertiesI
import org.droidmate.explorationModel.ConcreteId
import org.droidmate.explorationModel.config.ModelConfig
import org.droidmate.explorationModel.factory.ModelProvider
import org.droidmate.explorationModel.factory.StateProvider
import org.droidmate.explorationModel.factory.WidgetProvider
import java.util.UUID

class CustomModelProvider : ModelProvider<CustomModel>() {
    private fun processChildren(widget: CustomWidget, widgets: Collection<CustomWidget>) {
        widget.childrenId.addAll(getChildrenId(widget, widgets))

        val parent = widgets.firstOrNull { it.idHash == widget.parentHash } ?: return
        processChildren(parent, widgets)
    }

    private fun getChildrenId(widget: CustomWidget, widgets: Collection<CustomWidget>): Set<UUID> {
        val children = widgets.filter { widget.childHashes.contains(it.idHash) }
        val result = mutableSetOf<UUID>()

        children.forEach { child ->
            if (child.hasValueForId()) {
                result.add(child.uid)
            }
            result.addAll(getChildrenId(child, widgets))
        }

        return result
    }

    private fun setChildrenId(widgets: Collection<CustomWidget>) {
        val leafs = widgets.sortedBy { it.xpath }
            .filter { it.isLeaf() }

        leafs.forEach { widget ->
            processChildren(widget, widgets)
        }
    }

    private val stateProvider = object : StateProvider<CustomState, CustomWidget>() {
        override fun init(widgets: Collection<CustomWidget>, isHomeScreen: Boolean): CustomState {
            setChildrenId(widgets)
            return CustomState(widgets, isHomeScreen)
        }
    }

    private val widgetProvider = object : WidgetProvider<CustomWidget>() {
        override fun init(properties: UiElementPropertiesI, parentId: ConcreteId?): CustomWidget =
            CustomWidget(properties, parentId)
    }

    override fun create(config: ModelConfig): CustomModel =
        CustomModel(config = config, stateProvider = stateProvider, widgetProvider = widgetProvider)
}