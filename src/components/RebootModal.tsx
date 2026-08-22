import React from 'react';
import { View, Text, StyleSheet, Modal, TouchableOpacity, Alert, ToastAndroid, Platform } from 'react-native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { AdbService } from '../services/AdbService';

interface RebootModalProps {
  visible: boolean;
  onClose: () => void;
  mode: 'reboot' | 'power';
}

export const RebootModal: React.FC<RebootModalProps> = ({ visible, onClose, mode }) => {
  const showToast = (msg: string) => {
    if (Platform.OS === 'android') {
      ToastAndroid.show(msg, ToastAndroid.SHORT);
    }
  };

  const handleAction = async (actionType: 'normal' | 'recovery' | 'bootloader' | 'soft' | 'poweroff' | 'sleep') => {
    onClose();
    if (actionType === 'poweroff') {
      await AdbService.powerOff();
      showToast('Power off signal sent to TV');
    } else if (actionType === 'sleep') {
      await AdbService.sendKeyEvent('sleep');
      showToast('Put TV into sleep mode');
    } else {
      await AdbService.reboot(actionType);
      showToast(`Rebooting TV (${actionType})...`);
    }
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <View style={styles.header}>
            <MaterialCommunityIcons 
              name={mode === 'reboot' ? 'restart' : 'power'} 
              size={24} 
              color={mode === 'reboot' ? Colors.warning : Colors.danger} 
            />
            <Text style={styles.title}>{mode === 'reboot' ? 'Reboot Options' : 'Power Options'}</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Ionicons name="close" size={22} color={Colors.textSecondary} />
            </TouchableOpacity>
          </View>

          {mode === 'reboot' ? (
            <View style={styles.optionsList}>
              <TouchableOpacity style={styles.optionBtn} onPress={() => handleAction('normal')}>
                <MaterialCommunityIcons name="restart" size={20} color={Colors.accentGreen} />
                <View style={styles.optionTextWrap}>
                  <Text style={styles.optionTitle}>Standard Reboot</Text>
                  <Text style={styles.optionDesc}>Clean restart of system OS</Text>
                </View>
              </TouchableOpacity>

              <TouchableOpacity style={styles.optionBtn} onPress={() => handleAction('soft')}>
                <MaterialCommunityIcons name="lightning-bolt" size={20} color="#0288d1" />
                <View style={styles.optionTextWrap}>
                  <Text style={styles.optionTitle}>Soft Reboot (Fast)</Text>
                  <Text style={styles.optionDesc}>Restart Android UI runtime only</Text>
                </View>
              </TouchableOpacity>

              <TouchableOpacity style={styles.optionBtn} onPress={() => handleAction('recovery')}>
                <MaterialCommunityIcons name="shield-refresh-outline" size={20} color={Colors.warning} />
                <View style={styles.optionTextWrap}>
                  <Text style={styles.optionTitle}>Reboot to Recovery</Text>
                  <Text style={styles.optionDesc}>Boot into system recovery menu</Text>
                </View>
              </TouchableOpacity>
            </View>
          ) : (
            <View style={styles.optionsList}>
              <TouchableOpacity style={styles.optionBtn} onPress={() => handleAction('poweroff')}>
                <MaterialCommunityIcons name="power-settings" size={20} color={Colors.danger} />
                <View style={styles.optionTextWrap}>
                  <Text style={[styles.optionTitle, { color: Colors.danger }]}>Power Off TV</Text>
                  <Text style={styles.optionDesc}>Completely shut down the device</Text>
                </View>
              </TouchableOpacity>

              <TouchableOpacity style={styles.optionBtn} onPress={() => handleAction('sleep')}>
                <MaterialCommunityIcons name="power-sleep" size={20} color="#0288d1" />
                <View style={styles.optionTextWrap}>
                  <Text style={styles.optionTitle}>Sleep / Standby</Text>
                  <Text style={styles.optionDesc}>Turn off screen into ambient standby</Text>
                </View>
              </TouchableOpacity>
            </View>
          )}
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
  optionsList: {
    gap: 10,
  },
  optionBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ffffff',
    padding: 14,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.borderLight,
  },
  optionTextWrap: {
    marginLeft: 12,
    flex: 1,
  },
  optionTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  optionDesc: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
  },
});
