package com.steve1316.granblue_automation_android.bot.game_modes

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.granblue_automation_android.MainActivity.loggerTag
import com.steve1316.granblue_automation_android.bot.Game


/**
 * Provides the navigation and any necessary utility functions to handle the Arcarum game mode.
 */
class Arcarum(private val game: Game, private val mapName: String) {
	private val tag: String = "${loggerTag}Arcarum"

	private var firstRun: Boolean = true

	private class ArcarumException(message: String) : Exception(message)

	/**
	 * Navigates to the specified Arcarum expedition.
	 *
	 * @return True if the bot was able to start/resume the expedition. False otherwise.
	 */
	private fun navigateToMap(): Boolean {
		if (firstRun) {
			MessageLog.printToLog("\n[ARCARUM] Now beginning navigation to $mapName.", tag)
			game.goBackHome()

			game.wait(3.0)

			// Scroll up in case of the rare cases where refreshing the page lead to being loaded in on the bottom of the Home page.
			val tempFix: Boolean = game.imageUtils.findButton("home_menu", tries = 1) == null

			// Navigate to the Arcarum banner.
			var tries = 30
			while (tries > 0) {
				if (!game.findAndClickButton("arcarum_banner", tries = 1)) {
					if (tempFix) {
						game.gestureUtils.scroll(scrollDown = false)
					} else {
						game.gestureUtils.scroll()
					}

					game.wait(1.0)

					tries -= 1
					if (tries <= 0) {
						throw (IllegalStateException("Failed to navigate to Arcarum from the Home screen."))
					}
				} else {
					break
				}
			}

			firstRun = false
		} else {
			game.wait(4.0)
		}

		// Now make sure that the Extreme difficulty is selected.
		game.wait(1.0)

		// Check if bot is at Replicard Sandbox.
		if (game.imageUtils.confirmLocation("arcarum_sandbox")) {
			game.findAndClickButton("arcarum_head_back")
			game.wait(1.0)
		}

		// Confirm the completion popup if it shows up.
		if (game.imageUtils.confirmLocation("arcarum_expedition")) {
			game.findAndClickButton("ok")
		}

		game.findAndClickButton("arcarum_extreme")

		// Finally, navigate to the specified map to start it.
		MessageLog.printToLog("[ARCARUM] Now starting the specified expedition: $mapName", tag)
		val formattedMapName: String = mapName.lowercase().replace(" ", "_")

		if (!game.findAndClickButton("arcarum_${formattedMapName}", tries = 10)) {
			// Resume the expedition if it is already in-progress.
			game.findAndClickButton("arcarum_exploring")
		} else if (game.imageUtils.confirmLocation("arcarum_departure_check")) {
			MessageLog.printToLog("[ARCARUM] Now using 1 Arcarum ticket to start this expedition...", tag)
			val resultCheck = game.findAndClickButton("start_expedition")
			game.wait(6.0)
			return resultCheck
		} else if (game.findAndClickButton("resume")) {
			game.wait(3.0)
			return true
		} else {
			throw (IllegalStateException("Failed to encounter the Departure Check to confirm starting the expedition."))
		}

		return false
	}

	/**
	 * Chooses the next action to take for the current Arcarum expedition.
	 *
	 * @return The action to take next.
	 */
	private fun chooseAction(): String {
		// Wait a second in case the "Do or Die" animation plays.
		game.wait(2.0)

		// Determine what action to take.
		var tries = 3
		while (tries > 0) {
			MessageLog.printToLog("\n[ARCARUM] Now determining what action to take with $tries tries remaining...", tag)

			// Prioritise any enemies/chests/thorns that are available on the current node.
			val arcarumActions = game.imageUtils.findAll("arcarum_action")
			if (arcarumActions.size > 0) {
				game.gestureUtils.tap(arcarumActions[0].x, arcarumActions[0].y, "arcarum_action")

				game.wait(2.0)

				game.checkForCAPTCHA()

				return when {
					game.imageUtils.confirmLocation("arcarum_party_selection", tries = 3, bypassGeneralAdjustment = true) -> {
						"Combat"
					}
					game.findAndClickButton("ok", tries = 3, bypassGeneralAdjustment = true) -> {
						"Claimed Treasure/Keythorn"
					}
					else -> {
						"Claimed Spirethorn/No Action"
					}
				}
			}

			// Clear any detected Treasure popup after claiming a chest.
			MessageLog.printToLog("[ARCARUM] No action found for the current node. Looking for Treasure popup...", tag)
			if (game.imageUtils.confirmLocation("arcarum_treasure", tries = 3, bypassGeneralAdjustment = true)) {
				game.findAndClickButton("ok")
				return "Claimed Treasure"
			}

			// Next, determine if there is a available node to move to. Any bound monsters should have been destroyed by now.
			MessageLog.printToLog("[ARCARUM] No Treasure popup detected. Looking for an available node to move to...", tag)
			if (game.findAndClickButton("arcarum_node", tries = 3, bypassGeneralAdjustment = true)) {
				game.wait(1.0)
				return "Navigating"
			}

			if (game.configData.enableStopOnArcarumBoss && checkForBoss()) {
				return "Boss Detected"
			}

			// If all else fails, attempt to navigate to a node that is occupied by mob(s).
			MessageLog.printToLog("[ARCARUM] No available node to move to. Looking for nodes with mobs on them...", tag)
			if (game.findAndClickButton("arcarum_mob", tries = 3, bypassGeneralAdjustment = true) ||
				game.findAndClickButton("arcarum_red_mob", tries = 3, bypassGeneralAdjustment = true)
			) {
				game.wait(1.0)
				return "Navigating"
			}

			// If all else fails, see if there are any unclaimed chests, like the ones spawned by a random special event that spawns chests on all nodes.
			MessageLog.printToLog("[ARCARUM] No nodes with mobs on them. Looking for nodes with chests on them...", tag)
			if (game.findAndClickButton("arcarum_silver_chest", tries = 3, bypassGeneralAdjustment = true) ||
				game.findAndClickButton("arcarum_gold_chest", tries = 3, bypassGeneralAdjustment = true)
			) {
				game.wait(1.0)
				return "Navigating"
			}

			tries -= 1
		}

		MessageLog.printToLog("[ARCARUM] No action can be taken. Defaulting to moving to the next area.", tag)
		return "Next Area"
	}

	/**
	 * Checks for the existence of 3-3, 6-3 or 9-3 boss.
	 *
	 * @return Flag on whether or not a Boss was detected.
	 */
	private fun checkForBoss(): Boolean {
		MessageLog.printToLog("\n[ARCARUM] Checking if boss is available...", tag)

		return if (game.configData.enableStopOnArcarumBoss) {
			return game.imageUtils.findButton("arcarum_boss", tries = 3, bypassGeneralAdjustment = true) != null ||
					game.imageUtils.findButton("arcarum_boss2", tries = 3, bypassGeneralAdjustment = true) != null
		} else {
			false
		}
	}

	/**
	 * Starts the process of completing Arcarum expeditions.
	 *
	 */
	fun start() {
		var runsCompleted = 0
		while (runsCompleted < game.configData.itemAmount) {
			navigateToMap()

			while (true) {
				val action = chooseAction()
				MessageLog.printToLog("[ARCARUM] Action to take will be: $action", tag)

				if (action == "Combat") {
					// Start Combat Mode.
					if (game.selectPartyAndStartMission()) {
						if (game.imageUtils.confirmLocation("elemental_damage")) {
							throw (IllegalStateException("Encountered an important mob for Arcarum and the selected party does not conform to the enemy's weakness. Perhaps you would like to do this battle yourself?"))
						} else if (game.imageUtils.confirmLocation("arcarum_restriction")) {
							throw (IllegalStateException("Encountered a party restriction for Arcarum. Perhaps you would like to complete this section by yourself?"))
						}

						if (game.combatMode.startCombatMode()) {
							game.collectLoot(isCompleted = false, skipInfo = true)
							game.findAndClickButton("expedition")
						}
					}
				} else if (action == "Navigating") {
					// Move to the next available node.
					game.findAndClickButton("move")
				} else if (action == "Next Area") {
					// Either navigate to the next area or confirm the expedition's conclusion.
					if (game.findAndClickButton("arcarum_next_stage")) {
						game.findAndClickButton("ok")
						MessageLog.printToLog("[ARCARUM] Moving to the next area...", tag)
					} else if (game.findAndClickButton("arcarum_checkpoint")) {
						game.findAndClickButton("arcarum")
						MessageLog.printToLog("[ARCARUM] Expedition is complete", tag)
						runsCompleted += 1

						game.wait(1.0)
						game.checkSkyscope()
						break
					}
				} else if (action == "Boss Detected") {
					MessageLog.printToLog("[ARCARUM] Boss has been detected. Stopping the bot.", tag)
					throw ArcarumException("Boss has been detected. Stopping the bot.")
				}
			}
		}
	}
}