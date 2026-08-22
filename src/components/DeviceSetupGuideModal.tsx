import React from 'react';
import { View, Text, StyleSheet, Modal, TouchableOpacity, ScrollView } from 'react-native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';

interface DeviceSetupGuideModalProps {
  visible: boolean;
  onClose: () => void;
}

export const DeviceSetupGuideModal: React.FC<DeviceSetupGuideModalProps> = ({ visible, onClose }) => {
  return (
    <Modal visible={visible} animationType="slide" transparent={false} onRequestClose={onClose}>
      <View style={styles.container}>
        <View style={styles.header}>
          <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
            <Ionicons name="close" size={24} color="#ffffff" />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>ADB Setup Guide</Text>
          <View style={{ width: 40 }} />
        </View>

        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.banner}>
            <MaterialCommunityIcons name="television-shimmer" size={32} color={Colors.primary} />
            <Text style={styles.bannerTitle}>How to connect to Android TV & Fire TV</Text>
            <Text style={styles.bannerSub}>Follow these simple 3 steps on your TV screen:</Text>
          </View>

          {/* Step 1 */}
          <View style={styles.stepCard}>
            <View style={styles.stepNumberBadge}>
              <Text style={styles.stepNumber}>1</Text>
            </View>
            <View style={styles.stepBody}>
              <Text style={styles.stepTitle}>Enable Developer Options</Text>
              <Text style={styles.stepText}>
                On TV, go to <Text style={styles.bold}>Settings → Device Preferences → About</Text>.
                Scroll down to <Text style={styles.bold}>Build</Text> and click OK on remote <Text style={styles.bold}>7 times</Text> until it says "You are now a developer!".
              </Text>
            </View>
          </View>

          {/* Step 2 */}
          <View style={styles.stepCard}>
            <View style={styles.stepNumberBadge}>
              <Text style={styles.stepNumber}>2</Text>
            </View>
            <View style={styles.stepBody}>
              <Text style={styles.stepTitle}>Turn on USB / Network Debugging</Text>
              <Text style={styles.stepText}>
                Go back to <Text style={styles.bold}>Settings → Developer Options</Text> and toggle <Text style={styles.bold}>USB Debugging</Text> (or Network Debugging) to <Text style={styles.bold}>ON</Text>.
              </Text>
            </View>
          </View>

          {/* Step 3 */}
          <View style={styles.stepCard}>
            <View style={styles.stepNumberBadge}>
              <Text style={styles.stepNumber}>3</Text>
            </View>
            <View style={styles.stepBody}>
              <Text style={styles.stepTitle}>Authorize Prompt on TV</Text>
              <Text style={styles.stepText}>
                Connect from this app. A prompt will appear on your TV: <Text style={styles.bold}>"Allow USB debugging?"</Text>. Check <Text style={styles.bold}>"Always allow from this computer"</Text> and click <Text style={styles.bold}>OK</Text>.
              </Text>
            </View>
          </View>

          {/* Fire TV Specifics */}
          <View style={styles.fireTvCard}>
            <Text style={styles.fireTvTitle}>For Amazon Fire TV Stick:</Text>
            <Text style={styles.fireTvText}>
              Go to <Text style={styles.bold}>Settings → My Fire TV → Developer Options → ADB Debugging: ON</Text>. (If Developer Options is hidden, click Fire TV Stick name 7 times in About).
            </Text>
          </View>

          <TouchableOpacity style={styles.gotItBtn} onPress={onClose}>
            <Text style={styles.gotItBtnText}>Got it, Let's Connect!</Text>
          </TouchableOpacity>
        </ScrollView>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    backgroundColor: Colors.primary,
    paddingTop: 45,
    paddingBottom: 14,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  closeBtn: {
    padding: 6,
  },
  headerTitle: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '700',
  },
  content: {
    padding: 16,
    paddingBottom: 36,
  },
  banner: {
    backgroundColor: Colors.surfaceVariant,
    padding: 16,
    borderRadius: 16,
    alignItems: 'center',
    marginBottom: 16,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  bannerTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginTop: 8,
    textAlign: 'center',
  },
  bannerSub: {
    fontSize: 13,
    color: Colors.textSecondary,
    marginTop: 4,
    textAlign: 'center',
  },
  stepCard: {
    flexDirection: 'row',
    backgroundColor: '#ffffff',
    borderRadius: 14,
    padding: 14,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: Colors.borderLight,
  },
  stepNumberBadge: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: Colors.primaryContainer,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  stepNumber: {
    color: Colors.onPrimaryContainer,
    fontWeight: '800',
    fontSize: 15,
  },
  stepBody: {
    flex: 1,
  },
  stepTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  stepText: {
    fontSize: 13,
    color: Colors.textSecondary,
    lineHeight: 18,
  },
  bold: {
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  fireTvCard: {
    backgroundColor: '#fffbeb',
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: '#fef3c7',
    marginVertical: 8,
  },
  fireTvTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#b45309',
    marginBottom: 4,
  },
  fireTvText: {
    fontSize: 12,
    color: '#92400e',
    lineHeight: 17,
  },
  gotItBtn: {
    backgroundColor: Colors.accentGreen,
    borderRadius: 14,
    paddingVertical: 14,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 16,
  },
  gotItBtnText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '700',
  },
});
