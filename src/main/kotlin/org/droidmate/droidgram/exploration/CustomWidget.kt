package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.UiElementPropertiesI
import org.droidmate.explorationModel.ConcreteId
import org.droidmate.explorationModel.interaction.Widget
import org.droidmate.explorationModel.toUUID
import java.util.UUID

class CustomWidget(properties: UiElementPropertiesI, parentId: ConcreteId?) : Widget(properties, parentId) {
    private val newUidString by lazy {
        listOf(className, packageName, isPassword, isKeyboard, xpath).joinToString(separator = "<;>")
    }

    fun isToast(): Boolean = className.contains("Toast")

    override fun computeUId(): UUID {
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
                if (nlpText.isNotEmpty()) nlpText.toUUID()
                else nlpText.toUUID()
            }

            // we have an Widget without any visible text
            else -> newUidString.toUUID()
        }
    }
}