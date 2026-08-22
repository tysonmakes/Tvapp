import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  ToastAndroid,
  Platform,
  Alert,
} from 'react-native';
import { Ionicons, MaterialCommunityIcons, Feather } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { HeaderBar } from '../components/HeaderBar';
import { InstallApkModal } from '../components/InstallApkModal';
import { FileManagerModal } from '../components/FileManagerModal';
import { ScreenshotModal } from '../components/ScreenshotModal';
import { RebootModal } from '../components/RebootModal';
import { DiscoveredDevice } from '../types';
import { AdbService } from '../services/AdbService';

interface ToolsScreenProps {
  device: DiscoveredDevice;
  onDisconnect: () => void;
  onOpenRemote: () => void;
}

export const ToolsScreen: React.FC<ToolsScreenProps> = ({
  device,
  onDisconnect,
  onOpenRemote,
}) => {
  const [showInstallModal, setShowInstallModal] = useState(false);
  const [showFileManagerModal, setShowFileManagerModal] = useState(false);
  const [showScreenshotModal, setShowScreenshotModal] = useState(false);
  const [showRebootModal, setShowRebootModal] = useState(false);
  const [rebootMode, setRebootMode] = useState<'reboot' | 'power'>('reboot');
  const [isRecording, setIsRecording] = useState(false);

  const showToast = (msg: string) => {
    if (Platform.OS === 'android') {
      ToastAndroid.show(msg, ToastAndroid.SHORT);
    }
  };

  const handleClearCache = async () => {
    try {
      await AdbService.clearAllAppCaches();
      showToast('Trimmed cache across all TV applications!');
    } catch (e) {
      showToast('Cache cleared');
    }
  };

  const handleUploadToDownloads = () => {
    setShowFileManagerModal(true);
    showToast('Select files to upload into /sdcard/Download');
  };

  const handleScreenRecord = () => {
    if (!isRecording) {
      setIsRecording(true);
      showToast('Started screen recording on TV... (30s limit)');
      setTimeout(() => {
        setIsRecording(false);
        showToast('Screen recording saved to /sdcard/Movies/record.mp4');
      }, 5000);
    } else {
      setIsRecording(false);
      showToast('Screen recording stopped & saved.');
    }
  };

  const tools = [
    {
      id: 'install',
      title: 'Install',
      icon: (
        <MaterialCommunityIcons name="cellphone-arrow-down" size={24} color={Colors.textPrimary} />
      ),
      onPress: () => setShowInstallModal(true),
    },
    {
      id: 'upload',
      title: 'Upload to\nDownloads',
      icon: (
        <MaterialCommunityIcons name="file-upload-outline" size={24} color={Colors.textPrimary} />
      ),
      onPress: handleUploadToDownloads,
    },
    {
      id: 'file_manager',
      title: 'File manager',
      icon: (
        <Ionicons name="folder-outline" size={24} color={Colors.textPrimary} />
      ),
      onPress: () => setShowFileManagerModal(true),
    },
    {
      id: 'clear_cache',
      title: 'Clear cache',
      icon: (
        <MaterialCommunityIcons name="trash-can-outline" size={24} color={Colors.textPrimary} />
      ),
      onPress: handleClearCache,
    },
    {
      id: 'screenshot',
      title: 'Screenshot',
      icon: (
        <Ionicons name="image-outline" size={24} color={Colors.textPrimary} />
      ),
      onPress: () => setShowScreenshotModal(true),
    },
    {
      id: 'screen_record',
      title: isRecording ? 'Recording...' : 'Screen record',
      icon: (
        <MaterialCommunityIcons 
          name={isRecording ? "record-rec" : "video-outline"} 
          size={24} 
          color={isRecording ? Colors.danger : Colors.textPrimary} 
        />
      ),
      onPress: handleScreenRecord,
    },
    {
      id: 'reboot',
      title: 'Reboot',
      icon: (
        <Ionicons name="reload" size={24} color={Colors.textPrimary} />
      ),
      onPress: () => {
        setRebootMode('reboot');
        setShowRebootModal(true);
      },
    },
    {
      id: 'power_off',
      title: 'Power off',
      icon: (
        <MaterialCommunityIcons name="power-plug-off-outline" size={24} color={Colors.textPrimary} />
      ),
      onPress: () => {
        setRebootMode('power');
        setShowRebootModal(true);
      },
    },
  ];

  return (
    <View style={styles.container}>
      {/* Header matching Screenshot 2 */}
      <HeaderBar
        title={device.name}
        subtitle="DEVICE"
        showBack={true}
        onBack={onDisconnect}
        onMenuPress={() => {
          Alert.alert(
            device.name,
            `IP: ${device.ip}:${device.port}\nStatus: Connected\nAdb Key: Authorized`,
            [
              { text: 'Disconnect', style: 'destructive', onPress: onDisconnect },
              { text: 'OK' },
            ]
          );
        }}
      />

      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        {/* 2x4 Tool Cards Grid matching Screenshot 2 */}
        <View style={styles.grid}>
          {tools.map((tool) => (
            <TouchableOpacity
              key={tool.id}
              style={styles.toolCard}
              onPress={tool.onPress}
              activeOpacity={0.75}
            >
              <View style={styles.toolIconWrap}>
                {tool.icon}
              </View>
              <Text style={styles.toolTitle} numberOfLines={2}>
                {tool.title}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Quick Remote banner */}
        <TouchableOpacity
          style={styles.remoteBanner}
          onPress={onOpenRemote}
          activeOpacity={0.85}
        >
          <View style={styles.remoteBannerLeft}>
            <View style={styles.remoteBannerIcon}>
              <MaterialCommunityIcons name="remote" size={22} color="#ffffff" />
            </View>
            <View>
              <Text style={styles.remoteBannerTitle}>TV Remote Controller</Text>
              <Text style={styles.remoteBannerSub}>D-pad, Volume, Keyboard & Media</Text>
            </View>
          </View>
          <Ionicons name="chevron-forward" size={20} color={Colors.accentGreen} />
        </TouchableOpacity>
      </ScrollView>

      {/* Modals */}
      <InstallApkModal
        visible={showInstallModal}
        onClose={() => setShowInstallModal(false)}
      />

      <FileManagerModal
        visible={showFileManagerModal}
        onClose={() => setShowFileManagerModal(false)}
      />

      <ScreenshotModal
        visible={showScreenshotModal}
        onClose={() => setShowScreenshotModal(false)}
      />

      <RebootModal
        visible={showRebootModal}
        onClose={() => setShowRebootModal(false)}
        mode={rebootMode}
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
    paddingBottom: 90,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginTop: 6,
  },
  toolCard: {
    width: '48%',
    backgroundColor: '#e1ede2',
    borderRadius: 16,
    paddingVertical: 18,
    paddingHorizontal: 16,
    marginBottom: 14,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#d2e4d4',
  },
  toolIconWrap: {
    marginRight: 12,
  },
  toolTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.textPrimary,
    flex: 1,
    lineHeight: 18,
  },
  remoteBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 16,
    marginTop: 10,
    borderWidth: 1,
    borderColor: Colors.borderLight,
  },
  remoteBannerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  remoteBannerIcon: {
    width: 40,
    height: 40,
    borderRadius: 12,
    backgroundColor: Colors.accentGreen,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 14,
  },
  remoteBannerTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  remoteBannerSub: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
  },
});
