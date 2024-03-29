package com.steve1316.granblue_automation_android.bot.game_modes

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.automation_library.utils.MyAccessibilityService
import com.steve1316.granblue_automation_android.MainActivity.loggerTag
import com.steve1316.granblue_automation_android.bot.Game
import org.opencv.core.Point


class Raid(private val game: Game) {
	private val tag: String = "${loggerTag}Raid"

	private var joinRoomButtonLocation: Point = Point()
	private var roomCodeTextBoxLocation: Point = Point()
	private var numberOfRaidsJoined = 0
	private var firstInitialization = true

	private class RaidException(message: String) : Exception(message)

	/**
	 * Check and updates the number of Raids currently joined.
	 */
	private fun checkJoinedRaids() {
		val joinedLocations = game.imageUtils.findAll("joined")
		numberOfRaidsJoined = joinedLocations.size
		MessageLog.printToLog("\n[RAID] There are currently $numberOfRaidsJoined raids joined.", tag)
	}

	private fun clearJoinedRaids() {
		// While the user has passed the limit of 3 Raids currently joined, wait and recheck to see if any finish.
		while (numberOfRaidsJoined >= 3) {
			MessageLog.printToLog("[RAID] Detected maximum of 3 raids joined. Waiting 30 seconds to see if any finish.", tag)
			game.wait(30.0)

			game.goBackHome(confirmLocationCheck = true)
			game.findAndClickButton("quest")

			if (game.checkPendingBattles()) {
				game.findAndClickButton("quest")
				game.wait(3.0)
			}

			game.findAndClickButton("raid")
			game.wait(3.0)
			checkJoinedRaids()
		}
	}

	private fun joinRaid() {
		val recoveryTime = 15.0
		var tries = 10
		var joinSuccessful = false

		// Save the locations of the "Join Room" button and the "Room Code" text box.
		if (firstInitialization) {
			game.wait(2.0)
			joinRoomButtonLocation = game.imageUtils.findButton("join_a_room")!!
			roomCodeTextBoxLocation = if (!game.imageUtils.isTablet) {
				if (game.imageUtils.is720p) {
					Point(joinRoomButtonLocation.x - 200.0, joinRoomButtonLocation.y)
				} else {
					Point(joinRoomButtonLocation.x - 400.0, joinRoomButtonLocation.y)
				}
			} else {
				if (!game.imageUtils.isTabletLandscape) {
					Point(joinRoomButtonLocation.x - 300.0, joinRoomButtonLocation.y)
				} else {
					Point(joinRoomButtonLocation.x - 250.0, joinRoomButtonLocation.y)
				}
			}

			firstInitialization = false
		}

		// Loop and try to join a Raid from the parsed list of room codes. If none of the codes worked, wait before trying again.
		while (tries > 0) {
			var roomCodeTries = 30
			while (roomCodeTries > 0) {
				val roomCode = game.twitterRoomFinder.getRoomCode()

				if (roomCode != "") {
					// Set the room code.
					MyAccessibilityService.textToPaste = roomCode

					// Select the "Room Code" text box. The AccessibilityService should pick up that the textbox is a EditText and will paste the
					// room code into it.
					game.gestureUtils.tap(roomCodeTextBoxLocation.x, roomCodeTextBoxLocation.y, "template_room_code_textbox", longPress = true)

					// Wait several seconds to allow enough time for MyAccessibilityService to paste the code.
					game.wait(3.5)

					// Now tap the "Join Room" button.
					game.gestureUtils.tap(joinRoomButtonLocation.x, joinRoomButtonLocation.y, "join_a_room")

					game.wait(2.0)

					if (!game.checkPendingBattles()) {
						if (!game.findAndClickButton("ok")) {
							// Check for EP.
							game.checkEP()

							MessageLog.printToLog("[SUCCESS] Joining $roomCode was successful.", tag)
							numberOfRaidsJoined += 1
							joinSuccessful = true
							break
						} else {
							// Clear the text box by reloading the page.
							MessageLog.printToLog("[WARNING] $roomCode already ended or invalid.", tag)
							game.findAndClickButton("reload")
							game.findAndClickButton("enter_id", tries = 5)
						}
					} else {
						// Move from the Home screen back to the Backup Requests screen after clearing out all the Pending Battles.
						game.findAndClickButton("quest")
						game.findAndClickButton("raid")

						game.wait(2.0)

						checkJoinedRaids()
						game.findAndClickButton("enter_id")
					}
				}

				roomCodeTries -= 1
				game.wait(1.0)
			}

			// Exit condition to indicate a successful join and arriving at the Support Summon Selection screen.
			if (joinSuccessful) {
				break
			}

			tries -= 1
			if (game.configData.enableNoTimeout) {
				tries += 1
				MessageLog.printToLog("[WARNING] Could not find any valid room codes. \nWaiting $recoveryTime seconds and then trying again...", tag)
			} else {
				MessageLog.printToLog("[WARNING] Could not find any valid room codes. \nWaiting $recoveryTime seconds and then trying again with $tries tries left before exiting...", tag)
			}
			game.wait(recoveryTime)
		}
	}

	/**
	 * Navigates to the specified mission.
	 */
	private fun navigate() {
		MessageLog.printToLog("\n[RAID] Now beginning process to navigate to the mission: ${game.configData.missionName}...", tag)

		// Go to the Home screen and then to the Quests screen.
		game.goBackHome(confirmLocationCheck = true)
		game.findAndClickButton("quest")

		game.wait(3.0)

		// Check for the "You retreated from the raid battle" popup.
		if (game.imageUtils.confirmLocation("you_retreated_from_the_raid_battle", tries = 3)) {
			game.findAndClickButton("ok")
		}

		if (game.checkPendingBattles()) {
			game.findAndClickButton("quest")
			game.wait(3.0)
		}

		// Now go to the Backup Requests screen.
		game.findAndClickButton("raid")

		game.wait(3.0)

		if (game.imageUtils.confirmLocation("raid")) {
			// Check for any joined Raids.
			checkJoinedRaids()
			clearJoinedRaids()

			// Move to the "Enter ID" section of the Backup Requests screen.
			MessageLog.printToLog("[RAID] Moving to the \"Enter ID\" section of the Backup Requests screen...", tag)
			if (game.findAndClickButton("enter_id")) {
				joinRaid()
			}
		} else {
			throw RaidException("Failed to reach the Backup Requests screen.")
		}
	}

	/**
	 * Starts the process to complete a run for this Farming Mode and returns the number of items detected.
	 *
	 */
	fun start() {
		// Enable pasting logic in the accessibility service.
		MyAccessibilityService.enableTextToPaste = true

		// Start the navigation process.
		navigate()

		// Check for EP.
		game.checkEP()

		// Check if the bot is at the Summon Selection screen.
		if (game.imageUtils.confirmLocation("select_a_summon", tries = 30)) {
			if (game.selectSummon()) {
				// Select the Party.
				if (game.selectPartyAndStartMission()) {
					// Handle the rare case where joining the Raid after selecting the Summon and Party led the bot to the Quest Results screen with no loot to collect.
					if (game.imageUtils.confirmLocation("no_loot", disableAdjustment = true)) {
						MessageLog.printToLog("\n[RAID] Seems that the Raid just ended. Moving back to the Home screen and joining another Raid...", tag)
					} else {
						// Now start Combat Mode and detect any item drops.
						if (game.combatMode.startCombatMode()) {
							game.collectLoot(isCompleted = true)
						}
					}
				}
			}
		} else {
			throw RaidException("Failed to arrive at the Summon Selection screen.")
		}

		return
	}
}