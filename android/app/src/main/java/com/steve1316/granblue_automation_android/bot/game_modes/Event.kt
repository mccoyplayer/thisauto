package com.steve1316.granblue_automation_android.bot.game_modes

import com.steve1316.automation_library.data.SharedData
import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.granblue_automation_android.MainActivity.loggerTag
import com.steve1316.granblue_automation_android.bot.Game


class Event(private val game: Game, private val missionName: String) {
	private val tag: String = "${loggerTag}Event"

	private class EventException(message: String) : Exception(message)

	/**
	 * Checks for Event Nightmare and if it appeared and the user enabled it in settings, start it.
	 *
	 * @return True if Event Nightmare was detected and successfully completed. False otherwise.
	 */
	fun checkEventNightmare(): Boolean {
		if (game.configData.enableNightmare && game.imageUtils.findButton("event_claim_loot") != null) {
			// First check if the Nightmare is skippable.
			if (game.findAndClickButton("event_claim_loot")) {
				MessageLog.printToLog("\n[EVENT] Skippable Event Nightmare detected. Claiming it now...", tag)
				game.collectLoot(isCompleted = false, isEventNightmare = true)
				return true
			} else {
				MessageLog.printToLog("\n[EVENT] Detected Event Nightmare. Starting it now...", tag)

				MessageLog.printToLog("\n********************", tag)
				MessageLog.printToLog("********************", tag)
				MessageLog.printToLog("[EVENT] Event Nightmare", tag)
				MessageLog.printToLog("[EVENT] Event Nightmare Summons: ${game.configData.nightmareSummons}", tag)
				MessageLog.printToLog("[EVENT] Event Nightmare Group Number: ${game.configData.nightmareGroupNumber}", tag)
				MessageLog.printToLog("[EVENT] Event Nightmare Party Number: ${game.configData.nightmarePartyNumber}", tag)
				MessageLog.printToLog("********************", tag)
				MessageLog.printToLog("\n********************", tag)

				// Tap the "Play Next" button to head to the Summon Selection screen.
				game.findAndClickButton("play_next")

				game.wait(1.0)

				// Once the bot is at the Summon Selection screen, select your Summon and Party and start the mission.
				if (game.imageUtils.confirmLocation("select_a_summon")) {
					game.selectSummon(optionalSummonList = game.configData.nightmareSummons)
					val startCheck: Boolean = game.selectPartyAndStartMission(
						optionalGroupNumber = game.configData.nightmareGroupNumber, optionalPartyNumber = game.configData.nightmarePartyNumber,
						bypassFirstRun = true
					)

					// Once preparations are completed, start Combat Mode.
					if (startCheck && game.combatMode.startCombatMode(optionalCombatScript = game.configData.nightmareCombatScript)) {
						game.collectLoot(isCompleted = false, isEventNightmare = true)
						return true
					}
				}
			}
		} else if (!game.configData.enableNightmare && game.imageUtils.findButton("event_claim_loot") != null) {
			// First check if the Nightmare is skippable.
			if (game.findAndClickButton("event_claim_loot")) {
				MessageLog.printToLog("\n[EVENT] Skippable Event Nightmare detected. Claiming it now...", tag)
				game.collectLoot(isCompleted = false, isEventNightmare = true)
				return true
			} else {
				MessageLog.printToLog("\n[EVENT] Event Nightmare detected but user opted to not run it. Moving on...", tag)
				game.findAndClickButton("close")
			}
		} else {
			MessageLog.printToLog("\n[EVENT] No Event Nightmare detected. Moving on...", tag)
		}

		return false
	}

	/**
	 * Navigates to the specified Event (Token Drawboxes) mission.
	 */
	private fun navigateTokenDrawboxes() {
		MessageLog.printToLog("\n[EVENT.TOKEN.DRAWBOXES] Now beginning process to navigate to the mission: $missionName...", tag)

		// Go to the Home screen.
		game.goBackHome(confirmLocationCheck = true)

		// Go to the first banner that is usually the current Event by tapping on the "Menu" button.
		game.findAndClickButton("home_menu")
		game.wait(2.0)

		if (SharedData.displayHeight == 1920) {
			MessageLog.printToLog("[EVENT.TOKEN.DRAWBOXES] Screen too small. Moving the screen down in order to see all of the event banners.", tag)
			game.gestureUtils.swipe(100f, 1000f, 100f, 700f)
			game.wait(0.5)
		}

		var bannerLocations = game.imageUtils.findAll("event_banner")
		if (bannerLocations.size == 0) {
			bannerLocations = game.imageUtils.findAll("event_banner_blue")
		}

		if (game.configData.eventEnableNewPosition) {
			if (game.configData.eventNewPosition > bannerLocations.size - 1) {
				throw EventException("Value set for New Position was found to be invalid compared to the actual number of events found in the Home Menu.")
			}
			game.gestureUtils.tap(bannerLocations[game.configData.eventNewPosition].x, bannerLocations[game.configData.eventNewPosition].y, "event_banner")
		} else game.gestureUtils.tap(bannerLocations[0].x, bannerLocations[0].y, "event_banner")

		game.wait(3.0)

		// Check if there is a "Daily Missions" popup and close it.
		if (game.imageUtils.confirmLocation("event_daily_missions", tries = 3)) {
			MessageLog.printToLog("\n[EVENT.TOKEN.DRAWBOXES] Detected \"Daily Missions\" popup. Closing it...", tag)
			game.findAndClickButton("cancel")
		}

		// Remove the difficulty prefix from the mission name.
		var difficulty = ""
		val formattedMissionName: String
		when {
			missionName.contains("VH ") -> {
				difficulty = "Very Hard"
				formattedMissionName = missionName.substring(3)
			}
			missionName.contains("EX ") -> {
				difficulty = "Extreme"
				formattedMissionName = missionName.substring(3)
			}
			missionName.contains("IM ") -> {
				difficulty = "Impossible"
				formattedMissionName = missionName.substring(3)
			}
			else -> {
				formattedMissionName = missionName
			}
		}

		// Scroll down the screen a little bit for this UI layout that has Token Drawboxes.
		game.gestureUtils.swipe(500f, 1000f, 500f, 700f)

		game.wait(1.0)

		if (formattedMissionName == "Event Quest") {
			MessageLog.printToLog("[EVENT.TOKEN.DRAWBOXES] Now hosting Event Quest...", tag)
			if (!game.findAndClickButton("event_quests")) {
				throw EventException("Failed to proceed any further in Event (Token Drawboxes) navigation by missing the Event Quests button.")
			}

			game.wait(3.0)

			// Find the locations of all round "Play" buttons.
			val playRoundButtonLocations = game.imageUtils.findAll("play_round_button")

			// Now select the chosen difficulty.
			when (difficulty) {
				"Very Hard" -> {
					game.gestureUtils.tap(playRoundButtonLocations[2].x, playRoundButtonLocations[2].y, "play_round_button")
				}
				"Extreme" -> {
					game.gestureUtils.tap(playRoundButtonLocations[3].x, playRoundButtonLocations[3].y, "play_round_button")
				}
			}
		} else if (formattedMissionName == "Event Raid") {
			// Bring up the "Raid Battle" popup. Scroll the screen down a bit in case of small screen size.
			MessageLog.printToLog("[EVENT.TOKEN.DRAWBOXES] Now hosting Event Raid...", tag)
			if (!game.findAndClickButton("event_raid_battle")) {
				throw EventException("Failed to proceed any further in Event (Token Drawboxes) navigation by missing the Event Raids button.")
			}

			game.gestureUtils.swipe(500f, 1000f, 500f, 700f)
			game.wait(0.5)

			// Select the first category if the raids are split into two sections.
			val categories = game.imageUtils.findAll("event_raid_category", customConfidence = 0.9)
			if (categories.size > 0) {
				if (!game.configData.enableSelectBottomCategory) {
					game.gestureUtils.tap(categories[0].x - 50, categories[0].y, "event_raid_category")
				} else {
					game.gestureUtils.tap(categories[1].x - 50, categories[1].y, "event_raid_category")
				}
			}

			val apLocations = game.imageUtils.findAll("ap")

			// Now select the chosen difficulty.
			when (difficulty) {
				"Very Hard" -> {
					game.gestureUtils.tap(apLocations[0].x, apLocations[0].y, "ap")
				}
				"Extreme" -> {
					game.gestureUtils.tap(apLocations[1].x, apLocations[1].y, "ap")
				}
				"Impossible" -> {
					game.gestureUtils.tap(apLocations[2].x, apLocations[2].y, "ap")
				}
			}

			// If the user does not have enough Treasures to host a Extreme or Impossible Raid, host a Very Hard Raid instead.
			if (difficulty == "Extreme" && !game.imageUtils.waitVanish("ap", timeout = 10)) {
				MessageLog.printToLog("[EVENT.TOKEN.DRAWBOXES] Not enough treasures to host Extreme Raid. Hosting Very Hard Raid instead...", tag)
				game.gestureUtils.tap(apLocations[0].x, apLocations[0].y, "ap")
			} else if (difficulty == "Impossible" && !game.imageUtils.waitVanish("ap", timeout = 10)) {
				MessageLog.printToLog("[EVENT.TOKEN.DRAWBOXES] Not enough treasures to host Impossible Raid. Hosting Very Hard Raid instead...", tag)
				game.gestureUtils.tap(apLocations[0].x, apLocations[0].y, "ap")
			}
		}
	}

	/**
	 * Navigates to the specified Event mission.
	 */
	private fun navigate() {
		if (game.configData.farmingMode == "Event (Token Drawboxes)") {
			navigateTokenDrawboxes()
		} else {
			MessageLog.printToLog("\n[EVENT] Now beginning process to navigate to the mission: $missionName...", tag)

			// Go to the Home screen.
			game.goBackHome(confirmLocationCheck = true)

			game.findAndClickButton("quest")

			// Check for the "You retreated from the raid battle" popup.
			if (game.imageUtils.confirmLocation("you_retreated_from_the_raid_battle", tries = 3)) {
				game.findAndClickButton("ok")
			}

			// Go to the Special screen.
			game.findAndClickButton("special")
			game.wait(3.0)

			// Go to the Event section of the Special screen.
			game.findAndClickButton("special_event")

			// Remove the difficulty prefix from the mission name.
			var difficulty = ""
			var formattedMissionName = ""
			when {
				missionName.contains("VH ") -> {
					difficulty = "Very Hard"
					formattedMissionName = missionName.substring(3)
				}
				missionName.contains("EX ") -> {
					difficulty = "Extreme"
					formattedMissionName = missionName.substring(3)
				}
				missionName.contains("EX+ ") -> {
					difficulty = "Extreme+"
					formattedMissionName = missionName.substring(4)
				}
			}

			if (game.imageUtils.confirmLocation("special")) {
				// Check if there is a Nightmare already available.
				val nightmareIsAvailable: Int = if (game.imageUtils.findButton("event_nightmare") != null) {
					1
				} else {
					0
				}

				// Find the locations of all the "Select" buttons.
				val selectButtonLocations = game.imageUtils.findAll("select")
				val position = if (game.configData.enableLocationIncrementByOne) {
					1
				} else {
					0
				}

				// Open up Event Quests or Event Raids. Offset by 1 if there is a Nightmare available.
				if (formattedMissionName == "Event Quest") {
					MessageLog.printToLog("[EVENT] Now hosting Event Quest...", tag)
					game.gestureUtils.tap(selectButtonLocations[position + nightmareIsAvailable].x, selectButtonLocations[position + nightmareIsAvailable].y, "select")
				} else if (formattedMissionName == "Event Raid") {
					MessageLog.printToLog("[EVENT] Now hosting Event Raid...", tag)
					game.gestureUtils.tap(selectButtonLocations[(position + 1) + nightmareIsAvailable].x, selectButtonLocations[(position + 1) + nightmareIsAvailable].y, "select")
				}

				game.wait(3.0)

				// Find the locations of all round "Play" buttons.
				val playRoundButtonLocations = game.imageUtils.findAll("play_round_button")

				// Now select the chosen difficulty.
				when (difficulty) {
					"Very Hard" -> {
						game.gestureUtils.tap(playRoundButtonLocations[0].x, playRoundButtonLocations[0].y, "play_round_button")
					}
					"Extreme" -> {
						game.gestureUtils.tap(playRoundButtonLocations[1].x, playRoundButtonLocations[1].y, "play_round_button")
					}
					"Extreme+" -> {
						game.gestureUtils.tap(playRoundButtonLocations[2].x, playRoundButtonLocations[2].y, "play_round_button")
					}
				}
			} else {
				throw EventException("Failed to arrive at the Special page")
			}
		}
	}

	/**
	 * Starts the process to complete a run for this Farming Mode and returns the number of items detected.
	 *
	 * @param firstRun Flag that determines whether or not to run the navigation process again. Should be False if the Farming Mode supports the "Play Again" feature for repeated runs.
	 */
	fun start(firstRun: Boolean) {
		// Start the navigation process.
		when {
			firstRun -> {
				navigate()
			}
			game.findAndClickButton("play_again") -> {
				if (game.checkForPopups()) {
					navigate()
				}
			}
			else -> {
				// If the bot cannot find the "Play Again" button, check for Pending Battles and then perform navigation again.
				game.checkPendingBattles()
				navigate()
			}
		}

		// Check for AP.
		game.checkAP()

		// Check if the bot is at the Summon Selection screen.
		if (game.imageUtils.confirmLocation("select_a_summon", tries = 30)) {
			if (game.selectSummon()) {
				// Select the Party.
				game.selectPartyAndStartMission()

				// Now start Combat Mode and detect any item drops.
				if (game.combatMode.startCombatMode()) {
					game.collectLoot(isCompleted = true)
				}
			}
		} else {
			throw EventException("Failed to arrive at the Summon Selection screen.")
		}

		return
	}
}