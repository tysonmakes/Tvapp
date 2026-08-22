import React, { useState } from 'react';
import { View, Text, StyleSheet, Modal, TouchableOpacity, TextInput, ActivityIndicator, Alert } from 'react-native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { AdbService } from '../services/AdbService';

interface InstallApkModalProps {
  visible: boolean;
  onClose: () => void;
}

export const InstallApkModal: React.FC<InstallApkModalProps> = ({ visible, onClose }) => {
  const [apkUrl, setApkUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('');

  const handleInstallFromUrl = async () => {
    if (!apkUrl.trim()) {
      Alert.alert('URL Required', 'Please enter a direct APK download link.');
      return;
    }

    setLoading(true);
    setStatus('Downloading and sideloading APK to TV via ADB...');
    try {
      await AdbService.runShellCommand(`curl -o /sdcard/Download/app_install.apk -L "${apkUrl}" && pm install -r /sdcard/Download/app_install.apk`);
      setStatus('Success! Application installed on TV.');
      setTimeout(() => {
        setLoading(false);
        onClose();
      }, 1200);
    } catch (e: any) {
      setStatus(`Installation failed: ${e.message}`);
      setLoading(false);
    }
  };

  const handleQuickPreset = (name: string, url: string) => {
    setApkUrl(url);
  };

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <View style={styles.header}>
            <MaterialCommunityIcons name="cellphone-arrow-down" size={24} color={Colors.accentGreen} />
            <Text style={styles.title}>Install APK on TV</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Ionicons name="close" size={22} color={Colors.textSecondary} />
            </TouchableOpacity>
          </View>

          <Text style={styles.description}>
            Sideload APK directly onto your Android TV or Fire TV device over Wi-Fi without flash drives.
          </Text>

          <TextInput
            style={styles.input}
            placeholder="https://example.com/app.apk"
            placeholderTextColor={Colors.textTertiary}
            value={apkUrl}
            onChangeText={setApkUrl}
            autoCapitalize="none"
            keyboardType="url"
          />

          <Text style={styles.presetTitle}>Popular TV Apps Presets:</Text>
          <View style={styles.presetsWrap}>
            <TouchableOpacity
              style={styles.presetChip}
              onPress={() => handleQuickPreset('SmartTube', 'https://github.com/yuliskov/SmartTube/releases/download/latest/smarttube_stable.apk')}
            >
              <Text style={styles.presetChipText}>SmartTube TV</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.presetChip}
              onPress={() => handleQuickPreset('Kodi', 'https://mirrors.kodi.tv/releases/android/arm/kodi-21.0-Omega-armeabi-v7a.apk')}
            >
              <Text style={styles.presetChipText}>Kodi 21</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.presetChip}
              onPress={() => handleQuickPreset('VLC', 'https://get.videolan.org/vlc-android/3.5.4/VLC-Android-3.5.4-arm64-v8a.apk')}
            >
              <Text style={styles.presetChipText}>VLC TV</Text>
            </TouchableOpacity>
          </View>

          {status ? <Text style={styles.statusText}>{status}</Text> : null}

          <TouchableOpacity
            style={[styles.installBtn, loading && styles.disabledBtn]}
            onPress={handleInstallFromUrl}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color="#ffffff" size="small" />
            ) : (
              <Text style={styles.installBtnText}>Install to TV</Text>
            )}
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    padding: 20,
  },
  card: {
    backgroundColor: '#ebf4eb',
    borderRadius: 20,
    padding: 20,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  title: {
    fontSize: 18,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginLeft: 8,
    flex: 1,
  },
  closeBtn: {
    padding: 4,
  },
  description: {
    fontSize: 13,
    color: Colors.textSecondary,
    marginBottom: 16,
    lineHeight: 18,
  },
  input: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 12,
    fontSize: 14,
    color: Colors.textPrimary,
    marginBottom: 14,
  },
  presetTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textSecondary,
    marginBottom: 8,
    textTransform: 'uppercase',
  },
  presetsWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 16,
  },
  presetChip: {
    backgroundColor: Colors.surfaceVariant,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 14,
    marginRight: 8,
    marginBottom: 6,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  presetChipText: {
    fontSize: 12,
    color: Colors.primaryDark,
    fontWeight: '600',
  },
  statusText: {
    fontSize: 12,
    color: Colors.accentGreen,
    fontWeight: '600',
    textAlign: 'center',
    marginBottom: 12,
  },
  installBtn: {
    backgroundColor: Colors.accentGreen,
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  disabledBtn: {
    opacity: 0.7,
  },
  installBtnText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '700',
  },
});
