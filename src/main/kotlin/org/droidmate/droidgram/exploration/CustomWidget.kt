package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.UiElementPropertiesI
import org.droidmate.explorationModel.ConcreteId
import org.droidmate.explorationModel.interaction.Widget
import org.droidmate.explorationModel.toUUID
import org.omg.CORBA.Object
import java.util.UUID

class CustomWidget(properties: UiElementPropertiesI, parentId: ConcreteId?) : Widget(properties, parentId) {
    private val mLock = Object()
    private var idInitialized: Boolean = false
    internal val childrenId: MutableSet<UUID> = mutableSetOf()

    private val newUidString by lazy {
        listOf(className, packageName, isPassword, isKeyboard, xpath).joinToString(separator = "<;>")
    }

    private val uidStringWithChildren by lazy {
        listOf(className, packageName, isPassword, isKeyboard, childrenId.sorted()).joinToString(separator = "<;>")
    }

    fun isToast(): Boolean = className.contains("Toast")

    fun hasValueForId(): Boolean {
        return when {
            isToast() -> false

            resourceId.isNotBlank() -> true

            // special care for EditText elements, as the input text will change the [text] property
            !isKeyboard && isInputField -> when {
                hintText.isNotBlank() -> true
                contentDesc.isNotBlank() -> true
                resourceId.isNotBlank() -> true
                else -> false
            }

            // compute id from textual nlpText if there is any
            !isKeyboard && nlpText.isNotBlank() -> nlpText.isNotEmpty()

            // Images and image buttons are also valid
            className.contains("Image") -> true

            childrenId.isNotEmpty() -> true

            // we have an Widget without any visible text
            else -> false
        }
    }

    override fun computeUId(): UUID {
        synchronized(mLock) {
            check(!idInitialized) { "ID was already initialized" }
            idInitialized = true
        }
        return when {
            resourceId.isNotBlank() -> resourceId.toUUID()

            // special care for EditText elements, as the input text will change the [text] property
            !isKeyboard && isInputField -> when {
                hintText.isNotBlank() -> hintText.toUUID()
                contentDesc.isNotBlank() -> contentDesc.toUUID()
                resourceId.isNotBlank() -> resourceId.toUUID()
                else -> newUidString.toUUID()
            }

            !isKeyboard && nlpText.isNotBlank() -> { // compute id from textual nlpText if there is any
                if (nlpText.isNotEmpty()) {
                    nlpText.toUUID()
                } else {
                    newUidString.toUUID()
                }
            }

            childrenId.isNotEmpty() -> uidStringWithChildren.toUUID()

            // we have an Widget without any visible text
            else -> newUidString.toUUID()
        }
    }

    private fun String.childIfNotEmpty(label: String) = if (isNotBlank()) {
        "$label=$this"
    } else {
        ""
    }

    override fun toString(): String {
        val id = if (idInitialized) {
            uid.toString()
        } else {
            "uninitialized"
        }
        return "interactive=$isInteractive-${id}_$configId: $simpleClassName" +
                "[" +
                "${text.childIfNotEmpty("text")} " +
                "${hintText.childIfNotEmpty("hint")} " +
                "${contentDesc.childIfNotEmpty("description")} " +
                "${resourceId.childIfNotEmpty("resId")}, " +
                "inputType=$inputType $visibleBounds" +
                "]"
    }
}