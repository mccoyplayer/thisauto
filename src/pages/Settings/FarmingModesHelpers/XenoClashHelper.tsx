import { useContext } from "react"
import { View, Dimensions } from "react-native"
import { Text } from "react-native-elements"
import NumericInput from "react-native-numeric-input"
import CustomCheckbox from "../../../components/CustomCheckbox"
import { BotStateContext } from "../../../context/BotStateContext"

const XenoClashHelper = () => {
    const bsc = useContext(BotStateContext)

    if (bsc.settings.game.farmingMode === "Xeno Clash") {
        return (
            <View>
                <CustomCheckbox
                    text="Enable Selection of Bottom Option"
                    subtitle="Enabling this will select the bottom Xeno Clash option. By default, it selects the top option."
                    isChecked={bsc.settings.xenoClash.selectTopOption}
                    onPress={() => bsc.setSettings({ ...bsc.settings, xenoClash: { ...bsc.settings.xenoClash, selectTopOption: !bsc.settings.xenoClash.selectTopOption } })}
                />

                <CustomCheckbox
                    text="Enable if Xeno Clash is in different position"
                    subtitle="Enable this to properly select Xeno Clash if it is not positioned first on the list of events in the Home Menu."
                    isChecked={bsc.settings.xenoClash.enableNewPosition}
                    onPress={() => bsc.setSettings({ ...bsc.settings, xenoClash: { ...bsc.settings.xenoClash, enableNewPosition: !bsc.settings.xenoClash.enableNewPosition } })}
                />

                {bsc.settings.xenoClash.enableNewPosition ? (
                    <View>
                        <Text style={{ marginBottom: 10, fontSize: 12, opacity: 0.7, color: "black" }}>Default is the first position or the value of 0</Text>
                        <NumericInput
                            type="plus-minus"
                            leftButtonBackgroundColor="#eb5056"
                            rightButtonBackgroundColor="#EA3788"
                            rounded
                            valueType="integer"
                            minValue={0}
                            maxValue={5}
                            step={1}
                            value={bsc.settings.xenoClash.newPosition}
                            onChange={(value) => bsc.setSettings({ ...bsc.settings, xenoClash: { ...bsc.settings.xenoClash, newPosition: value } })}
                            containerStyle={{ marginBottom: 10, alignSelf: "center" }}
                            totalWidth={Dimensions.get("screen").width * 0.9}
                            totalHeight={50}
                        />
                    </View>
                ) : null}
            </View>
        )
    } else {
        return null
    }
}

export default XenoClashHelper