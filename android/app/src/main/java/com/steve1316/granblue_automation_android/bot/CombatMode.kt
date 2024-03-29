package com.steve1316.granblue_automation_android.bot

import com.steve1316.automation_library.utils.MessageLog
import com.steve1316.granblue_automation_android.MainActivity.loggerTag
import org.opencv.core.Point


/**
 * This class handles the Combat Mode and offers helper functions to assist it.
 */
class CombatMode(private val game: Game, private val debugMode: Boolean = false) {
	private val tag: String = "${loggerTag}CombatMode"

	private var healingItemCommands = listOf(
		"usegreenpotion.target(1)",
		"usegreenpotion.target(2)",
		"usegreenpotion.target(3)",
		"usegreenpotion.target(4)",
		"usebluepotion",
		"usefullelixir",
		"usesupportpotion",
		"useclarityherb.target(1)",
		"useclarityherb.target(2)",
		"useclarityherb.target(3)",
		"useclarityherb.target(4)",
		"userevivalpotion"
	)

	// Save some variables for use throughout the class.
	private var semiAuto = false
	private var fullAuto = false
	private var attackButtonLocation: Point? = null
	private var retreatCheckFlag = false
	private var startTime: Long = 0L
	private val listOfExitEventsForFalse = listOf("Time Exceeded", "No Loot")
	private val listOfExitEventsForTrue = listOf("Battle Concluded", "Exp Gained", "Loot Collected")
	private var commandTurnNumber: Int = 1
	private var turnNumber: Int = 1

	private class CombatModeException(message: String) : Exception(message)

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	// Checks
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////

	/**
	 * Checks if the Party wiped during Combat Mode. Updates the retreat flag if so.
	 */
	private fun checkForWipe() {
		if (debugMode) {
			MessageLog.printToLog("[INFO] Checking to see if Party wiped.", tag)
		}

		val partyWipeIndicatorLocation = game.imageUtils.findButton("party_wipe_indicator", tries = 1, suppressError = true)
		if (partyWipeIndicatorLocation != null || game.imageUtils.confirmLocation("salute_participants", tries = 1, suppressError = true)) {
			if (game.configData.farmingMode != "Raid" && game.configData.farmingMode != "Dread Barrage" && game.imageUtils.confirmLocation("continue")) {
				// Tap on the blue indicator to get rid of the overlay.
				if (partyWipeIndicatorLocation != null) {
					game.gestureUtils.tap(partyWipeIndicatorLocation.x, partyWipeIndicatorLocation.y, "party_wipe_indicator")
				}

				MessageLog.printToLog("[WARNING] Party has wiped during Combat Mode for this non-Raid battle. Retreating now...", tag)

				// Close the popup that asks if you want to use a Full Elixir. Then tap the red "Retreat" button.
				game.findAndClickButton("cancel")
				game.wait(1.0)
				game.findAndClickButton("retreat_confirmation")
				retreatCheckFlag = true
			} else if (game.configData.farmingMode == "Raid" || game.configData.farmingMode == "Dread Barrage" || game.configData.farmingMode == "Guild Wars" || game.configData.missionName.contains("Raid")) {
				MessageLog.printToLog("[WARNING] Party has wiped during Combat Mode for this Raid battle. Backing out now without retreating...", tag)

				// Head back to the Home screen.
				game.goBackHome(confirmLocationCheck = true)
				retreatCheckFlag = true
			} else if (game.configData.farmingMode == "Coop" && game.imageUtils.confirmLocation("salute_participants", tries = 1, suppressError = true)) {
				// Salute the participants.
				MessageLog.printToLog("[WARNING] Party has wiped during Coop Combat Mode. Leaving the Coop Room...", tag)

				game.findAndClickButton("salute")
				game.wait(1.0)
				game.findAndClickButton("ok")

				// Then cancel the popup that asks if you want to use a Full Elixir and then tap the "Leave" button.
				game.findAndClickButton("cancel")
				game.wait(1.0)
				game.findAndClickButton("leave")

				retreatCheckFlag = true
			}
		} else if (debugMode) {
			MessageLog.printToLog("[INFO] Party has not wiped.", tag)
		}
	}

	/**
	 * Checks if there are any dialog popups during Combat Mode from either Lyria or Vyrn and close them.
	 */
	private fun checkForDialog() {
		// Check for Lyria dialog popup first.
		var combatDialogLocation = game.imageUtils.findButton("dialog_lyria", tries = 2, suppressError = true, bypassGeneralAdjustment = true)
		if (combatDialogLocation != null) {
			game.gestureUtils.tap(combatDialogLocation.x, combatDialogLocation.y, "template_dialog")
			return
		}

		// Then check for Vyrn dialog popup next.
		combatDialogLocation = game.imageUtils.findButton("dialog_vyrn", tries = 2, suppressError = true, bypassGeneralAdjustment = true)
		if (combatDialogLocation != null) {
			game.gestureUtils.tap(combatDialogLocation.x, combatDialogLocation.y, "template_dialog")
			return
		}
	}

	/**
	 * Perform checks to see if the battle ended or not.
	 *
	 * @return Return "Nothing" if combat is still continuing. Otherwise, raise a CombatModeException whose message is the event name that caused the battle to end.
	 */
	private fun checkForBattleEnd(): String {
		when {
			game.configData.farmingMode == "Raid" && game.configData.enableAutoExitRaid && (System.currentTimeMillis() - startTime >= game.configData.timeAllowedUntilAutoExitRaid) -> {
				MessageLog.printToLog("\n####################", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("[COMBAT] Combat Mode ended due to exceeding time allowed.", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("####################", tag)
				throw CombatModeException("Time Exceeded")
			}
			retreatCheckFlag || game.imageUtils.confirmLocation("no_loot", tries = 1, suppressError = true, bypassGeneralAdjustment = true) -> {
				MessageLog.printToLog("\n####################", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("[COMBAT] Combat Mode has ended with not loot.", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("####################", tag)
				throw CombatModeException("No Loot")
			}
			game.imageUtils.confirmLocation("battle_concluded", tries = 1, suppressError = true, bypassGeneralAdjustment = true) -> {
				MessageLog.printToLog("\n[COMBAT] Battle concluded suddenly.", tag)
				MessageLog.printToLog("\n####################", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("[COMBAT] Ending Combat Mode.", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("####################", tag)
				game.findAndClickButton("reload")
				throw CombatModeException("Time Exceeded")
			}
			game.imageUtils.confirmLocation("exp_gained", tries = 1, suppressError = true, bypassGeneralAdjustment = true) -> {
				MessageLog.printToLog("\n####################", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("[COMBAT] Ending Combat Mode.", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("####################", tag)
				throw CombatModeException("Exp Gained")
			}
			game.imageUtils.confirmLocation("loot_collected", tries = 1, suppressError = true, bypassGeneralAdjustment = true) -> {
				MessageLog.printToLog("\n####################", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("[COMBAT] Ending Combat Mode.", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("####################", tag)
				throw CombatModeException("Loot Collected")
			}
			else -> {
				return "Nothing"
			}
		}
	}

	/**
	 * Check if the current battle is a raid-like battle.
	 *
	 * @return True if the current battle is a raid-like battle.
	 */
	private fun checkRaid(): Boolean {
		val eventRaids = arrayListOf("VH Event Raid", "EX Event Raid", "IM Event Raid")
		val rotbRaids = arrayListOf("EX Zhuque", "EX Xuanwu", "EX Baihu", "EX Qinglong", "Lvl 100 Shenxian")
		val dreadBarrageRaids = arrayListOf("1 Star", "2 Star", "3 Star", "4 Star", "5 Star")
		val provingGroundsRaids = arrayListOf("Extreme", "Extreme+")
		val guildWarsRaids = arrayListOf("Very Hard", "Extreme", "Extreme+", "NM90", "NM95", "NM100", "NM150")
		val xenoClashRaids = arrayListOf("Xeno Clash Raid")

		return game.configData.farmingMode == "Raid" || eventRaids.contains(game.configData.missionName) || rotbRaids.contains(game.configData.missionName) ||
				dreadBarrageRaids.contains(game.configData.missionName) || game.configData.farmingMode == "Proving Grounds" && provingGroundsRaids.contains(game.configData.missionName) ||
				game.configData.farmingMode == "Guild Wars" && guildWarsRaids.contains(game.configData.missionName) || xenoClashRaids.contains(game.configData.missionName) ||
				game.configData.farmingMode == "Arcarum" || game.configData.farmingMode == "Arcarum Sandbox"
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	// Helper Methods
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////

	/**
	 * Selects the portrait of the specified character during Combat Mode.
	 *
	 * @param characterNumber The character that needs to be selected.
	 */
	private fun selectCharacter(characterNumber: Int) {
		val x = if (!game.imageUtils.isTablet) {
			if (game.imageUtils.is720p) {
				when (characterNumber) {
					1 -> {
						attackButtonLocation!!.x - 480.0
					}
					2 -> {
						attackButtonLocation!!.x - 355.0
					}
					3 -> {
						attackButtonLocation!!.x - 230.0
					}
					else -> {
						attackButtonLocation!!.x - 105.0
					}
				}
			} else {
				when (characterNumber) {
					1 -> {
						attackButtonLocation!!.x - 715.0
					}
					2 -> {
						attackButtonLocation!!.x - 540.0
					}
					3 -> {
						attackButtonLocation!!.x - 350.0
					}
					else -> {
						attackButtonLocation!!.x - 180.0
					}
				}
			}
		} else {
			if (!game.imageUtils.isTabletLandscape) {
				when (characterNumber) {
					1 -> {
						attackButtonLocation!!.x - 530.0
					}
					2 -> {
						attackButtonLocation!!.x - 400.0
					}
					3 -> {
						attackButtonLocation!!.x - 265.0
					}
					else -> {
						attackButtonLocation!!.x - 130.0
					}
				}
			} else {
				// 563, 730
				when (characterNumber) {
					1 -> {
						attackButtonLocation!!.x - 415.0
					}
					2 -> {
						attackButtonLocation!!.x - 315.0
					}
					3 -> {
						attackButtonLocation!!.x - 200.0
					}
					else -> {
						attackButtonLocation!!.x - 100.0
					}
				}
			}
		}

		val y = if (!game.imageUtils.isTablet) {
			if (game.imageUtils.is720p) {
				attackButtonLocation!!.y + 185.0
			} else {
				attackButtonLocation!!.y + 290.0
			}
		} else {
			if (!game.imageUtils.isTabletLandscape) {
				attackButtonLocation!!.y + 220.0
			} else {
				attackButtonLocation!!.y + 170.0
			}
		}

		// Double tap the Character portrait to avoid any popups caused by other Raid participants.
		game.gestureUtils.tap(x, y, "template_character")
		game.gestureUtils.tap(x, y, "template_character")
	}

	/**
	 * Determine whether or not to reload after an Attack.
	 *
	 * @param override Override the set checks and reload anyways. Defaults to false.
	 * @return True if the bot reloaded the page. False otherwise.
	 */
	private fun reloadAfterAttack(override: Boolean = false): Boolean {
		// If the "Cancel" button vanishes, that means the attack is in-progress. Now reload the page and wait for either the attack to finish or Battle ended.
		if (game.configData.enableRefreshDuringCombat && (checkRaid() || override || (game.configData.farmingMode == "Generic" && game.configData.enableForceReload))) {
			if (checkForBattleEnd() == "Nothing") {
				MessageLog.printToLog("[COMBAT] Reloading now.", tag)
				game.findAndClickButton("reload")
				if (game.configData.enableCombatModeAdjustment) {
					game.wait(game.configData.adjustWaitingForReload.toDouble())
				} else {
					game.wait(3.0)
				}

				return true
			}
		}

		return false
	}

	/**
	 * Processes a Turn if its currently the incorrect Turn number.
	 *
	 */
	private fun processIncorrectTurn() {
		// Clear any detected dialog popups that might obstruct the "Attack" button.
		checkForDialog()

		// Wait for the Attack to process.
		MessageLog.printToLog("[COMBAT] Ending Turn ${turnNumber}...", tag)
		if (!fullAuto && !semiAuto) {
			game.findAndClickButton("attack", tries = 30)
			while (game.imageUtils.findButton("cancel", suppressError = true) != null) {
				if (debugMode) {
					MessageLog.printToLog("[DEBUG] While waiting for the incorrect turn to process , the \"Cancel\" button has not vanished from the screen yet.", tag)
				}

				game.wait(1.0)
			}
		} else {
			while (game.imageUtils.findButton("attack", suppressError = true) != null) {
				if (debugMode) {
					MessageLog.printToLog("[DEBUG] While waiting for the incorrect turn to process , the \"Attack\" button has not vanished from the screen yet.", tag)
				}

				game.wait(1.0)
			}
		}

		var reloadCheck = false

		// If the next Turn is the current Turn block, turn off auto.
		if (turnNumber + 1 == commandTurnNumber) {
			reloadCheck = reloadAfterAttack()
			if (!reloadCheck) {
				if (fullAuto) {
					game.findAndClickButton("full_auto_enabled", tries = 10)
				} else {
					game.findAndClickButton("semi_auto_enabled", tries = 10)
				}
			}

			fullAuto = false
			semiAuto = false
		}

		game.wait(1.0)

		// If the bot reloaded the page, determine if bot needs to enable Full/Semi Auto again.
		if (!reloadCheck) {
			reloadCheck = reloadAfterAttack()
			if (reloadCheck && fullAuto) {
				enableFullAuto()
			} else if (reloadCheck && semiAuto) {
				enableSemiAuto()
			}
		}

		waitForAttack()

		MessageLog.printToLog("[COMBAT] Turn $turnNumber has ended.", tag)

		if (game.findAndClickButton("next", tries = 3, suppressError = true)) {
			game.wait(3.0)
		}

		turnNumber += 1

		MessageLog.printToLog("[COMBAT] Starting Turn ${turnNumber}.", tag)
	}

	/**
	 * Wait several tries until the bot sees either the "Attack" or the "Next" button before starting a new turn.
	 *
	 * @return True if Attack ended into the next Turn. False if Attack ended but combat also ended as well.
	 */
	private fun waitForAttack(): Boolean {
		MessageLog.printToLog("[COMBAT] Waiting for attack to end...", tag)
		var tries = if (game.configData.enableCombatModeAdjustment) {
			game.configData.adjustWaitingForAttack
		} else {
			100
		}

		while (tries > 0 && !retreatCheckFlag && game.imageUtils.findButton("attack", tries = 1, suppressError = true) == null &&
			game.imageUtils.findButton("next", tries = 1, suppressError = true) == null
		) {
			checkForDialog()

			// Check if the Party wiped after attacking.
			checkForWipe()

			checkForBattleEnd()

			tries -= 1
		}

		MessageLog.printToLog("[COMBAT] Attack ended.", tag)

		return true
	}

	/**
	 * Enable Full/Semi auto for this battle.
	 *
	 * @return True if Full/Semi auto is enabled.
	 */
	private fun enableAuto(): Boolean {
		if (game.configData.enableRefreshDuringCombat && game.configData.enableAutoQuickSummon) {
			MessageLog.printToLog("[COMBAT] Automatically attempting to use Quick Summon...", tag)
			quickSummon()
		}


		var enabledAuto = game.findAndClickButton("full_auto") || game.findAndClickButton("full_auto_enabled")

		// If the bot failed to find and click the "Full Auto" button, fallback to the "Semi Auto" button.
		if (!enabledAuto) {
			MessageLog.printToLog("[COMBAT] Failed to find the \"Full Auto\" button. Falling back to Semi Auto.", tag)
			MessageLog.printToLog("[COMBAT] Double checking to see if Semi Auto is enabled.", tag)

			val enabledSemiAutoButtonLocation = game.imageUtils.findButton("semi_auto_enabled")
			if (enabledSemiAutoButtonLocation == null) {
				// Have the Party attack and then attempt to see if the "Semi Auto" button becomes visible.
				game.findAndClickButton("attack")

				game.wait(2.0)

				enabledAuto = game.findAndClickButton("semi_auto", tries = 10)
				if (enabledAuto) {
					MessageLog.printToLog("[COMBAT] Semi Auto is now enabled.", tag)
				}
			}
		} else {
			MessageLog.printToLog("[COMBAT] Enabled Full Auto.", tag)
		}

		return enabledAuto
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	// Commands
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////

	/**
	 * Start the Turn based on the read command and move the internal Turn count forward to match the command.
	 *
	 * @param command The command to execute.
	 */
	private fun startTurn(command: String) {
		// Clear any detected dialog popups that might obstruct the "Attack" button.
		checkForDialog()

		// Parse the Turn's number.
		commandTurnNumber = (command.split(":")[0].split(" ")[1]).toInt()

		// If the command is a "Turn #:" and it is currently not the correct Turn, attack until the Turn numbers match.
		if (!retreatCheckFlag && turnNumber != commandTurnNumber) {
			MessageLog.printToLog("[COMBAT] Attacking until the bot reaches Turn $commandTurnNumber.", tag)

			while (turnNumber != commandTurnNumber) {
				processIncorrectTurn()
			}
		} else {
			MessageLog.printToLog("\n[COMBAT] Starting Turn $turnNumber.", tag)
		}
	}

	/**
	 * Ends the Turn by clicking the Attack button.
	 *
	 */
	private fun endTurn() {
		// Tap the "Attack" button once every command inside the Turn Block has been processed.
		MessageLog.printToLog("[COMBAT] Ending Turn ${turnNumber}...", tag)

		if (fullAuto || semiAuto) {
			while (game.imageUtils.findButton("attack") != null) {
				game.wait(1.0)
			}
		} else {
			game.findAndClickButton("attack", tries = 10)

			// Wait until the "Cancel" button vanishes from the screen.
			if (game.imageUtils.findButton("combat_cancel", tries = 10) != null) {
				while (!game.imageUtils.waitVanish("combat_cancel", timeout = 5, suppressError = true)) {
					if (debugMode) {
						MessageLog.printToLog("[DEBUG] The \"Cancel\" button has not vanished from the screen yet.", tag)
					}

					game.wait(1.0)
				}
			}
		}

		// Check for exit conditions.
		checkForBattleEnd()

		if (game.findAndClickButton("next", tries = 3, suppressError = true)) {
			game.wait(3.0)
		}
	}

	/**
	 * Execute a wait command.
	 *
	 * @param commandList A split list of the command by its "." delimiter with the "wait" command being the first element.
	 * @param fallbackDelay A default delay if the wait command was invalid. Defaults to 1.0 second.
	 */
	private fun waitExecute(commandList: List<String>, fallbackDelay: Double = 1.0) {
		// Isolate the seconds inside the command.
		val waitCommand = if (commandList[0].contains(")")) {
			commandList[0].substringAfter("(").replace(")", "")
		} else {
			commandList[0].substringAfter("(") + "." + commandList[1].replace(")", "")
		}

		try {
			val waitSeconds = waitCommand.toDouble()
			MessageLog.printToLog("[COMBAT] Now waiting $waitSeconds second(s).", tag)
			game.wait(waitSeconds)
		} catch (e: Exception) {
			MessageLog.printToLog("[COMBAT] Could not parse out the seconds in the wait command. Waiting $fallbackDelay second(s) as fallback.", tag)
			game.wait(fallbackDelay)
		}
	}

	/**
	 * Uses the specified healing item during Combat Mode with an optional target if the item requires one.
	 *
	 * @param command The command for the healing item to use.
	 */
	private fun useCombatHealingItem(command: String) {
		if (debugMode) {
			MessageLog.printToLog("\n[DEBUG] Using item: $command", tag)
		}

		var target = 0

		// Grab the healing command.
		val healingItemCommandList = command.split(".")
		val healingItemCommand = healingItemCommandList[0]
		healingItemCommandList.drop(1)

		// Parse the target if the user is using a Green Potion or a Clarity Herb.
		if ((healingItemCommand == "usegreenpotion" || healingItemCommand == "useclarityherb") && healingItemCommandList[0].contains("target")) {
			when (healingItemCommandList[0]) {
				"target(1)" -> {
					target = 1
				}
				"target(2)" -> {
					target = 2
				}
				"target(3)" -> {
					target = 3
				}
				"target(4)" -> {
					target = 4
				}
			}
		}

		// Open up the "Use Item" popup.
		game.findAndClickButton("heal")

		// Format the item name.
		val formattedCommand = command.lowercase().replace(" ", "_")

		// Tap the specified item.
		if (formattedCommand == "usebluepotion" || formattedCommand == "usesupportpotion") {
			// Blue and Support Potions share the same image but they are at different positions on the screen.
			val potionLocations = game.imageUtils.findAll(formattedCommand)
			if (formattedCommand == "usebluepotion") {
				game.gestureUtils.tap(potionLocations[0].x, potionLocations[0].y, formattedCommand)
			} else {
				game.gestureUtils.tap(potionLocations[1].x, potionLocations[1].y, formattedCommand)
			}
		} else {
			game.findAndClickButton(formattedCommand)
		}

		// After the initial popup vanishes to reveal a new popup, either select a Character target or tap the confirmation button.
		if (game.imageUtils.waitVanish("tap_the_item_to_use", timeout = 5)) {
			when (formattedCommand) {
				"usegreenpotion" -> {
					MessageLog.printToLog("[COMBAT] Using Green Potion on Character $target.", tag)
					selectCharacter(target)
				}
				"usebluepotion" -> {
					MessageLog.printToLog("[COMBAT] Using Blue Potion on the whole Party.", tag)
					game.findAndClickButton("use")
				}
				"usefullelixir" -> {
					MessageLog.printToLog("[COMBAT] Using Full Elixir to revive and gain Full Charge.", tag)
					game.findAndClickButton("ok")
				}
				"usesupportpotion" -> {
					MessageLog.printToLog("[COMBAT] Using Support Potion on the whole Party.", tag)
					game.findAndClickButton("ok")
				}
				"useclarityherb" -> {
					MessageLog.printToLog("[COMBAT] Using Clarity Herb on Character $target.", tag)
					selectCharacter(target)
				}
				"userevivalpotion" -> {
					MessageLog.printToLog("[COMBAT] Using Revival Potion to revive the whole Party.", tag)
					game.findAndClickButton("ok")
				}
			}

			// Wait for the healing animation to finish.
			game.wait(1.0)

			if (!game.imageUtils.confirmLocation("use_item", tries = 1)) {
				MessageLog.printToLog("[SUCCESS] Successfully used healing item.", tag)
			} else {
				MessageLog.printToLog("[WARNING] Was not able to use the healing item. Canceling it now.", tag)
			}
		} else {
			MessageLog.printToLog("[WARNING] Failed to tap on the item. Either it does not exist for this particular Mission or you ran out.", tag)
			game.findAndClickButton("cancel")
		}
	}

	/**
	 * Request backup during Combat mode for this Raid.
	 */
	private fun requestBackup() {
		MessageLog.printToLog("\n[COMBAT] Now requesting Backup for this Raid.", tag)

		// Scroll the screen down a little bit to have the "Request Backup" button visible on all screen sizes. Then tap the button.
		game.gestureUtils.swipe(500f, 1000f, 500f, 400f)
		game.findAndClickButton("request_backup")

		game.wait(1.0)

		// Find the location of the "Cancel" button and tap the "Request Backup" button to the right of it. This is to ensure that the bot always
		// taps the button no matter the appearance of the "Request Backup" button, which changes frequently.
		val cancelButtonLocation = game.imageUtils.findButton("cancel")
		if (cancelButtonLocation != null) {
			if (!game.imageUtils.isTablet) {
				if (!game.imageUtils.is720p) {
					game.gestureUtils.tap(cancelButtonLocation.x + 340, cancelButtonLocation.y, "cancel")
				} else {
					game.gestureUtils.tap(cancelButtonLocation.x + 500, cancelButtonLocation.y, "cancel")
				}
			} else {
				if (!game.imageUtils.isTabletLandscape) {
					game.gestureUtils.tap(cancelButtonLocation.x + 370, cancelButtonLocation.y, "cancel")
				} else {
					game.gestureUtils.tap(cancelButtonLocation.x + 285, cancelButtonLocation.y, "cancel")
				}
			}
		}

		game.wait(1.0)

		// If requesting backup was successful, close the popup.
		if (game.imageUtils.confirmLocation("request_backup_success", tries = 1)) {
			MessageLog.printToLog("[COMBAT] Successfully requested Backup.", tag)
			game.findAndClickButton("ok")
		} else {
			MessageLog.printToLog("[COMBAT] Unable to request Backup. Possibly because it is still on cooldown.", tag)
			game.findAndClickButton("cancel")
		}

		// Now scroll back up to reset the view.
		game.gestureUtils.swipe(500f, 400f, 500f, 1000f)
	}

	/**
	 * Request backup during Combat mode for this Raid by using the Twitter feature.
	 */
	private fun tweetBackup() {
		MessageLog.printToLog("\n[COMBAT] Now requesting Backup for this Raid via Twitter.", tag)

		// Scroll the screen down a little bit to have the "Request Backup" button visible on all screen sizes. Then tap the button.
		game.gestureUtils.swipe(500f, 1000f, 500f, 400f)
		game.findAndClickButton("request_backup")

		game.wait(1.0)

		// Now tap the "Tweet" button.
		game.findAndClickButton("request_backup_tweet")
		game.wait(1.0)
		game.findAndClickButton("ok")

		game.wait(1.0)

		// If requesting backup was successful, close the popup.
		if (game.imageUtils.confirmLocation("request_backup_tweet_success", tries = 1)) {
			MessageLog.printToLog("[COMBAT] Successfully requested Backup via Twitter.", tag)
			game.findAndClickButton("ok")
		} else {
			MessageLog.printToLog("[COMBAT] Unable to request Backup via Twitter. Possibly because it is still on cooldown.", tag)
			game.findAndClickButton("cancel")
		}

		// Now scroll back up to reset the view.
		game.gestureUtils.swipe(500f, 400f, 500f, 1000f)
	}


	/**
	 * Selects the targeted enemy.
	 *
	 * @param command The command to be executed.
	 */
	private fun selectEnemyTarget(command: String) {
		for (target in 1..3) {
			if (command == "targetenemy(${target})") {
				val x: Double

				// Select the enemy target on the screen.
				when (target) {
					1 -> {
						x = if (!game.imageUtils.isTablet) {
							if (game.imageUtils.is720p) {
								400.0
							} else {
								626.0
							}
						} else {
							if (!game.imageUtils.isTabletLandscape) {
								458.0
							} else {
								360.0
							}
						}
					}
					2 -> {
						x = if (!game.imageUtils.isTablet) {
							if (game.imageUtils.is720p) {
								165.0
							} else {
								253.0
							}
						} else {
							if (!game.imageUtils.isTabletLandscape) {
								183.0
							} else {
								150.0
							}
						}
					}
					else -> {
						x = if (!game.imageUtils.isTablet) {
							if (game.imageUtils.is720p) {
								-75.0
							} else {
								-85.0
							}
						} else {
							if (!game.imageUtils.isTabletLandscape) {
								-67.0
							} else {
								-52.0
							}
						}
					}
				}

				val y: Double = if (!game.imageUtils.isTablet) {
					if (game.imageUtils.is720p) {
						430.0
					} else {
						667.0
					}
				} else {
					if (!game.imageUtils.isTabletLandscape) {
						478.0
					} else {
						378.0
					}
				}

				game.gestureUtils.tap(attackButtonLocation!!.x - x, attackButtonLocation!!.y - y, "template_enemy_target")
				game.findAndClickButton("set_target")
				MessageLog.printToLog("[COMBAT] Targeted Enemy #${target}.", tag)
			}
		}
	}

	/**
	 * Activate the specified Skill for the already selected Character.
	 *
	 * @param characterNumber The Character whose Skill needs to be used.
	 * @param skillCommandList The commands to be executed.
	 * @return Return True if the Turn will end due to a chained "attack" command. False otherwise.
	 */
	private fun useCharacterSkill(characterNumber: Int, skillCommandList: List<String>): Boolean {
		var tempSkillCommandList: List<String> = skillCommandList

		// Drop the first element if its the Character command.
		if (tempSkillCommandList[0].contains("character")) {
			tempSkillCommandList = tempSkillCommandList.drop(1)
		}

		while (tempSkillCommandList.isNotEmpty()) {
			// Stop if the Next button is present.
			if (game.imageUtils.findButton("next", tries = 1, suppressError = true) != null) {
				return false
			}

			if (tempSkillCommandList[0].contains("wait")) {
				waitExecute(tempSkillCommandList)
				tempSkillCommandList = tempSkillCommandList.drop(1)
			} else if (tempSkillCommandList[0].contains("attack")) {
				end()
				return true
			} else {
				val x = when (tempSkillCommandList[0]) {
					"useskill(1)" -> {
						MessageLog.printToLog("[COMBAT] Character $characterNumber uses Skill 1.", tag)
						if (!game.imageUtils.isTablet) {
							if (game.imageUtils.is720p) {
								attackButtonLocation!!.x - 320.0
							} else {
								attackButtonLocation!!.x - 485.0
							}
						} else {
							if (!game.imageUtils.isTabletLandscape) {
								attackButtonLocation!!.x - 356.0
							} else {
								attackButtonLocation!!.x - 275.0
							}
						}
					}
					"useskill(2)" -> {
						MessageLog.printToLog("[COMBAT] Character $characterNumber uses Skill 2.", tag)
						if (!game.imageUtils.isTablet) {
							if (game.imageUtils.is720p) {
								attackButtonLocation!!.x - 195.0
							} else {
								attackButtonLocation!!.x - 295.0
							}
						} else {
							if (!game.imageUtils.isTabletLandscape) {
								attackButtonLocation!!.x - 216.0
							} else {
								attackButtonLocation!!.x - 170.0
							}
						}
					}
					"useskill(3)" -> {
						MessageLog.printToLog("[COMBAT] Character $characterNumber uses Skill 3.", tag)
						if (!game.imageUtils.isTablet) {
							if (game.imageUtils.is720p) {
								attackButtonLocation!!.x - 70.0
							} else {
								attackButtonLocation!!.x - 105.0
							}
						} else {
							if (!game.imageUtils.isTabletLandscape) {
								attackButtonLocation!!.x - 77.0
							} else {
								attackButtonLocation!!.x - 60.0
							}
						}
					}
					"useskill(4)" -> {
						MessageLog.printToLog("[COMBAT] Character $characterNumber uses Skill 4.", tag)
						if (!game.imageUtils.isTablet) {
							if (game.imageUtils.is720p) {
								attackButtonLocation!!.x + 55.0
							} else {
								attackButtonLocation!!.x + 85.0
							}
						} else {
							if (!game.imageUtils.isTabletLandscape) {
								attackButtonLocation!!.x + 65.0
							} else {
								attackButtonLocation!!.x - 45.0
							}
						}
					}
					else -> {
						MessageLog.printToLog("[WARNING] Invalid command received for using the Character's Skill.", tag)
						game.findAndClickButton("back")
						return false
					}
				}

				tempSkillCommandList = tempSkillCommandList.drop(1)

				val y = if (!game.imageUtils.isTablet) {
					if (game.imageUtils.is720p) {
						attackButtonLocation!!.y + 255.0
					} else {
						attackButtonLocation!!.y + 395.0
					}
				} else {
					if (!game.imageUtils.isTabletLandscape) {
						attackButtonLocation!!.y + 287.0
					} else {
						attackButtonLocation!!.y + 230.0
					}
				}

				// Double tap the Skill to avoid any popups caused by other Raid participants.
				game.wait(0.5)
				game.gestureUtils.tap(x, y, "template_skill")

				game.wait(1.0)

				// Check if the Skill requires a target.
				if (game.imageUtils.confirmLocation("use_skill", bypassGeneralAdjustment = true)) {
					if (tempSkillCommandList.isNotEmpty()) {
						val selectCharacterLocation = game.imageUtils.findButton("select_a_character")

						when {
							selectCharacterLocation != null -> {
								game.wait(0.5)

								// Select the targeted Character.
								when (tempSkillCommandList[0]) { // 538, 688
									"target(1)" -> {
										MessageLog.printToLog("[COMBAT] Targeting Character 1 for Skill.", tag)
										if (!game.imageUtils.isTablet) {
											if (game.imageUtils.is720p) {
												game.gestureUtils.tap(selectCharacterLocation.x - 140.0, selectCharacterLocation.y + 125.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x - 195.0, selectCharacterLocation.y + 195.0, "template_target")
											}
										} else {
											if (!game.imageUtils.isTabletLandscape) {
												game.gestureUtils.tap(selectCharacterLocation.x - 150.0, selectCharacterLocation.y + 135.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x - 115.0, selectCharacterLocation.y + 115.0, "template_target")
											}
										}
									}
									"target(2)" -> {
										MessageLog.printToLog("[COMBAT] Targeting Character 2 for Skill.", tag)
										if (!game.imageUtils.isTablet) {
											if (game.imageUtils.is720p) {
												game.gestureUtils.tap(selectCharacterLocation.x + 5.0, selectCharacterLocation.y + 125.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x + 5.0, selectCharacterLocation.y + 195.0, "template_target")
											}
										} else {
											if (!game.imageUtils.isTabletLandscape) {
												game.gestureUtils.tap(selectCharacterLocation.x + 5.0, selectCharacterLocation.y + 135.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x + 5.0, selectCharacterLocation.y + 115.0, "template_target")
											}
										}
									}
									"target(3)" -> {
										MessageLog.printToLog("[COMBAT] Targeting Character 3 for Skill.", tag)
										if (!game.imageUtils.isTablet) {
											if (game.imageUtils.is720p) {
												game.gestureUtils.tap(selectCharacterLocation.x + 135.0, selectCharacterLocation.y + 125.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x + 210.0, selectCharacterLocation.y + 195.0, "template_target")
											}
										} else {
											if (!game.imageUtils.isTabletLandscape) {
												game.gestureUtils.tap(selectCharacterLocation.x + 155.0, selectCharacterLocation.y + 135.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x + 125.0, selectCharacterLocation.y + 115.0, "template_target")
											}
										}
									}
									"target(4)" -> {
										MessageLog.printToLog("[COMBAT] Targeting Character 4 for Skill.", tag)
										if (!game.imageUtils.isTablet) {
											if (game.imageUtils.is720p) {
												game.gestureUtils.tap(selectCharacterLocation.x - 140.0, selectCharacterLocation.y + 375.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x - 195.0, selectCharacterLocation.y + 570.0, "template_target")
											}
										} else {
											if (!game.imageUtils.isTabletLandscape) {
												game.gestureUtils.tap(selectCharacterLocation.x - 150.0, selectCharacterLocation.y + 415.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x - 115.0, selectCharacterLocation.y + 315.0, "template_target")
											}
										}
									}
									"target(5)" -> {
										MessageLog.printToLog("[COMBAT] Targeting Character 5 for Skill.", tag)
										if (!game.imageUtils.isTablet) {
											if (game.imageUtils.is720p) {
												game.gestureUtils.tap(selectCharacterLocation.x + 5.0, selectCharacterLocation.y + 375.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x + 5.0, selectCharacterLocation.y + 570.0, "template_target")
											}
										} else {
											if (!game.imageUtils.isTabletLandscape) {
												game.gestureUtils.tap(selectCharacterLocation.x + 5.0, selectCharacterLocation.y + 415.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x + 5.0, selectCharacterLocation.y + 315.0, "template_target")
											}
										}
									}
									"target(6)" -> {
										MessageLog.printToLog("[COMBAT] Targeting Character 6 for Skill.", tag)
										if (!game.imageUtils.isTablet) {
											if (game.imageUtils.is720p) {
												game.gestureUtils.tap(selectCharacterLocation.x + 135.0, selectCharacterLocation.y + 375.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x + 210.0, selectCharacterLocation.y + 570.0, "template_target")
											}
										} else {
											if (!game.imageUtils.isTabletLandscape) {
												game.gestureUtils.tap(selectCharacterLocation.x + 155.0, selectCharacterLocation.y + 415.0, "template_target")
											} else {
												game.gestureUtils.tap(selectCharacterLocation.x + 125.0, selectCharacterLocation.y + 315.0, "template_target")
											}
										}
									}
									else -> {
										if (tempSkillCommandList[0].contains("wait")) {
											waitExecute(tempSkillCommandList)
										} else {
											MessageLog.printToLog("[WARNING] Invalid command received for Skill targeting.", tag)
											game.findAndClickButton("cancel")
										}
									}
								}

								tempSkillCommandList = tempSkillCommandList.drop(1)
							}
							game.imageUtils.confirmLocation("skill_unusable", bypassGeneralAdjustment = true) -> {
								MessageLog.printToLog("[COMBAT] Character is currently skill-sealed. Unable to execute command.", tag)
								game.findAndClickButton("cancel")
							}
							tempSkillCommandList[0].contains("wait") -> {
								val waitCommand = tempSkillCommandList[0].substringAfter("(").replace(")", "")

								try {
									val waitSeconds = waitCommand.toDouble()
									MessageLog.printToLog("[COMBAT] Now waiting $waitSeconds seconds.", tag)
									game.wait(waitSeconds)
								} catch (e: Exception) {
									MessageLog.printToLog("[COMBAT] Could not parse out the seconds in the wait command. Waiting 1 second as fallback.", tag)
									game.wait(1.0)
								}
							}
						}
					}
				}
			}
		}

		// Once all commands for the selected Character have been processed, tap the "Back" button to return.
		game.findAndClickButton("back")

		return false
	}

	/**
	 * Activate the specified Summon.
	 *
	 * @param summonCommand The command to be executed.
	 * @return Return True if the Turn will end due to a chained "attack" command. False otherwise.
	 */
	private fun useSummon(summonCommand: String): Boolean {
		for (j in 1..6) {
			if (summonCommand.contains("summon($j)")) {
				// Bring up the available Summons.
				MessageLog.printToLog("[COMBAT] Invoking Summon $j.", tag)
				game.findAndClickButton("summon")

				game.wait(1.0)

				// Now tap on the specified Summon.
				var tries = 3
				while (tries > 0) {
					when (j) {
						1 -> {
							if (!game.imageUtils.isTablet) {
								if (game.imageUtils.is720p) {
									game.gestureUtils.tap(attackButtonLocation!!.x - 485.0, attackButtonLocation!!.y + 210.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x - 715.0, attackButtonLocation!!.y + 300.0, "summon")
								}
							} else {
								if (!game.imageUtils.isTabletLandscape) {
									game.gestureUtils.tap(attackButtonLocation!!.x - 528.0, attackButtonLocation!!.y + 220.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x - 420.0, attackButtonLocation!!.y + 170.0, "summon")
								}
							}
						}
						2 -> {
							if (!game.imageUtils.isTablet) {
								if (game.imageUtils.is720p) {
									game.gestureUtils.tap(attackButtonLocation!!.x - 370.0, attackButtonLocation!!.y + 210.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x - 545.0, attackButtonLocation!!.y + 300.0, "summon")
								}
							} else {
								if (!game.imageUtils.isTabletLandscape) {
									game.gestureUtils.tap(attackButtonLocation!!.x - 407.0, attackButtonLocation!!.y + 220.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x - 315.0, attackButtonLocation!!.y + 170.0, "summon")
								}
							}
						}
						3 -> {
							if (!game.imageUtils.isTablet) {
								if (game.imageUtils.is720p) {
									game.gestureUtils.tap(attackButtonLocation!!.x - 255.0, attackButtonLocation!!.y + 210.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x - 375.0, attackButtonLocation!!.y + 300.0, "summon")
								}
							} else {
								if (!game.imageUtils.isTabletLandscape) {
									game.gestureUtils.tap(attackButtonLocation!!.x - 274.0, attackButtonLocation!!.y + 220.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x - 215.0, attackButtonLocation!!.y + 170.0, "summon")
								}
							}
						}
						4 -> {
							if (!game.imageUtils.isTablet) {
								if (game.imageUtils.is720p) {
									game.gestureUtils.tap(attackButtonLocation!!.x - 140.0, attackButtonLocation!!.y + 210.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x - 205.0, attackButtonLocation!!.y + 300.0, "summon")
								}
							} else {
								if (!game.imageUtils.isTabletLandscape) {
									game.gestureUtils.tap(attackButtonLocation!!.x - 144.0, attackButtonLocation!!.y + 220.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x - 110.0, attackButtonLocation!!.y + 170.0, "summon")
								}
							}
						}
						5 -> {
							if (!game.imageUtils.isTablet) {
								if (game.imageUtils.is720p) {
									game.gestureUtils.tap(attackButtonLocation!!.x - 25.0, attackButtonLocation!!.y + 210.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x - 35.0, attackButtonLocation!!.y + 300.0, "summon")
								}
							} else {
								if (!game.imageUtils.isTabletLandscape) {
									game.gestureUtils.tap(attackButtonLocation!!.x - 20.0, attackButtonLocation!!.y + 220.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x - 15.0, attackButtonLocation!!.y + 170.0, "summon")
								}
							}
						}
						6 -> {
							if (!game.imageUtils.isTablet) {
								if (game.imageUtils.is720p) {
									game.gestureUtils.tap(attackButtonLocation!!.x + 90.0, attackButtonLocation!!.y + 210.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x + 135.0, attackButtonLocation!!.y + 300.0, "summon")
								}
							} else {
								if (!game.imageUtils.isTabletLandscape) {
									game.gestureUtils.tap(attackButtonLocation!!.x + 105.0, attackButtonLocation!!.y + 220.0, "summon")
								} else {
									game.gestureUtils.tap(attackButtonLocation!!.x + 85.0, attackButtonLocation!!.y + 170.0, "summon")
								}
							}
						}
					}

					game.wait(1.0)

					if (game.imageUtils.confirmLocation("summon_details", bypassGeneralAdjustment = true)) {
						val okButtonLocation = game.imageUtils.findButton("ok")

						if (okButtonLocation != null) {
							game.gestureUtils.tap(okButtonLocation.x, okButtonLocation.y, "ok")

							// Now wait for the Summon animation to complete.
							game.wait(7.0)
						} else {
							MessageLog.printToLog("[COMBAT] Summon $j cannot be invoked due to current restrictions.", tag)
							game.findAndClickButton("cancel")

							// Tap the "Back" button to return.
							game.findAndClickButton("back")
						}

						break
					} else {
						// Try to tap on the Summon again if a popup from the Raid absorbed the tap event.
						tries -= 1
					}
				}

				if (summonCommand.contains("wait")) {
					val splitCommand = summonCommand.split(".").drop(1)
					waitExecute(splitCommand, fallbackDelay = 5.0)
				}
			}
		}

		return if (summonCommand.contains("attack")) {
			end()
			true
		} else {
			false
		}
	}

	/**
	 * Activate a Quick Summon.
	 *
	 * @param command The command to be executed. Defaults to the regular quick summon command.
	 * @return Return True if the Turn will end due to a chained "attack" command. False otherwise.
	 */
	private fun quickSummon(command: String = ""): Boolean {
		MessageLog.printToLog("[COMBAT] Quick Summoning now...", tag)
		if (game.imageUtils.findButton("quick_summon_not_ready") == null &&
			(game.findAndClickButton("quick_summon1", bypassGeneralAdjustment = true) || game.findAndClickButton("quick_summon2", bypassGeneralAdjustment = true))
		) {
			MessageLog.printToLog("[COMBAT] Successfully quick summoned!", tag)

			if (command.contains("wait")) {
				val splitCommand = command.split(".").drop(1)
				waitExecute(splitCommand, fallbackDelay = 5.0)
			}

			if (command.contains("attack")) {
				end()
				return true
			}
		} else {
			MessageLog.printToLog("[COMBAT] Was not able to quick summon this Turn.", tag)
		}

		return false
	}

	/**
	 * Enable Semi Auto and if it fails, try to enable Full Auto.
	 *
	 */
	private fun enableSemiAuto() {
		MessageLog.printToLog("[COMBAT] Bot will now attempt to enable Semi Auto...", tag)

		semiAuto = game.imageUtils.findButton("semi_auto_enabled") !== null
		if (!semiAuto) {
			// Have the Party attack and then attempt to see if the "Semi Auto" button becomes visible.
			game.findAndClickButton("attack")
			semiAuto = game.findAndClickButton("semi_auto")

			// If the bot still cannot find the "Semi Auto" button, that probably means that the user has the "Full Auto" button on the screen instead.
			if (!semiAuto) {
				MessageLog.printToLog("[COMBAT] Failed to enable Semi Auto. Falling back to Full Auto...", tag)

				// Enable Full Auto.
				fullAuto = game.findAndClickButton("full_auto")
			} else {
				MessageLog.printToLog("[COMBAT] Semi Auto is now enabled.", tag)
			}
		}
	}

	/**
	 * Enable Full Auto and if it fails, try to enable Semi Auto.
	 *
	 */
	private fun enableFullAuto() {
		MessageLog.printToLog("[COMBAT] Bot will now attempt to enable Full Auto...", tag)
		fullAuto = game.findAndClickButton("full_auto")

		// If the bot failed to find and click the "Full Auto" button, fallback to the "Semi Auto" button.
		if (!fullAuto) {
			MessageLog.printToLog("[COMBAT] Bot failed to find the \"Full Auto\" button. Falling back to Semi Auto...", tag)
			enableSemiAuto()
		} else {
			MessageLog.printToLog("[COMBAT] Full Auto is now enabled.", tag)
		}
	}

	/**
	 * Attacks and then presses the Back button to quickly end animations.
	 *
	 */
	private fun attackBack() {
		if (game.findAndClickButton("attack")) {
			if (game.imageUtils.waitVanish("cancel", timeout = 10)) {
				MessageLog.printToLog("[COMBAT] Attacked and pressing the Back button now...", tag)
				back(incrementTurn = false)
			}

			// Advance the Turn number by 1.
			turnNumber += 1
		} else {
			MessageLog.printToLog("[COMBAT] Failed to execute the \"attackback\" command...", tag)
		}

	}

	/**
	 * Attacks and if there is a wait command attached, execute that as well.
	 *
	 * @param command The command to be executed.
	 */
	private fun attack(command: String) {
		if (game.findAndClickButton("attack", tries = 30)) {
			MessageLog.printToLog("[COMBAT] Successfully executed a manual attack.", tag)
		} else {
			MessageLog.printToLog("[COMBAT] Successfully executed a manual attack that resolved instantly.", tag)
		}

		if (command.contains("wait")) {
			val splitCommand = command.split(".").drop(1)
			waitExecute(splitCommand)
		}
	}

	/**
	 * Presses the Back button. Increments the Turn number if specified otherwise.
	 *
	 * @param incrementTurn Increments the Turn number. Defaults to True.
	 */
	private fun back(incrementTurn: Boolean = true) {
		if (game.findAndClickButton("home_back")) {
			MessageLog.printToLog("[COMBAT] Tapped the Back button.", tag)
			waitForAttack()

			if (incrementTurn) {
				// Advance the Turn number by 1.
				turnNumber += 1
			}
		} else {
			MessageLog.printToLog("[WARNING] Failed to find and tap the Back button.", tag)
		}
	}

	/**
	 * Reloads the page.
	 *
	 */
	private fun reload() {
		MessageLog.printToLog("[COMBAT] Bot will now attempt to manually reload...", tag)

		// Press the "Attack" button in order to show the "Cancel" button. Once that disappears, manually reload the page.
		if (game.findAndClickButton("attack")) {
			if (game.imageUtils.waitVanish("combat_cancel", timeout = 10)) {
				game.findAndClickButton("reload")
				game.wait(3.0)
			} else {
				// If the "Cancel" button fails to disappear after 10 tries, reload anyway.
				game.findAndClickButton("reload")
				game.wait(3.0)
			}
		}
	}

	/**
	 * Ends the Turn.
	 *
	 */
	private fun end() {
		endTurn()

		reloadAfterAttack()
		waitForAttack()

		MessageLog.printToLog("[COMBAT] Turn $turnNumber has ended.", tag)

		turnNumber += 1
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	// Looping Workflows for the End
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////

	/**
	 * Main workflow loop for both Semi Auto and Full Auto. The bot will progress the Quest/Raid until it ends or the Party wipes.
	 *
	 */
	private fun loopAuto() {
		var sleepPreventionTimer = 0
		while (!retreatCheckFlag && (fullAuto || semiAuto)) {
			// Check for exit conditions.
			checkForBattleEnd()

			if (game.findAndClickButton("next", tries = 1, suppressError = true)) {
				game.wait(3.0)
			}

			checkForWipe()

			if (checkRaid()) {
				// Click Next if it is available and enable automation again if combat continues.
				if (game.findAndClickButton("next", tries = 1, suppressError = true)) {
					game.wait(3.0)

					// Check for exit conditions and restart auto.
					if (checkForBattleEnd() == "Nothing") {
						enableAuto()
					}
				} else if (game.imageUtils.findButton("attack", tries = 1, suppressError = true) == null && game.imageUtils.findButton("next", tries = 1, suppressError = true) == null &&
					checkForBattleEnd() == "Nothing"
				) {
					game.wait(1.0)

					reloadAfterAttack(override = true)
					waitForAttack()

					// Check for exit conditions and restart auto.
					if (checkForBattleEnd() == "Nothing") {
						if (debugMode) {
							MessageLog.printToLog("[DEBUG] Clicked the Next button to move to the next wave. Attempting to restart Full/Semi Auto...", tag)
						}

						enableAuto()
					}
				}
			} else if (game.imageUtils.findButton("attack", tries = 1, suppressError = true) == null && game.imageUtils.findButton("next", tries = 1, suppressError = true) == null) {
				if (debugMode) {
					MessageLog.printToLog("[DEBUG] Attack and Next buttons have vanished. Determining if bot should reload...", tag)
				}

				if (reloadAfterAttack()) {
					// Enable Full/Semi Auto again if the bot reloaded.
					if (fullAuto) {
						enableFullAuto()
					} else if (semiAuto) {
						enableSemiAuto()
					}
				}
			}

			game.wait(1.0)

			sleepPreventionTimer += 1

			// The Android device would lock itself and go to sleep if there has been no inputs. Thus, some occasional swiping is required.
			if (sleepPreventionTimer != 0 && sleepPreventionTimer % 60 == 0) {
				MessageLog.printToLog("\n[COMBAT] Swiping screen to prevent Android device going to sleep due to inactivity.", tag)
				game.gestureUtils.swipe(500f, 1000f, 500f, 900f, 100L)
				game.gestureUtils.swipe(500f, 900f, 500f, 1000f, 100L)
			}
		}
	}

	/**
	 * Main workflow loop for manually pressing the Attack button and reloading until combat ends.
	 *
	 */
	private fun loopManual() {
		while (!retreatCheckFlag) {
			// Check for exit conditions.
			checkForBattleEnd()

			if (game.findAndClickButton("next", tries = 1, suppressError = true)) {
				game.wait(3.0)

				// Check for exit conditions.
				checkForBattleEnd()
			}

			game.findAndClickButton("attack", tries = 10)
			reloadAfterAttack()
			waitForAttack()
		}
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	// Entry Point
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////

	/**
	 * Start Combat Mode with the provided combat script.
	 *
	 * @param optionalCombatScript ArrayList of a optional combat script to override the one in the settings.
	 * @return True if Combat Mode ended successfully. False otherwise if the Party wiped or backed out without retreating.
	 */
	fun startCombatMode(optionalCombatScript: List<String>? = null): Boolean {
		val commandList = optionalCombatScript?.toMutableList() ?: game.configData.combatScript.toMutableList()

		startTime = System.currentTimeMillis()

		// Reset the Retreat, Semi Auto, and Full Auto flags.
		retreatCheckFlag = false
		semiAuto = false
		fullAuto = false
		var manualAttackAndReload = false
		var skipEnd = false
		commandTurnNumber = 1
		turnNumber = 1

		MessageLog.printToLog("\n####################", tag)
		MessageLog.printToLog("####################", tag)
		MessageLog.printToLog("[COMBAT] Starting Combat Mode.", tag)
		MessageLog.printToLog("####################", tag)
		MessageLog.printToLog("####################", tag)

		MessageLog.printToLog("[COMBAT] Size of script commands: ${commandList.size}", tag)

		// If current Farming Mode is Arcarum, attempt to dismiss potential stage effect popup like "Can't use Charge Attacks".
		if (game.configData.farmingMode == "Arcarum") {
			game.findAndClickButton("arcarum_stage_effect_active", tries = 10, bypassGeneralAdjustment = true)
		}

		// Save the position of the Attack button.
		attackButtonLocation = game.imageUtils.findButton("attack", tries = 50, bypassGeneralAdjustment = true)

		if (attackButtonLocation == null) {
			MessageLog.printToLog("\n[ERROR] Cannot find Attack button. Raid must have just ended.", tag, isError = true)
			return false
		}

		////////////////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////////////////
		// This is where the main workflow of Combat Mode is located.
		try {
			while (commandList.isNotEmpty() && !retreatCheckFlag) {
				var command = commandList.removeAt(0).lowercase()
				if (command.startsWith("//") || command.startsWith("#")) {
					// Ignore comments in script.
					continue
				} else if (command.contains("/") || command.contains("#")) {
					// Remove comments in the same line.
					command = command.substringBefore("/").substringBefore("#").trim()
				}

				MessageLog.printToLog("\n[COMBAT] Reading command: \"$command\"", tag)

				if (command.contains("turn")) {
					startTurn(command)
				} else if (turnNumber == commandTurnNumber) {
					// Proceed to process each command inside this Turn block until the "end" command is reached.

					// Check if the Battle has ended.
					checkForBattleEnd()

					// Determine which Character to take action.
					val characterSelected = when {
						command.contains("character1") -> {
							1
						}
						command.contains("character2") -> {
							2
						}
						command.contains("character3") -> {
							3
						}
						command.contains("character4") -> {
							4
						}
						else -> {
							0
						}
					}

					when {
						characterSelected != 0 -> {
							// Select the specified Character.
							selectCharacter(characterSelected)

							// Now execute each Skill command starting from left to right for this Character.
							val skillCommandList: List<String> = command.split(".").drop(1)
							if (useCharacterSkill(characterSelected, skillCommandList)) {
								skipEnd = true
							}
						}
						command == "requestbackup" -> {
							requestBackup()
						}
						command == "tweetbackup" -> {
							tweetBackup()
						}
						healingItemCommands.contains(command) -> {
							useCombatHealingItem(command)
						}
						command.contains("summon") && !command.contains("quicksummon") -> {
							if (useSummon(command)) {
								skipEnd = true
							}
						}
						command.contains("quicksummon") -> {
							if (quickSummon(command)) {
								skipEnd = true
							}
						}
						command == "enablesemiauto" -> {
							enableSemiAuto()
						}
						command == "enablefullauto" -> {
							enableFullAuto()
						}
						command.contains("targetenemy") -> {
							// Select enemy target.
							selectEnemyTarget(command)
						}
						command.contains("attackback") -> {
							attackBack()
						}
						command.contains("attack") -> {
							attack(command)
						}
						command.contains("back") -> {
							back()
						}
						command.contains("reload") -> {
							reload()
						}
						command.contains("repeatmanualattackandreload") -> {
							MessageLog.printToLog("[COMBAT] Enabling manually pressing the Attack button and reloading (if the mission supports it) until battle ends.", tag)
							manualAttackAndReload = true
						}
						!semiAuto && !fullAuto && command == "end" && !skipEnd -> {
							end()
						}
						command.indexOf("wait") == 0 -> {
							waitExecute(listOf(command))
						}
						command == "exit" -> {
							// End Combat Mode by heading back to the Home screen without retreating.
							MessageLog.printToLog("\n[COMBAT] Leaving this Raid without retreating.", tag)
							MessageLog.printToLog("\n####################", tag)
							MessageLog.printToLog("####################", tag)
							MessageLog.printToLog("[COMBAT] Ending Combat Mode.", tag)
							MessageLog.printToLog("####################", tag)
							MessageLog.printToLog("####################", tag)
							game.goBackHome(confirmLocationCheck = true)
							return false
						}
					}
				}

				////////////////////////////////////////////////////////////////////////////////
				////////////////////////////////////////////////////////////////////////////////
				// Handle certain commands that could be present outside of a Turn block.
				if (!semiAuto && !fullAuto && command == "enablesemiauto") {
					enableSemiAuto()
				} else if (!semiAuto && !fullAuto && command == "enablefullauto") {
					fullAuto
				} else if (command.contains("repeatmanualattackandreload")) {
					MessageLog.printToLog("[Combat] Enabling manually pressing the Attack button and reloading (if the mission supports it) until battle ends.", tag)
					manualAttackAndReload = true
				} else if (command.indexOf("wait") == 0) {
					waitExecute(listOf(command))
				}
			}

			// Deal with any the situation where high-profile raids end right when the bot loads in and all it sees is the "Next" button.
			if (game.configData.farmingMode == "Raid" && game.findAndClickButton("next", tries = 3)) {
				MessageLog.printToLog("\n####################", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("[COMBAT] Ending Combat Mode.", tag)
				MessageLog.printToLog("####################", tag)
				MessageLog.printToLog("####################", tag)
				return true
			}

			////////////////////////////////////////////////////////////////////////////////
			////////////////////////////////////////////////////////////////////////////////
			// When the bot arrives here, all the commands in the combat script has been processed.
			MessageLog.printToLog("[COMBAT] Bot has processed the entire combat script. Automatically attacking until the battle ends or Party wipes.", tag)

			if (!manualAttackAndReload) {
				// Attempt to activate Full Auto at the end of the combat script. If not, then attempt to activate Semi Auto.
				if (!semiAuto && !fullAuto) {
					enableFullAuto()
				}

				// Counteract slower instances when the battle finished right when the bot finished executing the script.
				if (game.findAndClickButton("next", tries = 1, suppressError = true)) {
					game.wait(3.0)
					checkForBattleEnd()
				}

				loopAuto()
			} else {
				// Main workflow loop for manually pressing the Attack button and reloading until combat ends.
				loopManual()
			}
		} catch (e: CombatModeException) {
			if (listOfExitEventsForFalse.contains(e.message)) {
				return false
			} else if (listOfExitEventsForTrue.contains(e.message)) {
				// Calculate elapsed time for the API.
				if (game.configData.enableOptInAPI) {
					game.configData.combatElapsedTime = System.currentTimeMillis() - startTime
				}

				return true
			}
		}

		////////////////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////////////////

		MessageLog.printToLog("\n####################", tag)
		MessageLog.printToLog("####################", tag)
		MessageLog.printToLog("[COMBAT] Ending Combat Mode.", tag)
		MessageLog.printToLog("####################", tag)
		MessageLog.printToLog("####################", tag)

		// Calculate elapsed time for the API.
		if (game.configData.enableOptInAPI) {
			game.configData.combatElapsedTime = System.currentTimeMillis() - startTime
		}

		return if (!retreatCheckFlag) {
			MessageLog.printToLog("\n[INFO] Bot has reached the Quest Results screen.", tag)
			true
		} else {
			false
		}
	}
}