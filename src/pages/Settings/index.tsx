import Checkbox from "../../components/CustomCheckbox"
import CustomButton from "../../components/CustomButton"
import CustomDropDownPicker from "../../components/CustomDropdownPicker"
import data from "../../data/data.json"
import DocumentPicker from "react-native-document-picker"
import React, { useContext, useEffect, useState } from "react"
import RNFS from "react-native-fs"
import TransferList from "../../components/TransferList"
import { BotStateContext } from "../../context/BotStateContext"
import { Dimensions, Modal, ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native"
import { Divider } from "react-native-elements"
import { Picker } from "@react-native-picker/picker"
import { Snackbar } from "react-native-paper"
import ArcarumHelper from "./FarmingModesHelpers/ArcarumHelper"
import ArcarumSandboxHelper from "./FarmingModesHelpers/ArcarumSandboxHelper"
import EventHelper from "./FarmingModesHelpers/EventHelper"
import GenericHelper from "./FarmingModesHelpers/GenericHelper"
import GuildWarsHelper from "./FarmingModesHelpers/GuildWarsHelper"
import ProvingGroundsHelper from "./FarmingModesHelpers/ProvingGroundsHelper"
import ROTBHelper from "./FarmingModesHelpers/ROTBHelper"
import XenoClashHelper from "./FarmingModesHelpers/XenoClashHelper"

interface Item {
    label: string
    value: string
}

export interface CombatScript {
    name: string
    script: string[]
}

const Settings = () => {
    const [modalOpen, setModalOpen] = useState<boolean>(false)
    const [firstTime, setFirstTime] = useState<boolean>(true)
    const [firstTime2, setFirstTime2] = useState<boolean>(true)
    const [snackbarOpen, setSnackbarOpen] = useState<boolean>(false)

    const [itemList, setItemList] = useState<Item[]>([])
    const [missionList, setMissionList] = useState<Item[]>([])

    const [farmingMode, setFarmingMode] = useState<string>("")
    const [item, setItem] = useState<string>("")
    const [mission, setMission] = useState<string>("")

    const bsc = useContext(BotStateContext)

    const styles = StyleSheet.create({
        root: {
            flex: 1,
            flexDirection: "column",
            justifyContent: "center",
            margin: 10,
        },
        farmingModePicker: {
            marginTop: 10,
            backgroundColor: farmingMode !== "" ? "azure" : "pink",
        },
        itemPicker: {
            marginTop: 10,
            backgroundColor: item !== "" ? "azure" : "pink",
        },
        missionPicker: {
            marginVertical: 10,
            backgroundColor: mission !== "" ? "azure" : "pink",
        },
        disabledPicker: {
            backgroundColor: "#808080",
            opacity: 0.7,
        },
        dropdown: {
            marginTop: 20,
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

    const farmingModes = [
        { value: "Quest" },
        { value: "Special" },
        { value: "Coop" },
        { value: "Raid" },
        { value: "Event" },
        { value: "Event (Token Drawboxes)" },
        { value: "Rise of the Beasts" },
        { value: "Guild Wars" },
        { value: "Dread Barrage" },
        { value: "Proving Grounds" },
        { value: "Xeno Clash" },
        { value: "Arcarum" },
        { value: "Arcarum Sandbox" },
        { value: "Generic" },
    ]

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // Callbacks

    // Load some specific states from context to local.
    useEffect(() => {
        setFarmingMode(bsc.settings.game.farmingMode)
        setItem(bsc.settings.game.item)
        setMission(bsc.settings.game.mission)
        setFirstTime(false)
    }, [])

    useEffect(() => {
        if (!firstTime) {
            // Reset selected Item and Mission and save the farming mode.
            bsc.setSettings({
                ...bsc.settings,
                game: { ...bsc.settings.game, farmingMode: farmingMode.toString(), item: "", mission: "", map: "" },
                nightmare: {
                    ...bsc.settings.nightmare,
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
                    ...bsc.settings.event,
                    enableLocationIncrementByOne: false,
                },
                arcarum: {
                    ...bsc.settings.arcarum,
                    enableStopOnArcarumBoss: true,
                },
                sandbox: {
                    ...bsc.settings.sandbox,
                    enableDefender: false,
                    enableGoldChest: false,
                    enableCustomDefenderSettings: false,
                    numberOfDefenders: 1,
                    defenderCombatScriptName: "",
                    defenderCombatScript: [],
                    defenderGroupNumber: 1,
                    defenderPartyNumber: 1,
                },
            })

            setFirstTime2(false)
        }
    }, [farmingMode])

    // Save every other setting.
    useEffect(() => {
        if (!firstTime) {
            bsc.setSettings({
                ...bsc.settings,
                game: {
                    ...bsc.settings.game,
                    item: item.toString(),
                    mission: mission.toString(),
                },
            })
        }
    }, [item, mission])

    // Populates the item list based on farming mode.
    useEffect(() => {
        let newItemList: Item[] = []
        if (
            farmingMode === "Quest" ||
            farmingMode === "Special" ||
            farmingMode === "Coop" ||
            farmingMode === "Raid" ||
            farmingMode === "Event" ||
            farmingMode === "Event (Token Drawboxes)" ||
            farmingMode === "Rise of the Beasts" ||
            farmingMode === "Guild Wars" ||
            farmingMode === "Dread Barrage" ||
            farmingMode === "Proving Grounds" ||
            farmingMode === "Xeno Clash" ||
            farmingMode === "Arcarum" ||
            farmingMode === "Arcarum Sandbox" ||
            farmingMode === "Generic"
        ) {
            Object.values(data[farmingMode]).forEach((tempItems) => {
                tempItems.items.forEach((item) => {
                    newItemList = newItemList.concat({ label: item, value: item })
                })
            })
        }

        // Remove any duplicates.
        const filteredNewItemList = newItemList.filter((v, i, a) => a.findIndex((t) => t.label === v.label) === i)
        setItemList(filteredNewItemList)

        if (!firstTime2) {
            setItem("")
        }
    }, [farmingMode])

    // Populate the mission list based on item.
    useEffect(() => {
        let newMissionList: Item[] = []
        if (
            bsc.settings.game.farmingMode === "Quest" ||
            bsc.settings.game.farmingMode === "Special" ||
            bsc.settings.game.farmingMode === "Coop" ||
            bsc.settings.game.farmingMode === "Raid" ||
            bsc.settings.game.farmingMode === "Event" ||
            bsc.settings.game.farmingMode === "Event (Token Drawboxes)" ||
            bsc.settings.game.farmingMode === "Rise of the Beasts" ||
            bsc.settings.game.farmingMode === "Guild Wars" ||
            bsc.settings.game.farmingMode === "Dread Barrage" ||
            bsc.settings.game.farmingMode === "Proving Grounds" ||
            bsc.settings.game.farmingMode === "Xeno Clash" ||
            bsc.settings.game.farmingMode === "Arcarum" ||
            bsc.settings.game.farmingMode === "Arcarum Sandbox" ||
            bsc.settings.game.farmingMode === "Generic"
        ) {
            Object.entries(data[bsc.settings.game.farmingMode]).forEach((obj) => {
                if (obj[1].items.indexOf(item.toString()) !== -1) {
                    newMissionList = newMissionList.concat({ label: obj[0], value: obj[0] })
                }
            })
        }

        const filteredNewMissionList = Array.from(new Set(newMissionList))
        setMissionList(filteredNewMissionList)

        if (!firstTime2) {
            setMission("")
        }
    }, [item])

    // Fetch the map that corresponds to the selected mission if applicable. Not for Coop.
    useEffect(() => {
        if (
            bsc.settings.game.farmingMode === "Quest" ||
            bsc.settings.game.farmingMode === "Special" ||
            bsc.settings.game.farmingMode === "Raid" ||
            bsc.settings.game.farmingMode === "Event" ||
            bsc.settings.game.farmingMode === "Event (Token Drawboxes)" ||
            bsc.settings.game.farmingMode === "Rise of the Beasts" ||
            bsc.settings.game.farmingMode === "Guild Wars" ||
            bsc.settings.game.farmingMode === "Dread Barrage" ||
            bsc.settings.game.farmingMode === "Proving Grounds" ||
            bsc.settings.game.farmingMode === "Xeno Clash" ||
            bsc.settings.game.farmingMode === "Arcarum" ||
            bsc.settings.game.farmingMode === "Arcarum Sandbox" ||
            bsc.settings.game.farmingMode === "Generic"
        ) {
            Object.entries(data[bsc.settings.game.farmingMode]).every((obj) => {
                if (obj[0] === mission) {
                    bsc.setSettings({ ...bsc.settings, game: { ...bsc.settings.game, mission: mission, map: obj[1].map } })
                    return false
                } else {
                    return true
                }
            })
        }
    }, [mission])

    useEffect(() => {
        // Manually set this flag to false as the snackbar autohiding does not set this to false automatically.
        setSnackbarOpen(true)
        setTimeout(() => setSnackbarOpen(false), 1500)
    }, [bsc.readyStatus])

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    // Rendering

    const renderCombatScriptSetting = () => {
        return (
            <View>
                <CustomButton
                    title={bsc.settings.game.combatScriptName === "" ? "Select Combat Script" : `Selected: ${bsc.settings.game.combatScriptName}`}
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

                                    bsc.setSettings({ ...bsc.settings, game: { ...bsc.settings.game, combatScriptName: pickerResult.name ? pickerResult.name : "", combatScript: newCombatScript } })
                                })
                            }
                        } catch (e: any) {
                            if (!e.message.includes("Can't perform a React")) {
                                console.warn(e)
                            }
                            bsc.setSettings({ ...bsc.settings, game: { ...bsc.settings.game, combatScriptName: "", combatScript: [] } })
                        }
                    }}
                />

                <Text style={{ marginBottom: 10, fontSize: 12, opacity: 0.7, color: "black" }}>
                    Combat Scripts need to be located in the root of the storage (/storage/emulated/0/), otherwise the app would not be able to open it due to Android SDK changes. To deselect,
                    cancel/back out of the document picker. If no combat script is selected, Full/Semi Auto is used by default.
                </Text>
            </View>
        )
    }

    const renderFarmingModeSetting = () => {
        return (
            <View>
                <CustomDropDownPicker containerStyle={styles.farmingModePicker} placeholder="Select Farming Mode" data={farmingModes} value={farmingMode} setValue={setFarmingMode} />

                {bsc.settings.game.farmingMode === "Special" ||
                bsc.settings.game.farmingMode === "Event" ||
                bsc.settings.game.farmingMode === "Event (Token Drawboxes)" ||
                bsc.settings.game.farmingMode === "Rise of the Beasts" ||
                bsc.settings.game.farmingMode === "Xeno Clash" ? (
                    <Checkbox
                        text="Enable Nightmare Settings"
                        subtitle="Enable additional settings to show up in the Extra Settings page."
                        isChecked={bsc.settings.nightmare.enableNightmare}
                        onPress={() => bsc.setSettings({ ...bsc.settings, nightmare: { ...bsc.settings.nightmare, enableNightmare: !bsc.settings.nightmare.enableNightmare } })}
                    />
                ) : null}

                {ArcarumHelper()}
                {ArcarumSandboxHelper()}
                {EventHelper()}
                {GenericHelper()}
                {GuildWarsHelper()}
                {ProvingGroundsHelper()}
                {ROTBHelper()}
                {XenoClashHelper()}
            </View>
        )
    }

    const renderItemSetting = () => {
        return (
            <CustomDropDownPicker
                containerStyle={styles.itemPicker}
                placeholder="Select Item"
                search={itemList.length > 10}
                showModal={itemList.length > 10}
                data={itemList}
                value={item}
                setValue={setItem}
                disabled={farmingMode === ""}
                disabledContainerStyle={[styles.itemPicker, styles.disabledPicker]}
            />
        )
    }

    const renderMissionSetting = () => {
        return (
            <CustomDropDownPicker
                containerStyle={[styles.missionPicker, { marginBottom: 0 }]}
                placeholder="Select Mission"
                search={missionList.length > 10}
                showModal={missionList.length > 10}
                data={missionList}
                value={mission}
                setValue={setMission}
                disabled={farmingMode === "" || item === ""}
                disabledContainerStyle={[styles.missionPicker, styles.disabledPicker]}
            />
        )
    }

    const renderItemAmountSetting = () => {
        return (
            <View style={{ marginTop: 10 }}>
                <Text style={{ color: "#000" }}>Item Amount:</Text>
                <Picker
                    selectedValue={bsc.settings.game.itemAmount}
                    onValueChange={(value) => bsc.setSettings({ ...bsc.settings, game: { ...bsc.settings.game, itemAmount: value } })}
                    mode="dropdown"
                    style={{ color: "#000" }}
                    dropdownIconColor={"#000"}
                >
                    {[...Array(999 - 1 + 1).keys()]
                        .map((x) => x + 1)
                        .map((value) => {
                            return <Picker.Item key={`key-${value}`} label={`${value}`} value={value} />
                        })}
                </Picker>
            </View>
        )
    }

    const renderSummonSetting = () => {
        return (
            <View>
                {bsc.settings.game.farmingMode !== "Coop" && bsc.settings.game.farmingMode !== "Arcarum" && bsc.settings.game.farmingMode !== "Arcarum Sandbox" ? (
                    <View style={{ zIndex: 9996 }}>
                        {bsc.settings.game.summons.length > 0 ? (
                            <CustomButton title="Select Support Summon(s)" width={"100%"} onPress={() => setModalOpen(true)} />
                        ) : (
                            <CustomButton title="Select Support Summon(s)" backgroundColor="red" width={"100%"} onPress={() => setModalOpen(true)} />
                        )}
                        <Modal transparent={true} animationType="fade" statusBarTranslucent={true} visible={modalOpen} onRequestClose={() => setModalOpen(false)}>
                            <View style={styles.modal}>
                                <TouchableOpacity style={styles.outsideModal} onPress={() => setModalOpen(false)} />
                                <View style={styles.componentContainer}>
                                    <TransferList isNightmare={false} />
                                </View>
                            </View>
                        </Modal>
                    </View>
                ) : null}
            </View>
        )
    }

    const renderGroupPartySettings = () => {
        return (
            <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                <View style={{ width: Dimensions.get("window").width * 0.3 }}>
                    <Text style={{ color: "#000" }}>Group #:</Text>
                    <Picker
                        selectedValue={bsc.settings.game.groupNumber}
                        onValueChange={(value) => bsc.setSettings({ ...bsc.settings, game: { ...bsc.settings.game, groupNumber: value } })}
                        mode="dropdown"
                        style={{ color: "#000" }}
                        dropdownIconColor={"#000"}
                    >
                        {[...Array(14).keys()]
                            .map((x) => x + 1)
                            .map((value) => {
                                return <Picker.Item key={`key-${value}`} label={`${value}`} value={value} />
                            })}
                    </Picker>
                </View>
                <View style={{ width: Dimensions.get("window").width * 0.3 }}>
                    <Text style={{ color: "#000" }}>Party #:</Text>
                    <Picker
                        selectedValue={bsc.settings.game.partyNumber}
                        onValueChange={(value) => bsc.setSettings({ ...bsc.settings, game: { ...bsc.settings.game, partyNumber: value } })}
                        mode="dropdown"
                        style={{ color: "#000" }}
                        dropdownIconColor={"#000"}
                    >
                        {[...Array(6).keys()]
                            .map((x) => x + 1)
                            .map((value) => {
                                return <Picker.Item key={`key-${value}`} label={`${value}`} value={value} />
                            })}
                    </Picker>
                </View>
            </View>
        )
    }

    return (
        <View style={styles.root}>
            <ScrollView nestedScrollEnabled={true} contentContainerStyle={{ flexGrow: 1 }}>
                <View style={{ marginHorizontal: 20 }}>
                    {renderCombatScriptSetting()}

                    <Divider />

                    {renderFarmingModeSetting()}

                    {renderItemSetting()}

                    {renderMissionSetting()}

                    {renderItemAmountSetting()}

                    {renderSummonSetting()}

                    {renderGroupPartySettings()}
                </View>
            </ScrollView>

            <Snackbar
                visible={snackbarOpen}
                onDismiss={() => setSnackbarOpen(false)}
                action={{
                    label: "Close",
                    onPress: () => {
                        setSnackbarOpen(false)
                    },
                }}
                style={{ backgroundColor: bsc.readyStatus ? "green" : "red", borderRadius: 10 }}
            >
                {bsc.readyStatus ? "Bot is ready!" : "Bot is not ready!"}
            </Snackbar>
        </View>
    )
}

export default Settings
