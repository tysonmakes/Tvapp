import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ToastAndroid,
  Platform,
} from 'react-native';
import { Ionicons, MaterialCommunityIcons, Feather } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { HeaderBar } from '../components/HeaderBar';
import { DiscoveredDevice, DeviceInfoData } from '../types';
import { MOCK_DEVICE_INFO, AdbService } from '../services/AdbService';

interface InfoScreenProps {
  device: DiscoveredDevice;
  onDisconnect: () => void;
}

export const InfoScreen: React.FC<InfoScreenProps> = ({ device, onDisconnect }) => {
  const [info, setInfo] = useState<DeviceInfoData>(MOCK_DEVICE_INFO);
  const [stayAwake, setStayAwake] = useState(true);

  const toggleStayAwake = async () => {
    const nextVal = !stayAwake;
    setStayAwake(nextVal);
    try {
      await AdbService.runShellCommand(`settings put global stay_on_while_plugged_in ${nextVal ? 3 : 0}`);
      if (Platform.OS === 'android') {
        ToastAndroid.show(`Stay Awake while plugged in: ${nextVal ? 'ON' : 'OFF'}`, ToastAndroid.SHORT);
      }
    } catch (e) {
      // ignore
    }
  };

  return (
    <View style={styles.container}>
      <HeaderBar
        title={device.name}
        subtitle="DEVICE"
        showBack={true}
        onBack={onDisconnect}
      />

      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        {/* Device Brand Header Card */}
        <View style={styles.heroCard}>
          <View style={styles.heroIconBox}>
            <Ionicons name="tv" size={32} color="#ffffff" />
          </View>
          <View style={styles.heroDetails}>
            <Text style={styles.heroModel}>{info.model}</Text>
            <Text style={styles.heroManufacturer}>{info.manufacturer} • Android {info.androidVersion}</Text>
            <View style={styles.ipBadge}>
              <View style={styles.greenDot} />
              <Text style={styles.ipBadgeText}>{device.ip}:{device.port}</Text>
            </View>
          </View>
        </View>

        {/* RAM Usage Meter */}
        <View style={styles.metricCard}>
          <View style={styles.metricHeader}>
            <View style={styles.metricTitleRow}>
              <MaterialCommunityIcons name="memory" size={20} color={Colors.accentGreen} />
              <Text style={styles.metricTitle}>RAM Memory</Text>
            </View>
            <Text style={styles.metricUsage}>{info.ramUsed} / {info.ramTotal} ({info.ramPercent}%)</Text>
          </View>
          <View style={styles.meterTrack}>
            <View style={[styles.meterFill, { width: `${info.ramPercent}%` }]} />
          </View>
        </View>

        {/* Storage Usage Meter */}
        <View style={styles.metricCard}>
          <View style={styles.metricHeader}>
            <View style={styles.metricTitleRow}>
              <MaterialCommunityIcons name="harddisk" size={20} color={Colors.accentGreen} />
              <Text style={styles.metricTitle}>Internal Storage</Text>
            </View>
            <Text style={styles.metricUsage}>{info.storageUsed} / {info.storageTotal} ({info.storagePercent}%)</Text>
          </View>
          <View style={styles.meterTrack}>
            <View style={[styles.meterFill, { width: `${info.storagePercent}%`, backgroundColor: '#3b82f6' }]} />
          </View>
        </View>

        {/* Specs Table Card */}
        <View style={styles.specsCard}>
          <Text style={styles.cardHeader}>Hardware & OS</Text>

          <View style={styles.specRow}>
            <Text style={styles.specLabel}>Resolution</Text>
            <Text style={styles.specValue}>{info.resolution}</Text>
          </View>

          <View style={styles.specRow}>
            <Text style={styles.specLabel}>Density (DPI)</Text>
            <Text style={styles.specValue}>{info.density}</Text>
          </View>

          <View style={styles.specRow}>
            <Text style={styles.specLabel}>SDK API Level</Text>
            <Text style={styles.specValue}>API {info.sdkVersion}</Text>
          </View>

          <View style={styles.specRow}>
            <Text style={styles.specLabel}>Security Patch</Text>
            <Text style={styles.specValue}>{info.securityPatch}</Text>
          </View>

          <View style={styles.specRow}>
            <Text style={styles.specLabel}>Build ID</Text>
            <Text style={styles.specValue} numberOfLines={1}>{info.buildNumber}</Text>
          </View>

          <View style={styles.specRow}>
            <Text style={styles.specLabel}>System Uptime</Text>
            <Text style={styles.specValue}>{info.uptime}</Text>
          </View>
        </View>

        {/* Network & Connectivity Card */}
        <View style={styles.specsCard}>
          <Text style={styles.cardHeader}>Network & Wi-Fi</Text>

          <View style={styles.specRow}>
            <Text style={styles.specLabel}>Wi-Fi Network</Text>
            <Text style={styles.specValue}>{info.wifiSsid}</Text>
          </View>

          <View style={styles.specRow}>
            <Text style={styles.specLabel}>IP Address</Text>
            <Text style={styles.specValue}>{device.ip}</Text>
          </View>

          <View style={styles.specRow}>
            <Text style={styles.specLabel}>MAC Address</Text>
            <Text style={styles.specValue}>{info.macAddress}</Text>
          </View>

          <View style={styles.specRow}>
            <Text style={styles.specLabel}>ADB Port</Text>
            <Text style={styles.specValue}>{device.port}</Text>
          </View>
        </View>

        {/* Quick System Tweaks */}
        <View style={styles.specsCard}>
          <Text style={styles.cardHeader}>Developer Tweaks</Text>

          <TouchableOpacity style={styles.tweakRow} onPress={toggleStayAwake}>
            <View style={styles.tweakTextWrap}>
              <Text style={styles.tweakTitle}>Stay Awake on Power</Text>
              <Text style={styles.tweakSub}>Screen will never sleep while charging</Text>
            </View>
            <MaterialCommunityIcons
              name={stayAwake ? 'toggle-switch' : 'toggle-switch-off-outline'}
              size={30}
              color={stayAwake ? Colors.accentGreen : Colors.textTertiary}
            />
          </TouchableOpacity>
        </View>
      </ScrollView>
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
    paddingBottom: 90,
  },
  heroCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 16,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: Colors.borderLight,
  },
  heroIconBox: {
    width: 54,
    height: 54,
    borderRadius: 16,
    backgroundColor: Colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 14,
  },
  heroDetails: {
    flex: 1,
  },
  heroModel: {
    fontSize: 16,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  heroManufacturer: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  ipBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#e1ede2',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 8,
    alignSelf: 'flex-start',
    marginTop: 6,
  },
  greenDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#22c55e',
    marginRight: 6,
  },
  ipBadgeText: {
    fontSize: 11,
    color: Colors.textPrimary,
    fontWeight: '600',
    fontFamily: 'monospace',
  },
  metricCard: {
    backgroundColor: '#ffffff',
    borderRadius: 14,
    padding: 14,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: Colors.borderLight,
  },
  metricHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  metricTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  metricTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginLeft: 6,
  },
  metricUsage: {
    fontSize: 12,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  meterTrack: {
    height: 8,
    backgroundColor: '#e2ece4',
    borderRadius: 4,
    overflow: 'hidden',
  },
  meterFill: {
    height: '100%',
    backgroundColor: Colors.accentGreen,
    borderRadius: 4,
  },
  specsCard: {
    backgroundColor: '#ffffff',
    borderRadius: 14,
    padding: 14,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: Colors.borderLight,
  },
  cardHeader: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.accentGreen,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 10,
  },
  specRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 7,
    borderBottomWidth: 1,
    borderBottomColor: '#f1f5f2',
  },
  specLabel: {
    fontSize: 13,
    color: Colors.textSecondary,
  },
  specValue: {
    fontSize: 13,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  tweakRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 4,
  },
  tweakTextWrap: {
    flex: 1,
    marginRight: 10,
  },
  tweakTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  tweakSub: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
  },
});
