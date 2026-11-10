import React, { useState } from "react";
import { Button, Modal, StatusBar, Text, View } from "react-native";
import { KeyboardProvider } from "react-native-keyboard-controller";

import type { StatusBarStyle } from "react-native";

export default function StatusBarRegression() {
  const [mounted, setMounted] = useState(false);
  const [style, setStyle] = useState<StatusBarStyle>("light-content");
  const [modal, setModal] = useState(false);
  const content = (inModal: boolean) => (
    <View
      style={{
        flex: 1,
        paddingTop: 80,
        backgroundColor: style === "dark-content" ? "white" : "#242424",
      }}
    >
      <Text style={{ color: style === "dark-content" ? "black" : "white" }}>
        {`Style: ${style}`}
      </Text>
      <Button title="Dark content" onPress={() => setStyle("dark-content")} />
      <Button title="Light content" onPress={() => setStyle("light-content")} />
      <Button
        title={inModal ? "Close modal" : "Open modal"}
        onPress={() => setModal(!inModal)}
      />
    </View>
  );

  if (!mounted) {
    return (
      <View style={{ flex: 1, paddingTop: 80 }}>
        <StatusBar barStyle="light-content" />
        <Button title="Mount provider" onPress={() => setMounted(true)} />
      </View>
    );
  }

  return (
    <KeyboardProvider>
      <StatusBar barStyle={style} />
      {content(false)}
      <Modal visible={modal} onRequestClose={() => setModal(false)}>
        {content(true)}
      </Modal>
    </KeyboardProvider>
  );
}
