package com.steve1316.granblue_automation_android.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.steve1316.granblue_automation_android.MainActivity

class ConfigData(myContext: Context) {
	private val tag = "${MainActivity.loggerTag}ConfigData"

	// Game
	val farmingMode: String
	val mapName: String
	val missionName: String
	val itemName: String
	val itemAmount: Int
	val combatScriptName: String
	val combatScript: List<String>
	val summonList: List<String>
	val groupNumber: Int
	val partyNumber: Int
	val debugMode: Boolean

	// Twitter
	val twitterAPIKey: String
	val twitterAPIKeySecret: String
	val twitterAccessToken: String
	val twitterAccessTokenSecret: String

	// Discord
	val enableDiscordNotifications: Boolean
	val discordToken: String
	val discordUserID: String

	// Configuration
	val enableAutoRestore: Boolean
	val enableDelayBetweenRuns: Boolean
	val delayBetweenRuns: Int
	val enableRandomizedDelayBetweenRuns: Boolean
	val delayBetweenRunsLowerBound: Int
	val delayBetweenRunsUpperBound: Int

	// Nightmare
	val enableNightmare: Boolean
	val nightmareCombatScriptName: String
	val nightmareCombatScript: List<String>
	val nightmareSummons: List<String>
	val nightmareGroupNumber: Int
	val nightmarePartyNumber: Int

	// Event
	val enableLocationIncrementByOne: Boolean

	// Raid
	val enableAutoExitRaid: Boolean
	val timeAllowedUntilAutoExitRaid: Long
	val enableNoTimeout: Boolean

	// Arcarum
	val enableStopOnArcarumBoss: Boolean

	// Android
	val enableDelayTap: Boolean
	val delayTapMilliseconds: Int
	val confidence: Double
	val confidenceAll: Double
	val customScale: Double
	val enableTestForHomeScreen: Boolean

	init {
		Log.d(tag, "Loading settings from SharedPreferences to memory...")

		val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext)

		farmingMode = sharedPreferences.getString("farmingMode", "")!!
		mapName = sharedPreferences.getString("map", "")!!
		missionName = sharedPreferences.getString("mission", "")!!
		itemName = sharedPreferences.getString("item", "")!!
		itemAmount = sharedPreferences.getInt("itemAmount", 1)
		combatScriptName = sharedPreferences.getString("combatScriptName", "")!!
		combatScript = sharedPreferences.getString("combatScript", "")!!.split("|")
		summonList = sharedPreferences.getString("summons", "")!!.split("|")
		groupNumber = sharedPreferences.getInt("groupNumber", 1)
		partyNumber = sharedPreferences.getInt("partyNumber", 1)
		debugMode = sharedPreferences.getBoolean("debugMode", false)

		twitterAPIKey = sharedPreferences.getString("twitterAPIKey", "")!!
		twitterAPIKeySecret = sharedPreferences.getString("twitterAPIKeySecret", "")!!
		twitterAccessToken = sharedPreferences.getString("twitterAccessToken", "")!!
		twitterAccessTokenSecret = sharedPreferences.getString("twitterAccessTokenSecret", "")!!

		enableDiscordNotifications = sharedPreferences.getBoolean("enableDiscordNotifications", false)
		discordToken = sharedPreferences.getString("discordToken", "")!!
		discordUserID = sharedPreferences.getString("discordUserID", "")!!

		enableAutoRestore = sharedPreferences.getBoolean("enableAutoRestore", false)
		enableDelayBetweenRuns = sharedPreferences.getBoolean("enableDelayBetweenRuns", false)
		delayBetweenRuns = sharedPreferences.getInt("delayBetweenRuns", 5)
		enableRandomizedDelayBetweenRuns = sharedPreferences.getBoolean("enableRandomizedDelayBetweenRuns", false)
		delayBetweenRunsLowerBound = sharedPreferences.getInt("delayBetweenRunsLowerBound", 5)
		delayBetweenRunsUpperBound = sharedPreferences.getInt("delayBetweenRunsUpperBound", 15)

		enableNightmare = sharedPreferences.getBoolean("enableNightmare", false)
		if (sharedPreferences.getBoolean("enableCustomNightmareSettings", false)) {
			Log.d(tag, "[NIGHTMARE] Settings initializing...")

			nightmareCombatScriptName = sharedPreferences.getString("nightmareCombatScriptName", "")!!

			nightmareCombatScript = if(sharedPreferences.getString("nightmareCombatScript", "")!!.split("|").isNotEmpty()) {
				sharedPreferences.getString("nightmareCombatScript", "")!!.split("|")
			} else {
				combatScript
			}

			nightmareSummons = if(sharedPreferences.getString("nightmareSummons", "")!!.split("|").isNotEmpty()) {
				sharedPreferences.getString("nightmareSummons", "")!!.split("|")
			} else {
				summonList
			}

			nightmareGroupNumber = sharedPreferences.getInt("nightmareGroupNumber", 1)
			nightmarePartyNumber = sharedPreferences.getInt("nightmarePartyNumber", 1)

			Log.d(tag, "[NIGHTMARE] Settings initialized.")
		} else {
			Log.d(tag, "[NIGHTMARE] Reusing settings from Farming Mode for $farmingMode Nightmare.")
			nightmareCombatScriptName = sharedPreferences.getString("nightmareCombatScriptName", "")!!
			nightmareCombatScript = sharedPreferences.getString("nightmareCombatScript", "")!!.split("|")
			nightmareSummons = sharedPreferences.getString("nightmareSummons", "")!!.split("|")
			nightmareGroupNumber = sharedPreferences.getInt("nightmareGroupNumber", 1)
			nightmarePartyNumber = sharedPreferences.getInt("nightmarePartyNumber", 1)
		}

		enableLocationIncrementByOne = sharedPreferences.getBoolean("enableLocationIncrementByOne", false)

		enableAutoExitRaid = sharedPreferences.getBoolean("enableAutoExitRaid", false)
		timeAllowedUntilAutoExitRaid = sharedPreferences.getInt("timeAllowedUntilAutoExitRaid", 1).toLong() * 60L * 1000L
		enableNoTimeout = sharedPreferences.getBoolean("enableNoTimeout", false)

		enableStopOnArcarumBoss = sharedPreferences.getBoolean("enableStopOnArcarumBoss", true)

		enableDelayTap = sharedPreferences.getBoolean("enableDelayTap", false)
		delayTapMilliseconds = sharedPreferences.getInt("delayTapMilliseconds", 1000)
		confidence = sharedPreferences.getFloat("confidence", 0.8f).toDouble() / 100.0
		confidenceAll = sharedPreferences.getFloat("confidenceAll", 0.8f).toDouble() / 100.0
		customScale = sharedPreferences.getFloat("customScale", 1.0f).toDouble()
		enableTestForHomeScreen = sharedPreferences.getBoolean("enableTestForHomeScreen", false)

		Log.d(tag, "Successfully loaded settings from SharedPreferences to memory.")
	}
}