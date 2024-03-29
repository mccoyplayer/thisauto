import axios, { AxiosError } from "axios"
import BouncyCheckbox from "react-native-bouncy-checkbox"
import Checkbox from "../../components/CustomCheckbox"
import CustomButton from "../../components/CustomButton"
import DocumentPicker from "react-native-document-picker"
import LoadingButton from "../../components/LoadingButton"
import NumericInput from "react-native-numeric-input"
import React, { useContext, useEffect, useState } from "react"
import RNFS from "react-native-fs"
import TitleDivider from "../../components/TitleDivider"
import TransferList from "../../components/TransferList"
import { BotStateContext } from "../../context/BotStateContext"
import { DeviceEventEmitter, Dimensions, Modal, ScrollView, StyleSheet, TouchableOpacity, View } from "react-native"
import { Divider, Input, Text } from "react-native-elements"
import { NativeModules } from "react-native"
import { Picker } from "@react-native-picker/picker"
import { RangeSlider, Slider } from "@sharcoux/slider"
import { Snackbar } from "react-native-paper"

const styles = StyleSheet.create({
    root: {
        flex: 1,
        flexDirection: "column",
        justifyContent: "center",
        margin: 10,
    },
    modal: {
        flex: 1,
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        backgroundColor: "rgba(80,80,80,0.3)",
    },
    outsideModal: {
        position: "absolute",
        height: "100%",
        width: "100%",
    },
    componentContainer: {
        width: Dimensions.get("window").width * 0.7,
        height: Dimensions.get("window").height * 0.9,
    },
})

const ExtraSettings = () => {
    const [modalOpen, setModalOpen] = useState<boolean>(false)
    const [showSnackbar, setShowSnackbar] = useState<boolean>(false)
    const [testInProgress, setTestInProgress] = useState<boolean>(false)
    const [testFailed, setTestFailed] = useState<boolean>(false)
    const [testErrorMessage, setTestErrorMessage] = useState<string>("")

    const bsc = useContext(BotStateContext)

    const { StartModule } = NativeModules

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // Rendering

    const renderNightmareSettings = () => {
        if (
            bsc.settings.nightmare.enableNightmare &&
            (bsc.settings.game.farmingMode === "Special" ||
                bsc.settings.game.farmingMode === "Event" ||
                bsc.settings.game.farmingMode === "Event (Token Drawboxes)" ||
                bsc.settings.game.farmingMode === "Xeno Clash" ||
                bsc.settings.game.farmingMode === "Rise of the Beasts")
        ) {
            var title: string = ""
            if (bsc.settings.game.farmingMode === "Special") {
                title = "Dimensional Halo"
            } else if (bsc.settings.game.farmingMode === "Rise of the Beasts") {
                title = "Extreme+"
            } else {
                title = "Nightmare"
            }

            return (
                <View>
                    <TitleDivider title={`${title} Settings`} hasIcon={true} iconName="sword-cross" iconColor="#000" />

                    <Text style={{ marginBottom: 10, fontSize: 12, opacity: 0.7 }}>
                        If "Enable Custom Settings" is not enabled, then the bot will reuse the settings for the Farming Mode. Same thing will happen if it is enabled and a separate combat script and
                        summons are not selected here.
                    </Text>

                    <Checkbox
                        text={`Enable Custom Settings for\n${title}`}
                        subtitle={`Enable customizing individual settings for ${title}`}
                        isChecked={bsc.settings.nightmare.enableCustomNightmareSettings}
                        onPress={() =>
                            bsc.setSettings({ ...bsc.settings, nightmare: { ...bsc.settings.nightmare, enableCustomNightmareSettings: !bsc.settings.nightmare.enableCustomNightmareSettings } })
                        }
                    />

                    {bsc.settings.nightmare.enableCustomNightmareSettings ? (
                        <View style={{ marginTop: -10, marginLeft: 10, marginBottom: 10, marginRight: 10 }}>
                            <CustomButton
                                title={bsc.settings.nightmare.nightmareCombatScriptName === "" ? "Select Nightmare Combat Script" : `Selected: ${bsc.settings.nightmare.nightmareCombatScriptName}`}
                                width={200}
                                borderRadius={20}
                                onPress={async () => {
                                    try {
                                        const pickerResult = await DocumentPicker.pickSingle({
                                            type: "text/plain",
                                        })

                                        const uri = pickerResult.uri
                                        if (uri.startsWith("content://")) {
                                            // Convert content uri to file uri.
                                            // Source: https://stackoverflow.com/a/62677483
                                            const uriComponents = uri.split("/")
                                            const fileNameAndExtension = uriComponents[uriComponents.length - 1]
                                            const destPath = `${RNFS.TemporaryDirectoryPath}/${fileNameAndExtension}`
                                            await RNFS.copyFile(uri, destPath)

                                            // Now read the file using the newly converted file uri.
                                            await RNFS.readFile("file://" + destPath).then((data) => {
                                                console.log("Read combat script: ", data)

                                                const newCombatScript: string[] = data
                                                    .replace(/\r\n/g, "\n") // Replace LF with CRLF.
                                                    .replace(/[\r\n]/g, "\n")
                                                    .replace("\t", "") // Replace tab characters.
                                                    .replace(/\t/g, "")
                                                    .split("\n")

                                                bsc.setSettings({
                                                    ...bsc.settings,
                                                    nightmare: {
                                                        ...bsc.settings.nightmare,
                                                        nightmareCombatScriptName: pickerResult.name ? pickerResult.name : "",
                                                        nightmareCombatScript: newCombatScript,
                                                    },
                                                })
                                            })
                                        }
                                    } catch (e: any) {
                                        if (!e.message.includes("Can't perform a React")) {
                                            console.warn(e)
                                        }
                                        bsc.setSettings({ ...bsc.settings, nightmare: { ...bsc.settings.nightmare, nightmareCombatScriptName: "", nightmareCombatScript: [] } })
                                    }
                                }}
                            />

                            <Text style={{ marginBottom: 10, fontSize: 12, opacity: 0.7, color: "black" }}>
                                To deselect, cancel/back out of the document picker. If no combat script is selected, Full/Semi Auto is used by default.
                            </Text>

                            <Divider />

                            <View>
                                <CustomButton title="Select Nightmare Support Summon(s)" width={"100%"} onPress={() => setModalOpen(true)} />
                                <Modal transparent={true} animationType="fade" statusBarTranslucent={true} visible={modalOpen} onRequestClose={() => setModalOpen(false)}>
                                    <View style={styles.modal}>
                                        <TouchableOpacity style={styles.outsideModal} onPress={() => setModalOpen(false)} />
                                        <View style={styles.componentContainer}>
                                            <TransferList isNightmare={true} />
                                        </View>
                                    </View>
                                </Modal>
                            </View>

                            <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                                <View style={{ width: Dimensions.get("window").width * 0.3 }}>
                                    <Text>Group #:</Text>
                                    <Picker
                                        selectedValue={bsc.settings.nightmare.nightmareGroupNumber}
                                        onValueChange={(value) => bsc.setSettings({ ...bsc.settings, nightmare: { ...bsc.settings.nightmare, nightmareGroupNumber: value } })}
                                        mode="dropdown"
                                        style={{ color: "#000" }}
                                        dropdownIconColor={"#000"}
                                    >
                                        {[...Array(7 - 1 + 1).keys()]
                                            .map((x) => x + 1)
                                            .map((value) => {
                                                return <Picker.Item key={`key-${value}`} label={`${value}`} value={value} />
                                            })}
                                    </Picker>
                                </View>
                                <View style={{ width: Dimensions.get("window").width * 0.3 }}>
                                    <Text>Party #:</Text>
                                    <Picker
                                        selectedValue={bsc.settings.nightmare.nightmarePartyNumber}
                                        onValueChange={(value) => bsc.setSettings({ ...bsc.settings, nightmare: { ...bsc.settings.nightmare, nightmarePartyNumber: value } })}
                                        mode="dropdown"
                                        style={{ color: "#000" }}
                                        dropdownIconColor={"#000"}
                                    >
                                        {[...Array(6 - 1 + 1).keys()]
                                            .map((x) => x + 1)
                                            .map((value) => {
                                                return <Picker.Item key={`key-${value}`} label={`${value}`} value={value} />
                                            })}
                                    </Picker>
                                </View>
                            </View>
                        </View>
                    ) : null}
                </View>
            )
        } else {
            return null
        }
    }

    const renderSandboxDefenderSettings = () => {
        if (bsc.settings.sandbox.enableDefender && bsc.settings.game.farmingMode === "Arcarum Sandbox") {
            return (
                <View>
                    <TitleDivider title="Defender Settings" hasIcon={true} iconName="sword-cross" />

                    <Text style={{ marginBottom: 10, fontSize: 12, opacity: 0.7 }}>If none of these settings are changed, then the bot will reuse the settings for the Farming Mode.</Text>

                    <Checkbox
                        text="Enable Custom Settings for Defender"
                        subtitle="Enable customizing individual settings for Defender"
                        isChecked={bsc.settings.sandbox.enableCustomDefenderSettings}
                        onPress={() => bsc.setSettings({ ...bsc.settings, sandbox: { ...bsc.settings.sandbox, enableCustomDefenderSettings: !bsc.settings.nightmare.enableCustomNightmareSettings } })}
                    />

                    {bsc.settings.sandbox.enableCustomDefenderSettings ? (
                        <View style={{ marginTop: -10, marginLeft: 10, marginBottom: 10, marginRight: 10 }}>
                            <CustomButton
                                title={bsc.settings.nightmare.nightmareCombatScriptName === "" ? "Select Defender Combat Script" : `Selected: ${bsc.settings.nightmare.nightmareCombatScriptName}`}
                                width={200}
                                borderRadius={20}
                                onPress={async () => {
                                    try {
                                        const pickerResult = await DocumentPicker.pickSingle({
                                            type: "text/plain",
                                        })

                                        const uri = pickerResult.uri
                                        if (uri.startsWith("content://")) {
                                            // Convert content uri to file uri.
                                            // Source: https://stackoverflow.com/a/62677483
                                            const uriComponents = uri.split("/")
                                            const fileNameAndExtension = uriComponents[uriComponents.length - 1]
                                            const destPath = `${RNFS.TemporaryDirectoryPath}/${fileNameAndExtension}`
                                            await RNFS.copyFile(uri, destPath)

                                            // Now read the file using the newly converted file uri.
                                            await RNFS.readFile("file://" + destPath).then((data) => {
                                                console.log("Read combat script: ", data)

                                                const newCombatScript: string[] = data
                                                    .replace(/\r\n/g, "\n") // Replace LF with CRLF.
                                                    .replace(/[\r\n]/g, "\n")
                                                    .replace("\t", "") // Replace tab characters.
                                                    .replace(/\t/g, "")
                                                    .split("\n")

                                                bsc.setSettings({
                                                    ...bsc.settings,
                                                    sandbox: { ...bsc.settings.sandbox, defenderCombatScriptName: pickerResult.name ? pickerResult.name : "", defenderCombatScript: newCombatScript },
                                                })
                                            })
                                        }
                                    } catch (e: any) {
                                        if (!e.message.includes("Can't perform a React")) {
                                            console.warn(e)
                                        }
                                        bsc.setSettings({ ...bsc.settings, sandbox: { ...bsc.settings.sandbox, defenderCombatScriptName: "", defenderCombatScript: [] } })
                                    }
                                }}
                            />

                            <Text style={{ marginBottom: 10, fontSize: 12, opacity: 0.7, color: "black" }}>
                                To deselect, cancel/back out of the document picker. If no combat script is selected, Full/Semi Auto is used by default.
                            </Text>

                            <View style={{ width: Dimensions.get("window").width * 0.3 }}>
                                <Text>How many times to run</Text>
                                <Picker
                                    selectedValue={bsc.settings.sandbox.numberOfDefenders}
                                    onValueChange={(value) => bsc.setSettings({ ...bsc.settings, sandbox: { ...bsc.settings.sandbox, numberOfDefenders: value } })}
                                    mode="dropdown"
                                    style={{ color: "#000" }}
                                    dropdownIconColor={"#000"}
                                >
                                    {[...Array(100).keys()]
                                        .map((x) => x + 1)
                                        .map((value) => {
                                            return <Picker.Item key={`key-${value}`} label={`${value}`} value={value} />
                                        })}
                                </Picker>
                            </View>

                            <Divider />

                            <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                                <View style={{ width: Dimensions.get("window").width * 0.3 }}>
                                    <Text>Group #:</Text>
                                    <Picker
                                        selectedValue={bsc.settings.sandbox.defenderGroupNumber}
                                        onValueChange={(value) => bsc.setSettings({ ...bsc.settings, sandbox: { ...bsc.settings.sandbox, defenderGroupNumber: value } })}
                                        mode="dropdown"
                                        style={{ color: "#000" }}
                                        dropdownIconColor={"#000"}
                                    >
                                        {[...Array(7 - 1 + 1).keys()]
                                            .map((x) => x + 1)
                                            .map((value) => {
                                                return <Picker.Item key={`key-${value}`} label={`${value}`} value={value} />
                                            })}
                                    </Picker>
                                </View>
                                <View style={{ width: Dimensions.get("window").width * 0.3 }}>
                                    <Text>Party #:</Text>
                                    <Picker
                                        selectedValue={bsc.settings.sandbox.defenderPartyNumber}
                                        onValueChange={(value) => bsc.setSettings({ ...bsc.settings, sandbox: { ...bsc.settings.sandbox, defenderPartyNumber: value } })}
                                        mode="dropdown"
                                        style={{ color: "#000" }}
                                        dropdownIconColor={"#000"}
                                    >
                                        {[...Array(6 - 1 + 1).keys()]
                                            .map((x) => x + 1)
                                            .map((value) => {
                                                return <Picker.Item key={`key-${value}`} label={`${value}`} value={value} />
                                            })}
                                    </Picker>
                                </View>
                            </View>
                        </View>
                    ) : null}
                </View>
            )
        }
    }

    const renderTwitterSettings = () => {
        return (
            <View>
                <TitleDivider
                    title="Twitter Settings"
                    subtitle="Please visit the wiki on the GitHub page for instructions on how to get these keys and tokens. In addition, API v1.1 is supported but not API v2."
                    hasIcon={true}
                    iconName="twitter"
                    iconColor="#1da1f2"
                />

                <Input
                    label="Twitter API Key"
                    multiline
                    containerStyle={{ marginLeft: -10 }}
                    value={bsc.settings.twitter.twitterAPIKey}
                    onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, twitter: { ...bsc.settings.twitter, twitterAPIKey: value } })}
                />
                <Input
                    label="Twitter API Key Secret"
                    multiline
                    containerStyle={{ marginLeft: -10 }}
                    value={bsc.settings.twitter.twitterAPIKeySecret}
                    onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, twitter: { ...bsc.settings.twitter, twitterAPIKeySecret: value } })}
                />
                <Input
                    label="Twitter Access Token"
                    multiline
                    containerStyle={{ marginLeft: -10 }}
                    value={bsc.settings.twitter.twitterAccessToken}
                    onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, twitter: { ...bsc.settings.twitter, twitterAccessToken: value } })}
                />
                <Input
                    label="Twitter Access Token Secret"
                    multiline
                    containerStyle={{ marginLeft: -10 }}
                    value={bsc.settings.twitter.twitterAccessTokenSecret}
                    onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, twitter: { ...bsc.settings.twitter, twitterAccessTokenSecret: value } })}
                />

                <LoadingButton title="Test Twitter API v1.1" loadingTitle="In progress..." isLoading={testInProgress} onPress={() => testTwitter()} />
            </View>
        )
    }

    const renderDiscordSettings = () => {
        return (
            <View>
                <TitleDivider
                    title="Discord Settings"
                    subtitle={`Please visit the wiki on the GitHub page for instructions on how to get the token and user ID.`}
                    hasIcon={true}
                    iconName="discord"
                    iconColor="#7289d9"
                />
                <Checkbox
                    text="Enable Discord Notifications"
                    subtitle="Enable notifications of loot drops and errors encountered by the bot via Discord DMs."
                    isChecked={bsc.settings.discord.enableDiscordNotifications}
                    onPress={() => bsc.setSettings({ ...bsc.settings, discord: { ...bsc.settings.discord, enableDiscordNotifications: !bsc.settings.discord.enableDiscordNotifications } })}
                />
                {bsc.settings.discord.enableDiscordNotifications ? (
                    <View>
                        <Input
                            label="Discord Token"
                            multiline
                            containerStyle={{ marginLeft: -10 }}
                            value={bsc.settings.discord.discordToken}
                            onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, discord: { ...bsc.settings.discord, discordToken: value } })}
                        />
                        <Input
                            label="Discord User ID"
                            multiline
                            containerStyle={{ marginLeft: -10 }}
                            value={bsc.settings.discord.discordUserID}
                            onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, discord: { ...bsc.settings.discord, discordUserID: value } })}
                        />
                        <LoadingButton title="Test Discord API" loadingTitle="In progress..." isLoading={testInProgress} onPress={() => testDiscord()} />
                    </View>
                ) : null}
            </View>
        )
    }

    const renderAPIIntegrationSettings = () => {
        return (
            <View>
                <TitleDivider
                    title="API Integration"
                    subtitle="You can opt-in to this feature where the bot will automatically send successful results from the Loot Collection process and you can view your results and similar ones over on the
                    Granblue Automation Statistics website."
                    hasIcon={true}
                    iconName="web"
                />
                <Checkbox
                    text="Enable Opt-in for API Integration"
                    subtitle="Enable API Integration with Granblue Automation Statistics"
                    isChecked={bsc.settings.api.enableOptInAPI}
                    onPress={() => bsc.setSettings({ ...bsc.settings, api: { ...bsc.settings.api, enableOptInAPI: !bsc.settings.api.enableOptInAPI } })}
                />
                {bsc.settings.api.enableOptInAPI ? (
                    <View>
                        <Text style={{ marginBottom: 10 }}>
                            {`How this works:\n\nInput your username and password below that you used to register a new account on the website. \n\nThe account registered on the website will be used to associate your success results from the Loot Collection process. A success result describes the Loot Collection process detecting a item drop after each run.`}
                        </Text>
                        <Input
                            label="Username"
                            containerStyle={{ marginLeft: -10 }}
                            value={bsc.settings.api.username}
                            placeholder="Insert your username here"
                            onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, api: { ...bsc.settings.api, username: value } })}
                        />
                        <Input
                            label="Password"
                            containerStyle={{ marginLeft: -10 }}
                            value={bsc.settings.api.password}
                            placeholder="Insert your password here"
                            onChangeText={(value: string) => bsc.setSettings({ ...bsc.settings, api: { ...bsc.settings.api, password: value } })}
                        />
                        <LoadingButton title="Test Login into API" loadingTitle="In progress..." isLoading={testInProgress} onPress={() => testAPIIntegration()} />
                    </View>
                ) : null}
            </View>
        )
    }

    const renderConfigurationSettings = () => {
        return (
            <View>
                <TitleDivider title="Configuration Settings" hasIcon={true} iconName="tune" />
                <Checkbox
                    text="Enable Debug Mode"
                    subtitle={`Enables debugging messages to show up in the log.\n\nIt will also enable saving screenshots to internal storage for debugging purposes. As such, it will increase average image processing time by ~500ms per operation.`}
                    isChecked={bsc.settings.game.debugMode}
                    onPress={() => bsc.setSettings({ ...bsc.settings, game: { ...bsc.settings.game, debugMode: !bsc.settings.game.debugMode } })}
                />

                <Text style={{ marginVertical: 10 }}>
                    Reduce Delays by X Seconds:{" "}
                    {bsc.settings.configuration.reduceDelaySeconds % 1 === 0 ? `${bsc.settings.configuration.reduceDelaySeconds}.0` : bsc.settings.configuration.reduceDelaySeconds}
                </Text>
                <Text style={{ marginBottom: 10, fontSize: 12, opacity: 0.7 }}>
                    The following setting below is useful if you have a fast enough connection that pages load almost instantly. If the amount selected reduces the delay to the negatives, then it will
                    default back to its original delay. Beware that changing this setting may lead to unintended behavior as the bot will be going faster, depending on how much you reduce each delay
                    by.
                </Text>
                <NumericInput
                    type="plus-minus"
                    leftButtonBackgroundColor="#eb5056"
                    rightButtonBackgroundColor="#EA3788"
                    rounded
                    valueType="real"
                    minValue={0.0}
                    step={0.1}
                    value={bsc.settings.configuration.reduceDelaySeconds}
                    onChange={(value) => bsc.setSettings({ ...bsc.settings, configuration: { ...bsc.settings.configuration, reduceDelaySeconds: value } })}
                    containerStyle={{ marginBottom: 10, alignSelf: "center" }}
                    totalWidth={Dimensions.get("screen").width * 0.9}
                    totalHeight={50}
                />

                {!bsc.settings.configuration.enableRandomizedDelayBetweenRuns ? (
                    <View>
                        <BouncyCheckbox
                            size={30}
                            fillColor={"red"}
                            unfillColor={"white"}
                            text="Enable Delay Between Runs"
                            iconStyle={{ borderColor: "red" }}
                            textStyle={{
                                textDecorationLine: "none",
                                color: "#000",
                            }}
                            style={{ marginVertical: 10, marginLeft: 2 }}
                            isChecked={bsc.settings.configuration.enableDelayBetweenRuns}
                            disableBuiltInState={true}
                            onPress={() => {
                                if (!bsc.settings.configuration.enableDelayBetweenRuns && bsc.settings.configuration.enableRandomizedDelayBetweenRuns) {
                                    bsc.setSettings({ ...bsc.settings, configuration: { ...bsc.settings.configuration, enableRandomizedDelayBetweenRuns: false } })
                                }

                                bsc.setSettings({ ...bsc.settings, configuration: { ...bsc.settings.configuration, enableDelayBetweenRuns: !bsc.settings.configuration.enableDelayBetweenRuns } })
                            }}
                        />
                        <Text style={{ marginBottom: 5, marginLeft: 2, fontSize: 12, opacity: 0.7 }}>Enable delay in seconds between runs to serve as a resting period.</Text>
                    </View>
                ) : null}

                {bsc.settings.configuration.enableDelayBetweenRuns ? (
                    <View>
                        <Text style={{ marginBottom: 10 }}>Delay: {bsc.settings.configuration.delayBetweenRuns}</Text>
                        <Slider
                            value={bsc.settings.configuration.delayBetweenRuns}
                            minimumValue={5}
                            maximumValue={60}
                            step={1}
                            onSlidingComplete={(value) => bsc.setSettings({ ...bsc.settings, configuration: { ...bsc.settings.configuration, delayBetweenRuns: value } })}
                            minimumTrackTintColor="black"
                            maximumTrackTintColor="gray"
                            thumbTintColor="teal"
                            thumbSize={25}
                            trackHeight={10}
                            style={{ width: "95%", alignSelf: "center", marginBottom: 10 }}
                        />
                    </View>
                ) : null}

                {!bsc.settings.configuration.enableDelayBetweenRuns ? (
                    <View>
                        <BouncyCheckbox
                            size={30}
                            fillColor={"red"}
                            unfillColor={"white"}
                            text={`Enable Randomized Delay Between\nRuns`}
                            iconStyle={{ borderColor: "red" }}
                            textStyle={{
                                textDecorationLine: "none",
                                color: "#000",
                            }}
                            style={{ marginVertical: 10, marginLeft: 2 }}
                            isChecked={bsc.settings.configuration.enableRandomizedDelayBetweenRuns}
                            disableBuiltInState={true}
                            onPress={() => {
                                if (!bsc.settings.configuration.enableRandomizedDelayBetweenRuns && bsc.settings.configuration.enableDelayBetweenRuns) {
                                    bsc.setSettings({ ...bsc.settings, configuration: { ...bsc.settings.configuration, enableDelayBetweenRuns: false } })
                                }

                                bsc.setSettings({
                                    ...bsc.settings,
                                    configuration: { ...bsc.settings.configuration, enableRandomizedDelayBetweenRuns: !bsc.settings.configuration.enableRandomizedDelayBetweenRuns },
                                })
                            }}
                        />
                        <Text style={{ marginBottom: 5, marginLeft: 2, fontSize: 12, opacity: 0.7 }}>Enable randomized delay in seconds between runs to serve as a resting period.</Text>
                    </View>
                ) : null}

                {bsc.settings.configuration.enableRandomizedDelayBetweenRuns ? (
                    <View>
                        <Text style={{ marginBottom: 10 }}>
                            Delay between {bsc.settings.configuration.delayBetweenRunsLowerBound}-{bsc.settings.configuration.delayBetweenRunsUpperBound} seconds
                        </Text>
                        <RangeSlider
                            range={[bsc.settings.configuration.delayBetweenRunsLowerBound, bsc.settings.configuration.delayBetweenRunsUpperBound]}
                            minimumValue={5}
                            maximumValue={60}
                            step={1}
                            minimumRange={1}
                            onSlidingComplete={(values) =>
                                bsc.setSettings({
                                    ...bsc.settings,
                                    configuration: { ...bsc.settings.configuration, delayBetweenRunsLowerBound: values[0], delayBetweenRunsUpperBound: values[1] },
                                })
                            }
                            outboundColor="gray"
                            inboundColor="black"
                            thumbTintColor="teal"
                            thumbSize={25}
                            trackHeight={10}
                            style={{ width: "95%", alignSelf: "center", marginBottom: 10 }}
                        />
                    </View>
                ) : null}

                <Checkbox
                    text="Enable Auto Exit Raid"
                    subtitle="Enables backing out of a Raid without retreating while under Semi/Full Auto after a certain period of time has passed."
                    isChecked={bsc.settings.raid.enableAutoExitRaid}
                    onPress={() => bsc.setSettings({ ...bsc.settings, raid: { ...bsc.settings.raid, enableAutoExitRaid: !bsc.settings.raid.enableAutoExitRaid } })}
                />

                {bsc.settings.raid.enableAutoExitRaid ? (
                    <View>
                        <Text style={{ marginBottom: 10 }}>Max Time Allowed for Semi/Full Auto: {bsc.settings.raid.timeAllowedUntilAutoExitRaid} minutes</Text>
                        <NumericInput
                            type="plus-minus"
                            leftButtonBackgroundColor="#eb5056"
                            rightButtonBackgroundColor="#EA3788"
                            rounded
                            valueType="integer"
                            minValue={1}
                            maxValue={15}
                            value={bsc.settings.raid.timeAllowedUntilAutoExitRaid}
                            onChange={(value) => bsc.setSettings({ ...bsc.settings, raid: { ...bsc.settings.raid, timeAllowedUntilAutoExitRaid: value } })}
                            containerStyle={{ marginBottom: 10, alignSelf: "center" }}
                            totalWidth={Dimensions.get("screen").width * 0.9}
                            totalHeight={50}
                        />
                    </View>
                ) : null}

                <Checkbox
                    text="Enable No Timeout"
                    subtitle="Enable no timeouts when attempting to farm Raids that appear infrequently."
                    isChecked={bsc.settings.raid.enableNoTimeout}
                    onPress={() => bsc.setSettings({ ...bsc.settings, raid: { ...bsc.settings.raid, enableNoTimeout: !bsc.settings.raid.enableNoTimeout } })}
                />

                <Checkbox
                    text="Enable Refreshing during Combat"
                    subtitle={
                        "Enables the ability to refresh to speed up Combat Mode whenever the Attack button disappears when it is pressed or during Full/Semi Auto. This option takes precedence above any other related setting to reloading during combat except via the reload command in a script."
                    }
                    isChecked={bsc.settings.configuration.enableRefreshDuringCombat}
                    onPress={() =>
                        bsc.setSettings({ ...bsc.settings, configuration: { ...bsc.settings.configuration, enableRefreshDuringCombat: !bsc.settings.configuration.enableRefreshDuringCombat } })
                    }
                />
                <Checkbox
                    text={`Enable Automatic Quick Summon\nduring Full/Semi Auto`}
                    subtitle={
                        'Enables the ability to automatically use Quick Summon during Full/Semi Auto. Note that this option only takes into effect when "Enable Refreshing during Combat" is turned on and that the bot is fighting a battle that is compatible with refreshing during combat.'
                    }
                    isChecked={bsc.settings.configuration.enableAutoQuickSummon}
                    onPress={() => bsc.setSettings({ ...bsc.settings, configuration: { ...bsc.settings.configuration, enableAutoQuickSummon: !bsc.settings.configuration.enableAutoQuickSummon } })}
                />
                <Checkbox
                    text={`Enable Bypassing Reset\nSummon Procedure`}
                    subtitle={
                        "Enables bypassing the bot resetting Summons if there are none of your chosen found during Summon Selection. The bot will reload the page and select the very first summon at the top of the list."
                    }
                    isChecked={bsc.settings.configuration.enableBypassResetSummon}
                    onPress={() => bsc.setSettings({ ...bsc.settings, configuration: { ...bsc.settings.configuration, enableBypassResetSummon: !bsc.settings.configuration.enableBypassResetSummon } })}
                />
            </View>
        )
    }

    const renderDeviceSettings = () => {
        return (
            <View>
                <TitleDivider
                    title="Device Settings"
                    subtitle={`Adjust and fine-tune settings related to device setups and image processing optimizations.`}
                    hasIcon={true}
                    iconName="tablet-cellphone"
                />
                <Text style={{ marginBottom: 10 }}>Set Confidence Level: {bsc.settings.android.confidence}%</Text>
                <NumericInput
                    type="plus-minus"
                    leftButtonBackgroundColor="#eb5056"
                    rightButtonBackgroundColor="#EA3788"
                    rounded
                    valueType="integer"
                    minValue={1}
                    maxValue={100}
                    value={bsc.settings.android.confidence}
                    onChange={(value) => bsc.setSettings({ ...bsc.settings, android: { ...bsc.settings.android, confidence: value } })}
                    containerStyle={{ marginBottom: 10, alignSelf: "center" }}
                    totalWidth={Dimensions.get("screen").width * 0.9}
                    totalHeight={50}
                />
                <Text style={{ marginBottom: 10 }}>Set Confidence Level for Multiple Matching: {bsc.settings.android.confidenceAll}%</Text>
                <NumericInput
                    type="plus-minus"
                    leftButtonBackgroundColor="#eb5056"
                    rightButtonBackgroundColor="#EA3788"
                    rounded
                    valueType="integer"
                    minValue={1}
                    maxValue={100}
                    value={bsc.settings.android.confidenceAll}
                    onChange={(value) => bsc.setSettings({ ...bsc.settings, android: { ...bsc.settings.android, confidenceAll: value } })}
                    containerStyle={{ marginBottom: 10, alignSelf: "center" }}
                    totalWidth={Dimensions.get("screen").width * 0.9}
                    totalHeight={50}
                />
                <Text>Set Custom Scale: {bsc.settings.android.customScale % 1 === 0 ? `${bsc.settings.android.customScale}.0` : bsc.settings.android.customScale}</Text>
                <Text style={{ marginBottom: 10, fontSize: 12, opacity: 0.7 }}>
                    Set the scale at which to resize existing image assets to match what would be shown on your device. Internally supported are 720p, 1080p, 1600p (Portrait) and 2560p (Landscape)
                    mode.
                </Text>
                <NumericInput
                    type="plus-minus"
                    leftButtonBackgroundColor="#eb5056"
                    rightButtonBackgroundColor="#EA3788"
                    rounded
                    valueType="real"
                    minValue={0.1}
                    maxValue={5.0}
                    step={0.1}
                    value={bsc.settings.android.customScale}
                    onChange={(value) => bsc.setSettings({ ...bsc.settings, android: { ...bsc.settings.android, customScale: value } })}
                    containerStyle={{ marginBottom: 10, alignSelf: "center" }}
                    totalWidth={Dimensions.get("screen").width * 0.9}
                    totalHeight={50}
                />
                <Checkbox
                    text="Enable Additional Delay Before Tap"
                    subtitle="Enables a range of delay before each tap in milliseconds (ms). The base point will be used to create a range from -100ms to +100ms using it to determine the additional delay."
                    isChecked={bsc.settings.android.enableDelayTap}
                    onPress={() => bsc.setSettings({ ...bsc.settings, android: { ...bsc.settings.android, enableDelayTap: !bsc.settings.android.enableDelayTap } })}
                />
                {bsc.settings.android.enableDelayTap ? (
                    <View>
                        <Text style={{ marginBottom: 10 }}>Set Base Point for Additional Delay: {bsc.settings.android.delayTapMilliseconds} milliseconds</Text>
                        <Slider
                            value={bsc.settings.android.delayTapMilliseconds}
                            minimumValue={1000}
                            maximumValue={5000}
                            step={100}
                            onSlidingComplete={(value) => bsc.setSettings({ ...bsc.settings, android: { ...bsc.settings.android, delayTapMilliseconds: value } })}
                            minimumTrackTintColor="black"
                            maximumTrackTintColor="gray"
                            thumbTintColor="teal"
                            thumbSize={25}
                            trackHeight={10}
                            style={{ width: "95%", alignSelf: "center", marginBottom: 10 }}
                        />
                    </View>
                ) : null}
                <Checkbox
                    text="Enable Test for Home Screen"
                    subtitle={`Enables test for getting to the Home screen instead of the regular bot process. If the test fails, then it will run a different test to find which scale is appropriate for your device.\n\nUseful for troubleshooting working confidences and scales for device compatibility.`}
                    isChecked={bsc.settings.android.enableTestForHomeScreen}
                    onPress={() => bsc.setSettings({ ...bsc.settings, android: { ...bsc.settings.android, enableTestForHomeScreen: !bsc.settings.android.enableTestForHomeScreen } })}
                />
            </View>
        )
    }

    const testAPIIntegration = async () => {
        setTestInProgress(true)
        await axios
            .post(`${bsc.entryPoint}/api/login`, { username: bsc.settings.api.username, password: bsc.settings.api.password }, { withCredentials: true })
            .then(() => {
                setTestFailed(false)
            })
            .catch((e: AxiosError) => {
                setTestFailed(true)
                setTestErrorMessage(`${e}`)
            })
            .finally(() => {
                setTestInProgress(false)
                setShowSnackbar(true)
            })
    }

    const testTwitter = () => {
        // Add listener to work around the UI freezing issue associated with Javacord blocking the thread.
        DeviceEventEmitter.addListener("testTwitter", (data) => {
            let result: string = data["message"]
            if (result !== "Test successfully completed.") {
                setTestFailed(true)
                setTestErrorMessage(result)
            } else {
                setTestFailed(false)
            }

            setTestInProgress(false)
            setShowSnackbar(true)
        })

        setTestInProgress(true)
        StartModule.startTwitterTest()
    }

    const testDiscord = () => {
        // Add listener to work around the UI freezing issue associated with Javacord blocking the thread.
        DeviceEventEmitter.addListener("testDiscord", (data) => {
            let result: string = data["message"]
            if (result !== "Test successfully completed.") {
                setTestFailed(true)
                setTestErrorMessage(result)
            } else {
                setTestFailed(false)
            }

            setTestInProgress(false)
            setShowSnackbar(true)
        })

        setTestInProgress(true)
        StartModule.startDiscordTest()
    }

    useEffect(() => {
        if (showSnackbar) {
            setTimeout(() => {
                setShowSnackbar(false)
            }, 10000)
        }
    }, [showSnackbar])

    return (
        <View style={styles.root}>
            <ScrollView>
                {renderNightmareSettings()}

                {renderSandboxDefenderSettings()}

                {renderTwitterSettings()}

                {renderDiscordSettings()}

                {renderAPIIntegrationSettings()}

                {renderConfigurationSettings()}

                {renderDeviceSettings()}
            </ScrollView>

            <Snackbar
                visible={showSnackbar}
                onDismiss={() => setShowSnackbar(false)}
                action={{
                    label: "Close",
                    onPress: () => {
                        setShowSnackbar(false)
                    },
                }}
                duration={10000}
                style={{ backgroundColor: testFailed ? "red" : "green", borderRadius: 10 }}
            >
                {testFailed ? testErrorMessage : "Test was successful."}
            </Snackbar>
        </View>
    )
}

export default ExtraSettings
