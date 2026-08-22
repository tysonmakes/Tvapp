import React from 'react';
import { View, Text, StyleSheet, Modal, TouchableOpacity, Alert, ToastAndroid, Platform } from 'react-native';
import { Ionicons, MaterialCommunityIcons, Feather } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { TVApp } from '../types';
import { AdbService } from '../services/AdbService';

interface AppActionModalProps {
  visible: boolean;
  app: TVApp | null;
  onClose: () => void;
  onAppUpdated: (updatedApp: TVApp) => void;
}

export const AppActionModal: React.FC<AppActionModalProps> = ({
  visible,
  app,
  onClose,
  onAppUpdated,
}) => {
  if (!app) return null;

  const showToast = (msg: string) => {
    if (Platform.OS === 'android') {
      ToastAndroid.show(msg, ToastAndroid.SHORT);
    }
  };

  const handleLaunch = async () => {
    onClose();
    await AdbService.launchApp(app.packageName);
    showToast(`Launching ${app.name}`);
  };

  const handleForceStop = async () => {
    onClose();
    await AdbService.forceStopApp(app.packageName);
    onAppUpdated({ ...app, isRunning: false });
    showToast(`Force stopped ${app.name}`);
  };

  const handleClearData = async () => {
    Alert.alert(
      'Clear App Data',
      `Are you sure you want to clear all data and cache for ${app.name}?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Clear Data',
          style: 'destructive',
          onPress: async () => {
            onClose();
            await AdbService.clearAppData(app.packageName);
            showToast(`Data cleared for ${app.name}`);
          },
        },
      ]
    );
  };

  const handleToggleDisable = async () => {
    const newState = !app.isEnabled;
    onClose();
    await AdbService.setAppEnabled(app.packageName, newState);
    onAppUpdated({ ...app, isEnabled: newState });
    showToast(`${app.name} is now ${newState ? 'Enabled' : 'Disabled'}`);
  };

  const handleUninstall = async () => {
    Alert.alert(
      'Uninstall App',
      `Do you want to uninstall ${app.name} (${app.packageName}) from the TV?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Uninstall',
          style: 'destructive',
          onPress: async () => {
            onClose();
            await AdbService.uninstallApp(app.packageName);
            showToast(`Uninstalled ${app.name}`);
          },
        },
      ]
    );
  };

  const handleDownloadApk = async () => {
    onClose();
    await AdbService.runShellCommand(`pm path ${app.packageName}`);
    showToast(`Extracting APK for ${app.name}... Downloaded to /sdcard/Download`);
  };

  const handlePermissions = async () => {
    onClose();
    await AdbService.runShellCommand(`dumpsys package ${app.packageName} | grep permission`);
    showToast(`Inspected permissions for ${app.name}`);
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <TouchableOpacity style={styles.backdrop} activeOpacity={1} onPress={onClose}>
        <View style={styles.menuContainer}>
          {/* Header of Popup */}
          <View style={styles.menuHeader}>
            <View style={[styles.appIconPill, { backgroundColor: app.iconBg }]}>
              <MaterialCommunityIcons name={app.iconType as any || 'application'} size={18} color="#ffffff" />
            </View>
            <View style={styles.headerTextWrap}>
              <Text style={styles.menuAppName} numberOfLines={1}>{app.name}</Text>
              <Text style={styles.menuPkgName} numberOfLines={1}>{app.packageName}</Text>
            </View>
          </View>

          <View style={styles.divider} />

          {/* Action List matching Screenshot 3 */}
          <TouchableOpacity style={styles.menuItem} onPress={handleLaunch}>
            <Ionicons name="play" size={18} color={Colors.accentGreen} />
            <Text style={styles.menuItemText}>Launch</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.menuItem} onPress={handleUninstall}>
            <MaterialCommunityIcons name="delete-outline" size={18} color={Colors.danger} />
            <Text style={[styles.menuItemText, { color: Colors.danger }]}>Uninstall</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.menuItem} onPress={handleDownloadApk}>
            <Feather name="download" size={18} color={Colors.textPrimary} />
            <Text style={styles.menuItemText}>Download APK</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.menuItem} onPress={handleForceStop}>
            <MaterialCommunityIcons name="close" size={18} color={Colors.textPrimary} />
            <Text style={styles.menuItemText}>Force stop</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.menuItem} onPress={handleClearData}>
            <MaterialCommunityIcons name="broom" size={18} color={Colors.textPrimary} />
            <Text style={styles.menuItemText}>Clear data</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.menuItem} onPress={handlePermissions}>
            <MaterialCommunityIcons name="shield-check-outline" size={18} color={Colors.textPrimary} />
            <Text style={styles.menuItemText}>Permissions</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.menuItem} onPress={handleToggleDisable}>
            <MaterialCommunityIcons 
              name={app.isEnabled ? 'toggle-switch' : 'toggle-switch-off-outline'} 
              size={20} 
              color={app.isEnabled ? Colors.accentGreen : Colors.textSecondary} 
            />
            <Text style={styles.menuItemText}>{app.isEnabled ? 'Disable' : 'Enable'}</Text>
          </TouchableOpacity>
        </View>
      </TouchableOpacity>
    </Modal>
  );
};

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  menuContainer: {
    width: 250,
    backgroundColor: '#ebf4eb',
    borderRadius: 16,
    paddingVertical: 10,
    paddingHorizontal: 6,
    elevation: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  menuHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  appIconPill: {
    width: 28,
    height: 28,
    borderRadius: 7,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
  },
  headerTextWrap: {
    flex: 1,
  },
  menuAppName: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  menuPkgName: {
    fontSize: 10,
    color: Colors.textSecondary,
  },
  divider: {
    height: 1,
    backgroundColor: Colors.border,
    marginVertical: 6,
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 14,
    borderRadius: 10,
  },
  menuItemText: {
    fontSize: 14,
    color: Colors.textPrimary,
    fontWeight: '600',
    marginLeft: 12,
  },
});
