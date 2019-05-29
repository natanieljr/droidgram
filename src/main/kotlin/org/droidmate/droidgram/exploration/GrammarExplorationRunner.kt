package org.droidmate.droidgram.exploration

import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.command.ExploreCommandBuilder
import org.droidmate.exploration.SelectorFunction
import org.droidmate.exploration.StrategySelector
import org.droidmate.explorationModel.Model
import org.droidmate.explorationModel.config.ModelConfig

object GrammarExplorationRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val cfg = ExplorationAPI.config(args)

            // val generatedInput = "ClickEvent(w00) ClickEvent(w12) ClickEvent(w13) ClickEvent(w14) ClickEvent(w15) ClickEvent(w16) ClickEvent(w16) ClickEvent(w16) "
            // val generatedInput = "ClickEvent(w03) ClickEvent(w07) ClickEvent(w08) ClickEvent(w09) LongClickEvent(w10) ClickEvent(w11) ClickEvent(w01) LongClickEvent(02)"
            /* val grammarMapping = mapOf(
                "s00" to "d41d8cd9-8f00-3204-a980-0998ecf8427e",
                "s01" to "419bec9f-fc39-3b0e-16fe-695f5a314b88",
                "s02" to "faaba87b-87b3-9f51-d00e-253ae5abafcb",
                "s03" to "c2824066-77a9-0470-97e4-bd25d5a114ea",
                "s04" to "ccfbe586-56d4-b22d-a25e-6245b4ccc2a7",
                "s05" to "bbb058f0-d1d0-1d93-9112-d5b02fc82e0d",
                "s06" to "4502ca77-e000-13c1-1a65-47373df8243b",
                "s07" to "0e940871-6424-c29f-e3f6-8530c21cd319",
                "s08" to "681a0524-a96e-e2e7-3d7c-81e40766f361",
                "s09" to "279efbbb-e61a-c3a9-fd01-787b4412d423",
                "s10" to "8b60d578-d483-760f-60c3-5238327b8689",
                "w00" to "13e7c4cd-d2e8-3b86-854f-1723bd4c6fbe",
                "w01" to "61bbad63-8ccd-3d5a-bd9a-b5a16ff9b954",
                "w02" to "bee0cfcd-c1a8-3ed8-bc34-bca4f1b14dbd",
                "w03" to "d23d13b6-8107-3d59-854d-ee0e8fdadc1c",
                "w04" to "9f3a8d12-280d-3531-9db7-5e2b76bbb191",
                "w05" to "353b3593-30fe-32df-9089-3ac38de79f64",
                "w06" to "dfa74791-ce78-3a32-9c3d-3e89b7edcec5",
                "w07" to "45e5d43e-074d-38b0-85b0-114b4a8acaee",
                "w08" to "cecc1e20-3ff3-3cf3-a935-9f1b8f3bf449",
                "w09" to "78a31c71-3aff-3250-831f-123ed1d661b0",
                "w10" to "2b210f14-f640-3ba6-a764-23ed7ec67c17",
                "w11" to "770224f5-238f-3d74-9e99-10138c82e463",
                "w12" to "1eec5dae-ccf3-3efe-8403-588ef64a6284",
                "w13" to "610b047c-edde-3702-835e-afa060c94059",
                "w14" to "081e8fa2-0e1c-3c10-bca4-7f41382203c9",
                "w15" to "2a428726-5a40-39f3-b141-ac49723aa82c",
                "w16" to "49c4e8d8-d84a-3b01-aac3-f7fd36c8cb82"
                )
            */
            val generatedInput = "ClickEvent(w03) ClickEvent(w04) LongClickEvent(w05) ClickEvent(w01) ClickEvent(w29) ClickEvent(w28) LongClickEvent(w02) ClickEvent(w02) TextInsert(w38,lixxdwxhjctsalrmgb) ClickEvent(w17) ClickEvent(w03) ClickEvent(w07) LongClickEvent(w22) ClickEvent(w23) ClickEvent(w08) ClickEvent(w09) LongClickEvent(w23) LongClickEvent(w10) LongClickEvent(w23) ClickEvent(w11) ClickEvent(w10) ClickEvent(w02) ClickEvent(w13) ClickEvent(w16) ClickEvent(w17) ClickEvent(w25) ClickEvent(w26) Click(w27) ClickEvent(w28) "
            val grammarMapping = mapOf(
                "s00" to "d41d8cd9-8f00-3204-a980-0998ecf8427e",
                "s01" to "419bec9f-fc39-3b0e-16fe-695f5a314b88",
                "s02" to "faaba87b-87b3-9f51-d00e-253ae5abafcb",
                "s03" to "c2824066-77a9-0470-97e4-bd25d5a114ea",
                "s04" to "ccfbe586-56d4-b22d-a25e-6245b4ccc2a7",
                "s05" to "bbb058f0-d1d0-1d93-9112-d5b02fc82e0d",
                "s06" to "4502ca77-e000-13c1-1a65-47373df8243b",
                "s07" to "ad4e1879-42dc-e8b4-82b0-9538a0d4f92e",
                "s08" to "7f737e8c-bf80-8a23-54d5-fb4c1d789a9d",
                "s09" to "1d3ac412-dac1-58bf-f29d-40d238b96939",
                "s10" to "a50a0560-de79-cebe-7a6c-82203c71df38",
                "s11" to "42d14ae6-f9ba-9d5a-1833-c7a657b2add4",
                "s12" to "1a182331-5c35-d5bf-ef7a-9ff0ba2de639",
                "s13" to "0e940871-6424-c29f-e3f6-8530c21cd319",
                "s14" to "7c50ddab-40f5-0723-51b3-5a6a9eed179d",
                "s15" to "baf83d21-c1b4-2f4e-905a-b9e11fac3fc8",
                "s16" to "279efbbb-e61a-c3a9-fd01-787b4412d423",
                "s17" to "66134b35-4e6e-303f-3b75-c7f4ac6640b9",
                "s18" to "b7df68b7-7776-a45b-8d41-e576d56eb4d5",
                "s19" to "d0818fed-c6d8-f4a0-a5e4-0cad24d1051a",
                "s20" to "c919e854-83d8-99d9-9e7c-6513e1d0aa53",
                "s21" to "8b60d578-d483-760f-60c3-5238327b8689",
                "s22" to "b3b58c45-191c-c961-8918-09047714d9db",
                "s23" to "ac5b4df7-7f65-913b-81bd-cab6dd5da1b5",
                "s24" to "ca52bf9e-8e2e-144b-9fb5-3c5dec2624c5",
                "s25" to "71f61db8-3e67-3819-4758-9a779c5f4893",
                "s26" to "e3007223-a28e-6c67-b862-eee300867ce1",
                "s27" to "5e54ecff-2084-db3a-33b7-69be7e7cebb4",
                "s28" to "681a0524-a96e-e2e7-3d7c-81e40766f361",
                "s29" to "a6ae41bb-6e3d-51aa-7c10-be7acc356224",
                "s30" to "eb0f7f36-ba28-6cf0-c071-fbf618207d6a",
                "s31" to "b87b21e6-ddf9-9f7f-8ddd-9ea63bf1aff9",
                "w00" to "13e7c4cd-d2e8-3b86-854f-1723bd4c6fbe",
                "w01" to "61bbad63-8ccd-3d5a-bd9a-b5a16ff9b954",
                "w02" to "bee0cfcd-c1a8-3ed8-bc34-bca4f1b14dbd",
                "w03" to "d23d13b6-8107-3d59-854d-ee0e8fdadc1c",
                "w04" to "9f3a8d12-280d-3531-9db7-5e2b76bbb191",
                "w05" to "353b3593-30fe-32df-9089-3ac38de79f64",
                "w06" to "dfa74791-ce78-3a32-9c3d-3e89b7edcec5",
                "w07" to "45e5d43e-074d-38b0-85b0-114b4a8acaee",
                "w08" to "cecc1e20-3ff3-3cf3-a935-9f1b8f3bf449",
                "w09" to "78a31c71-3aff-3250-831f-123ed1d661b0",
                "w10" to "2b210f14-f640-3ba6-a764-23ed7ec67c17",
                "w11" to "770224f5-238f-3d74-9e99-10138c82e463",
                "w12" to "1eec5dae-ccf3-3efe-8403-588ef64a6284",
                "w13" to "978094e1-d18d-3dca-973c-f63041bb1e7d",
                "w14" to "94ef9d6f-7961-3c09-91d9-88d86edc97b0",
                "w15" to "8a9c2dc2-8fb4-3720-a96b-c9a68577a2fa",
                "w16" to "0943601b-fabb-3c87-bb2f-6010d8a923a0",
                "w17" to "1a721faf-2df5-3972-bfd0-831c64b6146d",
                "w18" to "d4c81532-6151-3138-aeeb-2af76b67950c",
                "w19" to "9783587d-f703-3792-adc1-065afd281e18",
                "w20" to "081e8fa2-0e1c-3c10-bca4-7f41382203c9",
                "w21" to "8f0bb2cc-6ae7-3dfb-88ee-fba534422089",
                "w22" to "2a428726-5a40-39f3-b141-ac49723aa82c",
                "w23" to "d354fd07-34bd-36fc-9079-807deb878518",
                "w24" to "34302dc2-87d9-32c3-a842-7c5b854f2c78",
                "w25" to "d6a42830-94ab-358e-823c-c911f5be7e23",
                "w26" to "610b047c-edde-3702-835e-afa060c94059",
                "w27" to "7ced26ee-5420-3c28-a60e-bc0fa5a473b8",
                "w28" to "49c4e8d8-d84a-3b01-aac3-f7fd36c8cb82",
                "w29" to "099d0d92-a053-30ab-8150-7e2325ea1dc4",
                "w30" to "24fa5e13-1388-3b25-bf77-5cf93bf31f74",
                "w31" to "5d91a827-08d0-3add-8b27-f8eb3cfb668a",
                "w32" to "d7494374-a4ee-3b83-9e15-85023e1f4b06",
                "w33" to "9f178199-fdce-376a-8fe3-265832536d6f",
                "w34" to "e6ee918d-ad2e-3aa1-87be-b9e99cb4a049",
                "w35" to "bff8413c-5045-3c27-b68d-8e2f4c813ff0",
                "w36" to "4a9c56d5-6344-3618-8c6d-8e806d73fa27",
                "w37" to "f664b548-4cb3-3b66-91e8-9f2b808b3340",
                "w38" to "1afa2f7d-0cd4-3c30-8de4-6994a88858ae",
                "w39" to "cd6ba2b0-23d1-328f-998d-b5dcc851018a"
                )

            val selector: SelectorFunction = { _, pool, _ ->
                val strategy = pool.getFirstInstanceOf(GrammarStrategy::class.java)

                // Sync with grammar
                strategy?.grammarWatcher?.join()

                strategy
            }

            val builder = ExploreCommandBuilder.fromConfig(cfg)
                .insertBefore(StrategySelector.startExplorationReset, "Grammar", selector)
                .withStrategy(GrammarStrategy(generatedInput, grammarMapping))

            val modelProvider: (String) -> Model = { appName -> CustomModel(ModelConfig(appName, cfg = cfg)) }
            ExplorationAPI.explore(cfg, commandBuilder = builder, modelProvider = modelProvider)
        }
    }
}