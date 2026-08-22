import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  TextInput,
  ActivityIndicator,
  Clipboard,
  ToastAndroid,
  Platform,
} from 'react-native';
import { Ionicons, MaterialCommunityIcons, Feather } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { HeaderBar } from '../components/HeaderBar';
import { DiscoveredDevice, ShellCommandHistory } from '../types';
import { AdbService } from '../services/AdbService';

interface ShellScreenProps {
  device: DiscoveredDevice;
  onDisconnect: () => void;
}

export const ShellScreen: React.FC<ShellScreenProps> = ({ device, onDisconnect }) => {
  const [command, setCommand] = useState('');
  const [loading, setLoading] = useState(false);
  const [history, setHistory] = useState<ShellCommandHistory[]>([
    {
      id: 'cmd_init',
      command: 'getprop ro.product.model',
      output: 'onn UHD Streaming Device',
      exitCode: 0,
      timestamp: Date.now(),
    },
  ]);

  const runCommand = async (cmdToRun: string) => {
    const trimmed = cmdToRun.trim();
    if (!trimmed) return;

    setLoading(true);
    try {
      const res = await AdbService.runShellCommand(trimmed);
      const newEntry: ShellCommandHistory = {
        id: `cmd_${Date.now()}`,
        command: trimmed,
        output: res.output,
        exitCode: res.exitCode,
        timestamp: Date.now(),
      };
      setHistory([newEntry, ...history]);
      setCommand('');
    } catch (e: any) {
      const errEntry: ShellCommandHistory = {
        id: `cmd_${Date.now()}`,
        command: trimmed,
        output: `Error: ${e.message || 'Unknown shell failure'}`,
        exitCode: 1,
        timestamp: Date.now(),
      };
      setHistory([errEntry, ...history]);
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = (text: string) => {
    Clipboard.setString(text);
    if (Platform.OS === 'android') {
      ToastAndroid.show('Copied output to clipboard', ToastAndroid.SHORT);
    }
  };

  const quickCommands = [
    { label: 'top (CPU)', cmd: 'top -n 1 -m 5' },
    { label: 'df -h (Disk)', cmd: 'df -h' },
    { label: 'Current App', cmd: 'dumpsys window | grep mCurrentFocus' },
    { label: 'Installed Apps', cmd: 'pm list packages -3' },
    { label: 'Battery / Power', cmd: 'dumpsys battery' },
    { label: 'IP Address', cmd: 'ip addr show wlan0' },
    { label: 'Android Props', cmd: 'getprop | head -n 25' },
    { label: 'logcat (Recent)', cmd: 'logcat -d -t 30' },
  ];

  return (
    <View style={styles.container}>
      <HeaderBar
        title={device.name}
        subtitle="DEVICE"
        showBack={true}
        onBack={onDisconnect}
      />

      <View style={styles.content}>
        {/* Quick Command Chips */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          style={styles.quickBar}
          contentContainerStyle={styles.quickBarContent}
        >
          {quickCommands.map((item) => (
            <TouchableOpacity
              key={item.label}
              style={styles.quickChip}
              onPress={() => runCommand(item.cmd)}
            >
              <Text style={styles.quickChipText}>{item.label}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* Command Input Bar */}
        <View style={styles.inputCard}>
          <Text style={styles.promptSymbol}>$</Text>
          <TextInput
            style={styles.commandInput}
            placeholder="Type ADB shell command..."
            placeholderTextColor="#7a9282"
            value={command}
            onChangeText={setCommand}
            autoCapitalize="none"
            autoCorrect={false}
            returnKeyType="go"
            onSubmitEditing={() => runCommand(command)}
          />
          <TouchableOpacity
            style={[styles.runBtn, (!command.trim() || loading) && styles.disabledRunBtn]}
            onPress={() => runCommand(command)}
            disabled={!command.trim() || loading}
          >
            {loading ? (
              <ActivityIndicator size="small" color="#ffffff" />
            ) : (
              <Ionicons name="play" size={18} color="#ffffff" />
            )}
          </TouchableOpacity>
        </View>

        {/* Terminal Output Stream */}
        <ScrollView contentContainerStyle={styles.terminalContainer} showsVerticalScrollIndicator={true}>
          {history.map((item) => (
            <View key={item.id} style={styles.terminalCard}>
              <View style={styles.terminalCardHeader}>
                <View style={styles.cmdRow}>
                  <Text style={styles.terminalPrompt}>$</Text>
                  <Text style={styles.terminalCommandText}>{item.command}</Text>
                </View>
                <TouchableOpacity onPress={() => copyToClipboard(item.output)} style={styles.copyBtn}>
                  <Ionicons name="copy-outline" size={16} color="#8fa896" />
                </TouchableOpacity>
              </View>
              <Text style={[styles.terminalOutputText, item.exitCode !== 0 && styles.terminalErrorText]}>
                {item.output || '(No output)'}
              </Text>
            </View>
          ))}
        </ScrollView>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  content: {
    flex: 1,
    paddingHorizontal: 14,
    paddingTop: 10,
  },
  quickBar: {
    maxHeight: 44,
    marginBottom: 10,
  },
  quickBarContent: {
    paddingRight: 10,
  },
  quickChip: {
    backgroundColor: '#e1ede2',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 14,
    marginRight: 8,
    borderWidth: 1,
    borderColor: '#d2e4d4',
  },
  quickChipText: {
    fontSize: 12,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  inputCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#17221b',
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderWidth: 1,
    borderColor: '#293a2e',
    marginBottom: 12,
  },
  promptSymbol: {
    color: '#34d399',
    fontSize: 18,
    fontWeight: '800',
    marginRight: 8,
    fontFamily: 'monospace',
  },
  commandInput: {
    flex: 1,
    color: '#ffffff',
    fontSize: 14,
    fontFamily: 'monospace',
    paddingVertical: 10,
  },
  runBtn: {
    backgroundColor: Colors.accentGreen,
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  disabledRunBtn: {
    opacity: 0.5,
  },
  terminalContainer: {
    paddingBottom: 90,
  },
  terminalCard: {
    backgroundColor: '#111813',
    borderRadius: 12,
    padding: 12,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#243528',
  },
  terminalCardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderBottomWidth: 1,
    borderBottomColor: '#202e23',
    paddingBottom: 6,
    marginBottom: 8,
  },
  cmdRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  terminalPrompt: {
    color: '#34d399',
    fontWeight: '800',
    marginRight: 6,
    fontFamily: 'monospace',
  },
  terminalCommandText: {
    color: '#ffffff',
    fontSize: 13,
    fontWeight: '700',
    fontFamily: 'monospace',
  },
  copyBtn: {
    padding: 4,
  },
  terminalOutputText: {
    color: '#b9f3cb',
    fontSize: 12,
    fontFamily: 'monospace',
    lineHeight: 18,
  },
  terminalErrorText: {
    color: '#f87171',
  },
});
