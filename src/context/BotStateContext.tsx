import React, { createContext, useState } from "react"

export interface Settings {
    // Game settings.
    game: {
        combatScriptName: string
        combatScript: string[]
        farmingMode: string
        item: string
        mission: string
        map: string
        itemAmount: number
        summons: string[]
        summonElements: string[]
        groupNumber: number
        partyNumber: number
        debugMode: boolean
    }

    // Twitter settings.
    twitter: {
        twitterAPIKey: string
        twitterAPIKeySecret: string
        twitterAccessToken: string
        twitterAccessTokenSecret: string
    }

    // Discord settings.
    discord: {
        enableDiscordNotifications: boolean
        discordToken: string
        discordUserID: string
    }

    // Configuration settings.
    configuration: {
        enableAutoRestore: boolean
        enableFullElixir: boolean
        enableSoulBalm: boolean
        enableDelayBetweenRuns: boolean
        delayBetweenRuns: number
        enableRandomizedDelayBetweenRuns: boolean
        delayBetweenRunsLowerBound: number
        delayBetweenRunsUpperBound: number
    }

    // Extra Settings related to Nightmares from certain Farming Modes.
    nightmare: {
        enableNightmare: boolean
        enableCustomNightmareSettings: boolean
        nightmareCombatScriptName: string
        nightmareCombatScript: string[]
        nightmareSummons: string[]
        nightmareSummonElements: string[]
        nightmareGroupNumber: number
        nightmarePartyNumber: number
    }

    // Settings specific to certain Farming Modes.
    event: {
        enableLocationIncrementByOne: boolean
    }

    raid: {
        enableAutoExitRaid: boolean
        timeAllowedUntilAutoExitRaid: number
        enableNoTimeout: boolean
    }

    arcarum: {
        enableStopOnArcarumBoss: boolean
    }

    android: {
        enableDelayTap: boolean
        setDelayTapMilliseconds: number
        confidence: number
        confidenceAll: number
        customScale: number
        enableTestForHomeScreen: boolean
    }
}

// Set the default settings.
export const defaultSettings: Settings = {
    game: {
        combatScriptName: "",
        combatScript: [],
        farmingMode: "",
        item: "",
        mission: "",
        map: "",
        itemAmount: 1,
        summons: [],
        summonElements: [],
        groupNumber: 1,
        partyNumber: 1,
        debugMode: false,
    },
    twitter: {
        twitterAPIKey: "",
        twitterAPIKeySecret: "",
        twitterAccessToken: "",
        twitterAccessTokenSecret: "",
    },
    discord: {
        enableDiscordNotifications: false,
        discordToken: "",
        discordUserID: "",
    },
    configuration: {
        enableAutoRestore: true,
        enableFullElixir: false,
        enableSoulBalm: false,
        enableDelayBetweenRuns: false,
        delayBetweenRuns: 15,
        enableRandomizedDelayBetweenRuns: false,
        delayBetweenRunsLowerBound: 15,
        delayBetweenRunsUpperBound: 60,
    },
    nightmare: {
        enableNightmare: false,
        enableCustomNightmareSettings: false,
        nightmareCombatScriptName: "",
        nightmareCombatScript: [],
        nightmareSummons: [],
        nightmareSummonElements: [],
        nightmareGroupNumber: 1,
        nightmarePartyNumber: 1,
    },
    event: {
        enableLocationIncrementByOne: false,
    },
    raid: {
        enableAutoExitRaid: false,
        timeAllowedUntilAutoExitRaid: 10,
        enableNoTimeout: false,
    },
    arcarum: {
        enableStopOnArcarumBoss: true,
    },
    android: {
        enableDelayTap: false,
        setDelayTapMilliseconds: 1000,
        confidence: 0.8,
        confidenceAll: 0.8,
        customScale: 1.0,
        enableTestForHomeScreen: false,
    },
}

interface IProviderProps {
    readyStatus: boolean
    setReadyStatus: (readyStatus: boolean) => void
    isBotRunning: boolean
    setIsBotRunning: (isBotRunning: boolean) => void
    startBot: boolean
    setStartBot: (startBot: boolean) => void
    stopBot: boolean
    setStopBot: (stopBot: boolean) => void
    refreshAlert: boolean
    setRefreshAlert: (refreshAlert: boolean) => void
    settings: Settings
    setSettings: (settings: Settings) => void
}

export const BotStateContext = createContext<IProviderProps>({} as IProviderProps)

// https://stackoverflow.com/a/60130448 and https://stackoverflow.com/a/60198351
export const BotStateProvider = ({ children }: any): JSX.Element => {
    const [readyStatus, setReadyStatus] = useState<boolean>(false)
    const [isBotRunning, setIsBotRunning] = useState<boolean>(false)
    const [startBot, setStartBot] = useState<boolean>(false)
    const [stopBot, setStopBot] = useState<boolean>(false)
    const [refreshAlert, setRefreshAlert] = useState<boolean>(false)

    const [settings, setSettings] = useState<Settings>(defaultSettings)

    const providerValues: IProviderProps = {
        readyStatus,
        setReadyStatus,
        isBotRunning,
        setIsBotRunning,
        startBot,
        setStartBot,
        stopBot,
        setStopBot,
        refreshAlert,
        setRefreshAlert,
        settings,
        setSettings,
    }

    return <BotStateContext.Provider value={providerValues}>{children}</BotStateContext.Provider>
}
