package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.UiElementPropertiesI
import org.droidmate.explorationModel.ConcreteId
import org.droidmate.explorationModel.config.ModelConfig
import org.droidmate.explorationModel.factory.AbstractModel
import org.droidmate.explorationModel.factory.StateFactory
import org.droidmate.explorationModel.factory.WidgetFactory
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import org.droidmate.explorationModel.toUUID
import java.util.LinkedList

class CustomModel(
    override val config: ModelConfig,
    override val stateProvider: StateFactory<CustomState, CustomWidget>,
    override val widgetProvider: WidgetFactory<CustomWidget>
) : AbstractModel<CustomState, CustomWidget>() {

    private fun createWidget(properties: UiElementPropertiesI, parentInt: Int?): Widget {
        val parent = if (parentInt != null) {
            val parentUID = parentInt.toString().toUUID()
            ConcreteId(parentUID, parentUID)
        } else {
            null
        }
        return CustomWidget(properties, parent)
    }

    /** used on model update to compute the list of UI elements contained in the current UI screen ([State]).
     *  used by ModelParser to create [Widget] object from persisted data
     */
    override fun generateWidgets(elements: Map<Int, UiElementPropertiesI>): Collection<CustomWidget> {
        val widgets = HashMap<Int, CustomWidget>()
        val workQueue = LinkedList<UiElementPropertiesI>().apply {
            addAll(elements.values.filter { it.parentHash == 0 }) // add all roots to the work queue
        }

        check(elements.isEmpty() || workQueue.isNotEmpty()) {
            "ERROR we don't have any roots something went wrong on UiExtraction"
        }

        while (workQueue.isNotEmpty()) {
            with(workQueue.pollFirst()) {
                val parent = if (parentHash != 0) {
                    widgets[parentHash]!!.parentHash
                } else {
                    null
                }

                widgets[idHash] = createWidget(this, parent) as CustomWidget

                childHashes.forEach {
                    // check(elements[it]!=null){"ERROR no element with hashId $it in working queue"}
                    if (elements[it] == null) {
                        logger.warn("could not find child with id $it of widget $this ")
                    } else {
                        workQueue.add(elements[it] ?: error("Element should have been found"))
                    }
                }
            }
        }

        check(widgets.size == elements.size) {
            "ERROR not all UiElements were generated correctly in the model ${elements.filter {
                !widgets.containsKey(
                    it.key
                )
            }.values}"
        }

        assert(elements.all { e -> widgets.values.any { it.idHash == e.value.idHash } }) {
            "ERROR not all UiElements were generated correctly in the model ${elements.filter {
                !widgets.containsKey(
                    it.key
                )
            }}"
        }
        return widgets.values
    }
}