package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.UiElementPropertiesI
import org.droidmate.explorationModel.ConcreteId
import org.droidmate.explorationModel.Model
import org.droidmate.explorationModel.config.ModelConfig
import org.droidmate.explorationModel.interaction.ActionResult
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import org.droidmate.explorationModel.toUUID
import java.util.HashMap
import java.util.LinkedList
import java.util.UUID

class CustomModel(config: ModelConfig) : Model(config) {

    private fun createWidget(properties: UiElementPropertiesI, parentInt: Int?): Widget {
        val parent = if (parentInt != null) {
            val parentUID = parentInt.toString().toUUID()
            ConcreteId(parentUID, parentUID)
        } else {
            null
        }
        return CustomWidget(properties, parent)
    }

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

    @Suppress("UNCHECKED_CAST")
    override fun generateState(action: ActionResult, widgets: Collection<Widget>): State {
        return with(action.guiSnapshot) {
            setChildrenId(widgets as Collection<CustomWidget>)
            CustomState(widgets, isHomeScreen)
        }
    }

    /** used on model update to compute the list of UI elements contained in the current UI screen ([State]).
     *  used by ModelParser to create [Widget] object from persisted data
     */
    override fun generateWidgets(elements: Map<Int, UiElementPropertiesI>): Collection<Widget> {
        val widgets = HashMap<Int, Widget>()
        val workQueue = LinkedList<UiElementPropertiesI>().apply {
            addAll(elements.values.filter { it.parentHash == 0 }) // add all roots to the work queue
        }
        check(elements.isEmpty() || workQueue.isNotEmpty()) { "ERROR we don't have any roots something went wrong on UiExtraction" }
        while (workQueue.isNotEmpty()) {
            with(workQueue.pollFirst()) {
                val parent = if (parentHash != 0) widgets[parentHash]!!.parentHash else null
                widgets[idHash] = createWidget(this, parent)
                childHashes.forEach {
                    // check(elements[it]!=null){"ERROR no element with hashId $it in working queue"}
                    if (elements[it] == null)
                        logger.warn("could not find child with id $it of widget $this ")
                    else workQueue.add(elements[it]!!)
                } // FIXME if null we can try to find element.parentId = this.idHash !IN workQueue as repair function, but why does it happen at all
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