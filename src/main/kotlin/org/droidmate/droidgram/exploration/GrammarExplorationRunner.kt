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

            //val generatedInput = "ClickEvent(w00) ClickEvent(w12) ClickEvent(w13) ClickEvent(w14) ClickEvent(w15) ClickEvent(w16) ClickEvent(w16) ClickEvent(w16) "
            val generatedInput = "ClickEvent(w03) ClickEvent(w07) ClickEvent(w08) ClickEvent(w09) LongClickEvent(w10) ClickEvent(w11) ClickEvent(w01) LongClickEvent(w02)"
            val grammarMapping = mapOf(
                "s00" to 	"d41d8cd9-8f00-3204-a980-0998ecf8427e",
                "s01" to 	"419bec9f-fc39-3b0e-16fe-695f5a314b88",
                "s02" to 	"faaba87b-87b3-9f51-d00e-253ae5abafcb",
                "s03" to 	"c2824066-77a9-0470-97e4-bd25d5a114ea",
                "s04" to 	"ccfbe586-56d4-b22d-a25e-6245b4ccc2a7",
                "s05" to 	"bbb058f0-d1d0-1d93-9112-d5b02fc82e0d",
                "s06" to 	"4502ca77-e000-13c1-1a65-47373df8243b",
                "s07" to 	"0e940871-6424-c29f-e3f6-8530c21cd319",
                "s08" to 	"681a0524-a96e-e2e7-3d7c-81e40766f361",
                "s09" to 	"279efbbb-e61a-c3a9-fd01-787b4412d423",
                "s10" to 	"8b60d578-d483-760f-60c3-5238327b8689",
                "w00" to 	"13e7c4cd-d2e8-3b86-854f-1723bd4c6fbe",
                "w01" to 	"61bbad63-8ccd-3d5a-bd9a-b5a16ff9b954",
                "w02" to 	"bee0cfcd-c1a8-3ed8-bc34-bca4f1b14dbd",
                "w03" to 	"d23d13b6-8107-3d59-854d-ee0e8fdadc1c",
                "w04" to 	"9f3a8d12-280d-3531-9db7-5e2b76bbb191",
                "w05" to 	"353b3593-30fe-32df-9089-3ac38de79f64",
                "w06" to 	"dfa74791-ce78-3a32-9c3d-3e89b7edcec5",
                "w07" to 	"45e5d43e-074d-38b0-85b0-114b4a8acaee",
                "w08" to 	"cecc1e20-3ff3-3cf3-a935-9f1b8f3bf449",
                "w09" to 	"78a31c71-3aff-3250-831f-123ed1d661b0",
                "w10" to 	"2b210f14-f640-3ba6-a764-23ed7ec67c17",
                "w11" to 	"770224f5-238f-3d74-9e99-10138c82e463",
                "w12" to 	"1eec5dae-ccf3-3efe-8403-588ef64a6284",
                "w13" to 	"610b047c-edde-3702-835e-afa060c94059",
                "w14" to 	"081e8fa2-0e1c-3c10-bca4-7f41382203c9",
                "w15" to 	"2a428726-5a40-39f3-b141-ac49723aa82c",
                "w16" to 	"49c4e8d8-d84a-3b01-aac3-f7fd36c8cb82"
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