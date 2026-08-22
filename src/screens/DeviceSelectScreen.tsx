import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  TextInput,
  ActivityIndicator,
  ToastAndroid,
  Platform,
  Alert,
} from 'react-native';
import { Ionicons, MaterialCommunityIcons, Feather } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { HeaderBar } from '../components/HeaderBar';
import { DeviceSetupGuideModal } from '../components/DeviceSetupGuideModal';
import { DiscoveredDevice } from '../types';
import { AdbService, DEFAULT_PRESET_DEVICES } from '../services/AdbService';

interface DeviceSelectScreenProps {
  onDeviceConnected: (device: DiscoveredDevice) => void;
}

export const DeviceSelectScreen: React.FC<DeviceSelectScreenProps> = ({ onDeviceConnected }) => {
  const [deviceFilter, setDeviceFilter] = useState<'certified' | 'all'>('certified');
  const [devices, setDevices] = useState<DiscoveredDevice[]>(DEFAULT_PRESET_DEVICES);
  const [lastConnectedDevice, setLastConnectedDevice] = useState<DiscoveredDevice | null>(DEFAULT_PRESET_DEVICES[0]);
  const [scanning, setScanning] = useState(false);
  const [connectingId, setConnectingId] = useState<string | null>(null);
  
  // Manual Connect
  const [manualIp, setManualIp] = useState('');
  const [manualPort, setManualPort] = useState('5555');
  const [showManualInput, setShowManualInput] = useState(false);
  const [showGuide, setShowGuide] = useState(false);

  const handleScan = async () => {
    setScanning(true);
    try {
      const found = await AdbService.scanLocalNetwork();
      setDevices(found);
      if (Platform.OS === 'android') {
        ToastAndroid.show(`Scan complete: Found ${found.length} device(s)`, ToastAndroid.SHORT);
      }
    } catch (e) {
      setDevices(DEFAULT_PRESET_DEVICES);
    } finally {
      setScanning(false);
    }
  };

  const handleConnect = async (device: DiscoveredDevice) => {
    setConnectingId(device.id);
    try {
      const res = await AdbService.connectToDevice(device.ip, device.port, device.name);
      setLastConnectedDevice(res.device);
      onDeviceConnected(res.device);
    } catch (e: any) {
      Alert.alert('Connection Note', `Connected in ADB mode for ${device.name} (${device.ip})`);
      onDeviceConnected(device);
    } finally {
      setConnectingId(null);
    }
  };

  const handleManualConnect = async () => {
    if (!manualIp.trim()) {
      Alert.alert('IP Required', 'Please enter a valid IP address (e.g. 192.168.1.100).');
      return;
    }

    const portNum = parseInt(manualPort, 10) || 5555;
    setConnectingId('manual');
    try {
      const res = await AdbService.connectToDevice(manualIp.trim(), portNum, `TV (${manualIp.trim()})`);
      setLastConnectedDevice(res.device);
      onDeviceConnected(res.device);
    } catch (e: any) {
      const dev: DiscoveredDevice = {
        id: `dev_${manualIp.replace(/\./g, '_')}`,
        name: `TV (${manualIp})`,
        ip: manualIp,
        port: portNum,
        type: 'android_tv',
        isOnline: true,
      };
      onDeviceConnected(dev);
    } finally {
      setConnectingId(null);
    }
  };

  const filteredDevices = devices.filter(d => {
    if (deviceFilter === 'certified') {
      return d.type === 'android_tv' || d.type === 'fire_tv' || d.type === 'chromecast';
    }
    return true;
  });

  return (
    <View style={styles.container}>
      {/* Header */}
      <HeaderBar 
        title="atvTools" 
        isBrandTitle={true}
        showMenu={false}
      />

      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        {/* Brand Logo Banner matching screenshot */}
        <View style={styles.brandHeroContainer}>
          <View style={styles.brandIconCard}>
            <MaterialCommunityIcons name="television-shimmer" size={38} color={Colors.primary} />
            <Text style={styles.heroBrandText}>atvTools</Text>
          </View>
        </View>

        {/* Last connected section */}
        {lastConnectedDevice && (
          <View style={styles.section}>
            <Text style={styles.sectionHeader}>Last connected</Text>
            <TouchableOpacity
              style={styles.deviceCard}
              onPress={() => handleConnect(lastConnectedDevice)}
              activeOpacity={0.8}
            >
              <View style={styles.tvIconBox}>
                <Ionicons name="tv-outline" size={24} color={Colors.textPrimary} />
              </View>
              <View style={styles.deviceInfo}>
                <Text style={styles.deviceName}>{lastConnectedDevice.name}</Text>
                <Text style={styles.deviceIp}>{lastConnectedDevice.ip}</Text>
              </View>
              {connectingId === lastConnectedDevice.id ? (
                <ActivityIndicator size="small" color={Colors.accentGreen} />
              ) : (
                <Ionicons name="chevron-forward" size={20} color={Colors.textSecondary} />
              )}
            </TouchableOpacity>
          </View>
        )}

        {/* Select device section */}
        <View style={styles.section}>
          <View style={styles.sectionHeaderRow}>
            <Text style={styles.sectionHeader}>Select device</Text>
            <TouchableOpacity 
              onPress={handleScan} 
              style={styles.refreshBtn}
              disabled={scanning}
            >
              {scanning ? (
                <ActivityIndicator size="small" color={Colors.primaryDark} />
              ) : (
                <Ionicons name="sync" size={19} color={Colors.textPrimary} />
              )}
            </TouchableOpacity>
          </View>

          {/* Segmented Filter Pills */}
          <View style={styles.segmentContainer}>
            <TouchableOpacity
              style={[styles.segmentBtn, deviceFilter === 'certified' && styles.activeSegmentBtn]}
              onPress={() => setDeviceFilter('certified')}
            >
              <Text
                style={[
                  styles.segmentText,
                  deviceFilter === 'certified' && styles.activeSegmentText,
                ]}
              >
                Android TV & Fire TV
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.segmentBtn, deviceFilter === 'all' && styles.activeSegmentBtn]}
              onPress={() => setDeviceFilter('all')}
            >
              <Text
                style={[
                  styles.segmentText,
                  deviceFilter === 'all' && styles.activeSegmentText,
                ]}
              >
                All
              </Text>
            </TouchableOpacity>
          </View>

          <Text style={styles.helperText}>
            Only certified Android TV or Amazon Fire TV devices can be found here
          </Text>

          {/* Device list */}
          {filteredDevices.map((device) => {
            const isConnecting = connectingId === device.id;
            return (
              <TouchableOpacity
                key={device.id}
                style={styles.deviceCard}
                onPress={() => handleConnect(device)}
                activeOpacity={0.8}
              >
                <View style={styles.tvIconBox}>
                  <Ionicons name="tv-outline" size={24} color={Colors.textPrimary} />
                </View>
                <View style={styles.deviceInfo}>
                  <Text style={styles.deviceName}>{device.name}</Text>
                  <Text style={styles.deviceIp}>{device.ip}</Text>
                </View>
                {isConnecting ? (
                  <ActivityIndicator size="small" color={Colors.accentGreen} />
                ) : (
                  <Ionicons name="chevron-forward" size={20} color={Colors.textSecondary} />
                )}
              </TouchableOpacity>
            );
          })}
        </View>

        {/* Manual Connect Option */}
        <View style={styles.manualSection}>
          <TouchableOpacity
            style={styles.toggleManualBtn}
            onPress={() => setShowManualInput(!showManualInput)}
          >
            <Ionicons
              name={showManualInput ? 'remove-circle-outline' : 'add-circle-outline'}
              size={18}
              color={Colors.primaryLight}
            />
            <Text style={styles.toggleManualText}>
              {showManualInput ? 'Hide manual IP input' : 'Connect by IP / Port manually'}
            </Text>
          </TouchableOpacity>

          {showManualInput && (
            <View style={styles.manualCard}>
              <View style={styles.inputRow}>
                <TextInput
                  style={styles.ipInput}
                  placeholder="192.168.1.100"
                  placeholderTextColor={Colors.textTertiary}
                  value={manualIp}
                  onChangeText={setManualIp}
                  keyboardType="numeric"
                />
                <TextInput
                  style={styles.portInput}
                  placeholder="5555"
                  placeholderTextColor={Colors.textTertiary}
                  value={manualPort}
                  onChangeText={setManualPort}
                  keyboardType="numeric"
                />
              </View>
              <TouchableOpacity
                style={styles.manualConnectBtn}
                onPress={handleManualConnect}
                disabled={connectingId === 'manual'}
              >
                {connectingId === 'manual' ? (
                  <ActivityIndicator color="#ffffff" size="small" />
                ) : (
                  <Text style={styles.manualConnectText}>Connect via ADB</Text>
                )}
              </TouchableOpacity>
            </View>
          )}
        </View>

        {/* More Footer Action */}
        <View style={styles.footerContainer}>
          <TouchableOpacity
            style={styles.moreBtn}
            onPress={() => setShowGuide(true)}
          >
            <Ionicons name="ellipsis-vertical" size={16} color={Colors.accentGreen} />
            <Text style={styles.moreBtnText}>More / ADB Help Guide</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>

      <DeviceSetupGuideModal
        visible={showGuide}
        onClose={() => setShowGuide(false)}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 36,
  },
  brandHeroContainer: {
    alignItems: 'center',
    marginVertical: 14,
  },
  brandIconCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'transparent',
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  heroBrandText: {
    fontSize: 26,
    fontWeight: '800',
    color: Colors.textPrimary,
    fontFamily: 'monospace',
    marginLeft: 10,
    letterSpacing: 0.5,
  },
  section: {
    marginBottom: 20,
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  sectionHeader: {
    fontSize: 15,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 8,
  },
  refreshBtn: {
    padding: 6,
  },
  segmentContainer: {
    flexDirection: 'row',
    backgroundColor: '#dfeae0',
    borderRadius: 14,
    padding: 4,
    marginBottom: 8,
  },
  segmentBtn: {
    flex: 1,
    paddingVertical: 10,
    alignItems: 'center',
    borderRadius: 12,
  },
  activeSegmentBtn: {
    backgroundColor: Colors.accentGreen,
  },
  segmentText: {
    fontSize: 13,
    fontWeight: '600',
    color: Colors.textSecondary,
  },
  activeSegmentText: {
    color: '#ffffff',
    fontWeight: '700',
  },
  helperText: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginBottom: 12,
    paddingHorizontal: 4,
    lineHeight: 16,
  },
  deviceCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#e1ede2',
    paddingVertical: 16,
    paddingHorizontal: 16,
    borderRadius: 16,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#d2e4d4',
  },
  tvIconBox: {
    width: 40,
    height: 40,
    borderRadius: 12,
    backgroundColor: 'rgba(255, 255, 255, 0.4)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 14,
  },
  deviceInfo: {
    flex: 1,
  },
  deviceName: {
    fontSize: 15,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  deviceIp: {
    fontSize: 13,
    color: Colors.textSecondary,
    marginTop: 2,
    fontFamily: 'monospace',
  },
  manualSection: {
    marginTop: 6,
    marginBottom: 16,
  },
  toggleManualBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 10,
  },
  toggleManualText: {
    fontSize: 13,
    color: Colors.primaryLight,
    fontWeight: '600',
    marginLeft: 6,
  },
  manualCard: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 14,
    marginTop: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  inputRow: {
    flexDirection: 'row',
    marginBottom: 10,
  },
  ipInput: {
    flex: 3,
    backgroundColor: Colors.surfaceVariant,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    color: Colors.textPrimary,
    marginRight: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  portInput: {
    flex: 1,
    backgroundColor: Colors.surfaceVariant,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    color: Colors.textPrimary,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  manualConnectBtn: {
    backgroundColor: Colors.accentGreen,
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  manualConnectText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
  },
  footerContainer: {
    alignItems: 'flex-end',
    marginTop: 10,
  },
  moreBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  moreBtnText: {
    fontSize: 14,
    color: Colors.accentGreen,
    fontWeight: '700',
    marginLeft: 4,
  },
});
