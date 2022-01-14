package com.steve1316.granblue_automation_android.utils

import android.util.Log
import com.steve1316.granblue_automation_android.MainActivity.loggerTag
import com.steve1316.granblue_automation_android.bot.Game
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import kotlinx.coroutines.coroutineScope
import java.util.*

/**
 * This class takes care of notifying users of status updates via Discord private DMs.
 */
class DiscordUtils(val game: Game) {
	private val tag: String = "${loggerTag}DiscordUtils"

	companion object {
		val queue: Queue<String> = LinkedList()

		lateinit var client: Kord

		suspend fun disconnectClient() {
			if (this::client.isInitialized) {
				client.logout()
			}
		}
	}

	suspend fun main(): Unit = coroutineScope {
		try {
			Log.d(tag, "Starting Discord process now...")

			// Initialize the client with the Bot Account's token.
			client = Kord(game.configData.discordToken)

			// This listener gets fired when the client is connected to the Discord API.
			client.on<ReadyEvent> {
				// Get the user's private DM channel via their Snowflake.
				val snowflake = Snowflake(game.configData.discordUserID)
				val dmChannel = client.getUser(snowflake)?.getDmChannelOrNull()!!

				Log.d(tag, "Successful connection to Discord API.")
				queue.add("```diff\n+ Successful connection to Discord API for Granblue Automation Android\n```")

				// Loop and send any messages inside the Queue.
				while (true) {
					if (queue.isNotEmpty()) {
						val message = queue.remove()
						dmChannel.createMessage(message)

						if (message.contains("Terminated connection to Discord API")) {
							break
						}
					}
				}

				Log.d(tag, "Terminated connection to Discord API.")
				client.logout()
			}

			// Login to the Discord API. This will block this Thread but will allow the onReadyEvent listener to continue running.
			client.login()
		} catch (e: Exception) {
			Log.d(tag, "Failed to initialize Kord client: ${e.stackTraceToString()}")
			disconnectClient()
		}
	}
}