package com.steve1316.granblue_automation_android.bot.game_modes

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.granblue_automation_android.MainActivity.loggerTag
import com.steve1316.granblue_automation_android.bot.Game


class Generic(private val game: Game) {
	private val tag: String = "${loggerTag}Generic"

	private class GenericException(message: String) : Exception(message)

	/**
	 * Starts the process of completing a generic setup that supports the 'Play Again' logic.
	 *
	 */
	fun start() {
		MessageLog.printToLog("\n[GENERIC] Now checking for run eligibility...", tag)

		// Bot can start either at the Combat screen with the "Attack" button visible, the Loot Collection screen with the "Play Again" button visible, or the Coop Room screen.
		when {
			game.imageUtils.findButton("attack", tries = 10) != null -> {
				MessageLog.printToLog("\n[GENERIC] Bot is at the Combat screen. Starting Combat Mode now...", tag)
				if (game.combatMode.startCombatMode()) {
					game.collectLoot(isCompleted = true)
				}
			}
			game.findAndClickButton("coop_start", tries = 10) -> {
				MessageLog.printToLog("[GENERIC] Bot is at the Coop Room screen. Starting the Coop mission and Combat Mode now...", tag)

				game.wait(3.0)

				if (game.combatMode.startCombatMode()) {
					game.collectLoot(isCompleted = true)

					// Head back to the Coop Room.
					game.findAndClickButton("coop_room")

					// Check for "Daily Missions" popup for Coop.
					if (game.imageUtils.confirmLocation("coop_daily_missions")) {
						game.findAndClickButton("close")
					}
				}
			}
			else -> {
				MessageLog.printToLog("\n[GENERIC] Bot is not at the Combat or Coop Room screen. Checking for the Loot Collection screen now...", tag)

				// Press the "Play Again" button if necessary, otherwise start Combat Mode.
				if (game.findAndClickButton("play_again")) {
					game.checkForPopups()
				} else {
					throw GenericException(
						"Failed to detect the 'Play Again' button. Bot can start either at the Combat screen with the 'Attack' button visible, the Loot Collection screen with the 'Play Again' button visible, or the Coop Room screen with the 'Start' button visible with the party already selected..."
					)
				}

				// Check for AP.
				game.checkAP()

				// Check if the bot is at the Summon Selection screen.
				if (game.imageUtils.confirmLocation("select_a_summon", tries = 30)) {
					if (game.selectSummon()) {
						// Do not select party and just commence the mission.
						if (game.findAndClickButton("ok", tries = 30)) {
							// Now start Combat Mode and detect any item drops.
							if (game.combatMode.startCombatMode()) {
								game.collectLoot(isCompleted = true)
							}
						} else {
							throw GenericException("Failed to skip party selection.")
						}
					}
				} else {
					throw GenericException("Failed to arrive at the Summon Selection screen.")
				}
			}
		}

		return
	}
}