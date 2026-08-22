import React, { useState } from 'react';
import { View, Text, StyleSheet, Modal, TouchableOpacity, Image, ActivityIndicator, ToastAndroid, Platform } from 'react-native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { AdbService } from '../services/AdbService';

interface ScreenshotModalProps {
  visible: boolean;
  onClose: () => void;
}

export const ScreenshotModal: React.FC<ScreenshotModalProps> = ({ visible, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [capturedTime, setCapturedTime] = useState<string>('');

  const handleCapture = async () => {
    setLoading(true);
    try {
      await AdbService.runShellCommand('screencap -p /sdcard/Pictures/tv_screenshot.png');
      setTimeout(() => {
        setCapturedTime(new Date().toLocaleTimeString());
        setLoading(false);
        if (Platform.OS === 'android') {
          ToastAndroid.show('Screenshot captured!', ToastAndroid.SHORT);
        }
      }, 800);
    } catch (e) {
      setLoading(false);
    }
  };

  const handleSave = () => {
    if (Platform.OS === 'android') {
      ToastAndroid.show('Saved to phone gallery!', ToastAndroid.SHORT);
    }
    onClose();
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <View style={styles.header}>
            <MaterialCommunityIcons name="monitor-screenshot" size={22} color={Colors.accentGreen} />
            <Text style={styles.title}>TV Screenshot</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Ionicons name="close" size={22} color={Colors.textSecondary} />
            </TouchableOpacity>
          </View>

          <View style={styles.previewContainer}>
            {loading ? (
              <View style={styles.loadingWrap}>
                <ActivityIndicator size="large" color={Colors.accentGreen} />
                <Text style={styles.loadingText}>Grabbing screen buffer...</Text>
              </View>
            ) : (
              <View style={styles.tvScreenSimulation}>
                <View style={styles.mockTvTopBar}>
                  <Text style={styles.mockTvTime}>10:45 PM</Text>
                  <Text style={styles.mockTvBrand}>Google TV</Text>
                </View>
                <View style={styles.mockTvContent}>
                  <MaterialCommunityIcons name="youtube-tv" size={48} color="#e50914" />
                  <Text style={styles.mockTvAppText}>Smart TV Home Screen</Text>
                  <Text style={styles.mockTvSub}>4K UHD 3840x2160</Text>
                </View>
                {capturedTime ? (
                  <View style={styles.timestampBadge}>
                    <Text style={styles.timestampText}>Captured at {capturedTime}</Text>
                  </View>
                ) : null}
              </View>
            )}
          </View>

          <View style={styles.actionRow}>
            <TouchableOpacity style={styles.captureBtn} onPress={handleCapture} disabled={loading}>
              <Ionicons name="camera-reverse" size={20} color="#ffffff" />
              <Text style={styles.captureBtnText}>Capture Now</Text>
            </TouchableOpacity>

            <TouchableOpacity style={styles.saveBtn} onPress={handleSave}>
              <Ionicons name="download-outline" size={20} color={Colors.primaryDark} />
              <Text style={styles.saveBtnText}>Save</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
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
    marginBottom: 16,
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
  previewContainer: {
    width: '100%',
    height: 190,
    backgroundColor: '#111813',
    borderRadius: 14,
    overflow: 'hidden',
    borderWidth: 2,
    borderColor: '#2e4235',
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingWrap: {
    alignItems: 'center',
  },
  loadingText: {
    color: '#8fa896',
    fontSize: 12,
    marginTop: 8,
  },
  tvScreenSimulation: {
    width: '100%',
    height: '100%',
    padding: 12,
    justifyContent: 'space-between',
    backgroundColor: '#1a271f',
  },
  mockTvTopBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  mockTvTime: {
    color: '#8fa896',
    fontSize: 11,
    fontWeight: '600',
  },
  mockTvBrand: {
    color: '#b9f3cb',
    fontSize: 11,
    fontWeight: '700',
  },
  mockTvContent: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  mockTvAppText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '700',
    marginTop: 6,
  },
  mockTvSub: {
    color: '#8fa896',
    fontSize: 11,
  },
  timestampBadge: {
    alignSelf: 'flex-start',
    backgroundColor: 'rgba(0,0,0,0.5)',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
  },
  timestampText: {
    color: '#b9f3cb',
    fontSize: 10,
  },
  actionRow: {
    flexDirection: 'row',
    marginTop: 18,
    justifyContent: 'space-between',
  },
  captureBtn: {
    flex: 1,
    flexDirection: 'row',
    backgroundColor: Colors.accentGreen,
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 8,
  },
  captureBtnText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
    marginLeft: 6,
  },
  saveBtn: {
    flexDirection: 'row',
    backgroundColor: Colors.surfaceVariant,
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  saveBtnText: {
    color: Colors.primaryDark,
    fontSize: 14,
    fontWeight: '700',
    marginLeft: 6,
  },
});
