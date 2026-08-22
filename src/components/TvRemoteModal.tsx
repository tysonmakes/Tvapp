import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Modal,
  SafeAreaView,
  TextInput,
  ScrollView,
  ToastAndroid,
  Platform,
} from 'react-native';
import { Ionicons, MaterialCommunityIcons, Feather } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { AdbService } from '../services/AdbService';
import { DiscoveredDevice } from '../types';

interface TvRemoteModalProps {
  visible: boolean;
  onClose: () => void;
  device: DiscoveredDevice | null;
}

export const TvRemoteModal: React.FC<TvRemoteModalProps> = ({ visible, onClose, device }) => {
  const [activeTab, setActiveTab] = useState<'remote' | 'keyboard' | 'numpad'>('remote');
  const [inputText, setInputText] = useState('');
  const [lastAction, setLastAction] = useState<string>('Ready');

  const handleKey = async (key: string, label: string) => {
    setLastAction(`Pressed ${label}`);
    try {
      await AdbService.sendKeyEvent(key);
    } catch (e: any) {
      setLastAction(`Error: ${e.message}`);
    }
  };

  const handleSendText = async () => {
    if (!inputText.trim()) return;
    try {
      await AdbService.sendTextInput(inputText);
      if (Platform.OS === 'android') {
        ToastAndroid.show(`Sent: "${inputText}"`, ToastAndroid.SHORT);
      }
      setLastAction(`Typed "${inputText}"`);
      setInputText('');
    } catch (e: any) {
      setLastAction(`Error typing text`);
    }
  };

  const handleLaunchQuickApp = async (pkg: string, name: string) => {
    setLastAction(`Opening ${name}...`);
    try {
      await AdbService.launchApp(pkg);
    } catch (e: any) {
      setLastAction(`Error: ${e.message}`);
    }
  };

  return (
    <Modal visible={visible} animationType="slide" transparent={false} onRequestClose={onClose}>
      <SafeAreaView style={styles.container}>
        {/* Top bar */}
        <View style={styles.header}>
          <TouchableOpacity onPress={onClose} style={styles.headerBtn}>
            <Ionicons name="close" size={24} color="#ffffff" />
          </TouchableOpacity>
          <View style={styles.headerTitleContainer}>
            <Text style={styles.headerTitle}>{device?.name || 'TV Remote'}</Text>
            <Text style={styles.headerSubtitle}>{device?.ip || 'Connected via ADB'}</Text>
          </View>
          <TouchableOpacity 
            onPress={() => handleKey('power', 'Power')} 
            style={[styles.headerBtn, styles.powerBtn]}
          >
            <Ionicons name="power" size={20} color="#ff4d4f" />
          </TouchableOpacity>
        </View>

        {/* Tab switchers */}
        <View style={styles.modeTabs}>
          <TouchableOpacity
            style={[styles.modeTab, activeTab === 'remote' && styles.activeModeTab]}
            onPress={() => setActiveTab('remote')}
          >
            <MaterialCommunityIcons 
              name="remote" 
              size={18} 
              color={activeTab === 'remote' ? Colors.primaryDark : '#8fa896'} 
            />
            <Text style={[styles.modeTabText, activeTab === 'remote' && styles.activeModeTabText]}>
              Remote
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.modeTab, activeTab === 'keyboard' && styles.activeModeTab]}
            onPress={() => setActiveTab('keyboard')}
          >
            <Ionicons 
              name="keypad" 
              size={18} 
              color={activeTab === 'keyboard' ? Colors.primaryDark : '#8fa896'} 
            />
            <Text style={[styles.modeTabText, activeTab === 'keyboard' && styles.activeModeTabText]}>
              Keyboard
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.modeTab, activeTab === 'numpad' && styles.activeModeTab]}
            onPress={() => setActiveTab('numpad')}
          >
            <Feather 
              name="grid" 
              size={18} 
              color={activeTab === 'numpad' ? Colors.primaryDark : '#8fa896'} 
            />
            <Text style={[styles.modeTabText, activeTab === 'numpad' && styles.activeModeTabText]}>
              NumPad
            </Text>
          </TouchableOpacity>
        </View>

        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          {activeTab === 'remote' && (
            <View style={styles.remoteLayout}>
              {/* Quick top bar */}
              <View style={styles.topControlRow}>
                <TouchableOpacity style={styles.pillBtn} onPress={() => handleKey('tv_input', 'Input Source')}>
                  <MaterialCommunityIcons name="video-input-hdmi" size={18} color="#ffffff" />
                  <Text style={styles.pillBtnText}>Input</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.pillBtn} onPress={() => handleKey('mute', 'Mute')}>
                  <Ionicons name="volume-mute" size={18} color="#ffffff" />
                  <Text style={styles.pillBtnText}>Mute</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.pillBtn} onPress={() => handleKey('settings', 'Settings')}>
                  <Ionicons name="settings-sharp" size={18} color="#ffffff" />
                  <Text style={styles.pillBtnText}>Settings</Text>
                </TouchableOpacity>
              </View>

              {/* DPAD Controller */}
              <View style={styles.dpadContainer}>
                {/* UP */}
                <TouchableOpacity
                  style={[styles.dpadArrow, styles.dpadUp]}
                  onPress={() => handleKey('up', 'UP')}
                  activeOpacity={0.7}
                >
                  <Ionicons name="caret-up" size={32} color="#ffffff" />
                </TouchableOpacity>

                {/* LEFT */}
                <TouchableOpacity
                  style={[styles.dpadArrow, styles.dpadLeft]}
                  onPress={() => handleKey('left', 'LEFT')}
                  activeOpacity={0.7}
                >
                  <Ionicons name="caret-back" size={32} color="#ffffff" />
                </TouchableOpacity>

                {/* OK CENTER */}
                <TouchableOpacity
                  style={styles.dpadCenter}
                  onPress={() => handleKey('ok', 'OK')}
                  activeOpacity={0.7}
                >
                  <Text style={styles.dpadCenterText}>OK</Text>
                </TouchableOpacity>

                {/* RIGHT */}
                <TouchableOpacity
                  style={[styles.dpadArrow, styles.dpadRight]}
                  onPress={() => handleKey('right', 'RIGHT')}
                  activeOpacity={0.7}
                >
                  <Ionicons name="caret-forward" size={32} color="#ffffff" />
                </TouchableOpacity>

                {/* DOWN */}
                <TouchableOpacity
                  style={[styles.dpadArrow, styles.dpadDown]}
                  onPress={() => handleKey('down', 'DOWN')}
                  activeOpacity={0.7}
                >
                  <Ionicons name="caret-down" size={32} color="#ffffff" />
                </TouchableOpacity>
              </View>

              {/* Primary System Nav Buttons */}
              <View style={styles.systemNavRow}>
                <TouchableOpacity style={styles.roundActionBtn} onPress={() => handleKey('back', 'Back')}>
                  <Ionicons name="arrow-back" size={24} color="#ffffff" />
                  <Text style={styles.actionBtnLabel}>BACK</Text>
                </TouchableOpacity>

                <TouchableOpacity style={[styles.roundActionBtn, styles.homeActionBtn]} onPress={() => handleKey('home', 'Home')}>
                  <Ionicons name="home" size={26} color="#b9f3cb" />
                  <Text style={[styles.actionBtnLabel, { color: '#b9f3cb' }]}>HOME</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.roundActionBtn} onPress={() => handleKey('menu', 'Menu')}>
                  <MaterialCommunityIcons name="menu" size={24} color="#ffffff" />
                  <Text style={styles.actionBtnLabel}>MENU</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.roundActionBtn} onPress={() => handleKey('app_switch', 'Recents')}>
                  <MaterialCommunityIcons name="layers-outline" size={24} color="#ffffff" />
                  <Text style={styles.actionBtnLabel}>APPS</Text>
                </TouchableOpacity>
              </View>

              {/* Volume & Media playback */}
              <View style={styles.volumeAndMediaRow}>
                {/* Volume Rocker */}
                <View style={styles.rockerCard}>
                  <TouchableOpacity style={styles.rockerBtn} onPress={() => handleKey('volup', 'Vol +')}>
                    <Ionicons name="add" size={24} color="#ffffff" />
                  </TouchableOpacity>
                  <Text style={styles.rockerTitle}>VOL</Text>
                  <TouchableOpacity style={styles.rockerBtn} onPress={() => handleKey('voldown', 'Vol -')}>
                    <Ionicons name="remove" size={24} color="#ffffff" />
                  </TouchableOpacity>
                </View>

                {/* Media Playback Card */}
                <View style={styles.mediaBox}>
                  <View style={styles.mediaRow}>
                    <TouchableOpacity style={styles.mediaBtn} onPress={() => handleKey('rewind', 'Rewind')}>
                      <Ionicons name="play-back" size={20} color="#ffffff" />
                    </TouchableOpacity>
                    <TouchableOpacity style={[styles.mediaBtn, styles.playPauseBtn]} onPress={() => handleKey('play_pause', 'Play/Pause')}>
                      <Ionicons name="play" size={24} color="#08331a" />
                    </TouchableOpacity>
                    <TouchableOpacity style={styles.mediaBtn} onPress={() => handleKey('fast_forward', 'Forward')}>
                      <Ionicons name="play-forward" size={20} color="#ffffff" />
                    </TouchableOpacity>
                  </View>
                </View>
              </View>

              {/* Quick App Launchers */}
              <View style={styles.quickAppsSection}>
                <Text style={styles.quickAppsTitle}>Quick Launch</Text>
                <View style={styles.quickAppsRow}>
                  <TouchableOpacity 
                    style={[styles.quickAppBtn, { backgroundColor: '#e50914' }]}
                    onPress={() => handleLaunchQuickApp('com.netflix.ninja', 'Netflix')}
                  >
                    <Text style={styles.quickAppText}>NETFLIX</Text>
                  </TouchableOpacity>

                  <TouchableOpacity 
                    style={[styles.quickAppBtn, { backgroundColor: '#ff0000' }]}
                    onPress={() => handleLaunchQuickApp('com.google.android.youtube.tv', 'YouTube')}
                  >
                    <Text style={styles.quickAppText}>YouTube</Text>
                  </TouchableOpacity>

                  <TouchableOpacity 
                    style={[styles.quickAppBtn, { backgroundColor: '#00a8e1' }]}
                    onPress={() => handleLaunchQuickApp('com.amazon.amazonvideo.livingroom', 'Prime Video')}
                  >
                    <Text style={styles.quickAppText}>Prime</Text>
                  </TouchableOpacity>

                  <TouchableOpacity 
                    style={[styles.quickAppBtn, { backgroundColor: '#1db954' }]}
                    onPress={() => handleLaunchQuickApp('com.spotify.tv.android', 'Spotify')}
                  >
                    <Text style={styles.quickAppText}>Spotify</Text>
                  </TouchableOpacity>
                </View>
              </View>
            </View>
          )}

          {activeTab === 'keyboard' && (
            <View style={styles.keyboardContainer}>
              <Text style={styles.kbTitle}>Type on Remote TV</Text>
              <Text style={styles.kbSubtitle}>
                Type text here and hit send to quickly fill TV search bars, YouTube search, and login credentials.
              </Text>
              
              <View style={styles.inputWrap}>
                <TextInput
                  style={styles.textInput}
                  placeholder="Enter text to type on TV..."
                  placeholderTextColor="#7e9686"
                  value={inputText}
                  onChangeText={setInputText}
                  autoCapitalize="none"
                  returnKeyType="send"
                  onSubmitEditing={handleSendText}
                />
                <TouchableOpacity style={styles.sendBtn} onPress={handleSendText}>
                  <Ionicons name="send" size={20} color="#08331a" />
                </TouchableOpacity>
              </View>

              <View style={styles.quickTextRow}>
                {['https://', 'youtube.com', 'kodi', 'wifi password', 'search'].map((item) => (
                  <TouchableOpacity
                    key={item}
                    style={styles.quickChip}
                    onPress={() => setInputText(item)}
                  >
                    <Text style={styles.quickChipText}>{item}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              <View style={styles.keyboardActions}>
                <TouchableOpacity style={styles.kbActionBtn} onPress={() => handleKey('KEYCODE_DEL', 'Backspace')}>
                  <Ionicons name="backspace-outline" size={20} color="#ffffff" />
                  <Text style={styles.kbActionText}>Backspace</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.kbActionBtn} onPress={() => handleKey('KEYCODE_SPACE', 'Space')}>
                  <MaterialCommunityIcons name="keyboard-space" size={20} color="#ffffff" />
                  <Text style={styles.kbActionText}>Space</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.kbActionBtn} onPress={() => handleKey('KEYCODE_ENTER', 'Enter')}>
                  <Ionicons name="return-down-forward" size={20} color="#ffffff" />
                  <Text style={styles.kbActionText}>Enter</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}

          {activeTab === 'numpad' && (
            <View style={styles.numpadContainer}>
              <View style={styles.numpadGrid}>
                {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((num) => (
                  <TouchableOpacity
                    key={num}
                    style={styles.numBtn}
                    onPress={() => handleKey(`num${num}`, `${num}`)}
                  >
                    <Text style={styles.numBtnText}>{num}</Text>
                  </TouchableOpacity>
                ))}
                <TouchableOpacity
                  style={styles.numBtn}
                  onPress={() => handleKey('KEYCODE_DEL', 'Del')}
                >
                  <Ionicons name="backspace-outline" size={24} color="#ff7875" />
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.numBtn}
                  onPress={() => handleKey('num0', '0')}
                >
                  <Text style={styles.numBtnText}>0</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.numBtn}
                  onPress={() => handleKey('ok', 'Enter')}
                >
                  <Ionicons name="checkmark-circle-outline" size={26} color="#b9f3cb" />
                </TouchableOpacity>
              </View>
            </View>
          )}

          <View style={styles.statusBar}>
            <Text style={styles.statusText}>{lastAction}</Text>
          </View>
        </ScrollView>
      </SafeAreaView>
    </Modal>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f1612',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#1e2b22',
  },
  headerBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#1b281f',
    alignItems: 'center',
    justifyContent: 'center',
  },
  powerBtn: {
    backgroundColor: '#3b1717',
  },
  headerTitleContainer: {
    alignItems: 'center',
  },
  headerTitle: {
    color: '#ffffff',
    fontSize: 17,
    fontWeight: '700',
  },
  headerSubtitle: {
    color: '#8fa896',
    fontSize: 12,
    marginTop: 2,
  },
  modeTabs: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#141e17',
    borderBottomWidth: 1,
    borderBottomColor: '#1f2e23',
    justifyContent: 'space-around',
  },
  modeTab: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 6,
    paddingHorizontal: 16,
    borderRadius: 20,
  },
  activeModeTab: {
    backgroundColor: Colors.primaryContainer,
  },
  modeTabText: {
    color: '#8fa896',
    fontSize: 13,
    fontWeight: '600',
    marginLeft: 6,
  },
  activeModeTabText: {
    color: Colors.onPrimaryContainer,
    fontWeight: '700',
  },
  scrollContent: {
    padding: 16,
    alignItems: 'center',
    paddingBottom: 40,
  },
  remoteLayout: {
    width: '100%',
    alignItems: 'center',
  },
  topControlRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
    marginBottom: 20,
  },
  pillBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#1e2a22',
    paddingVertical: 10,
    marginHorizontal: 4,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#2d3e32',
  },
  pillBtnText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '600',
    marginLeft: 6,
  },
  dpadContainer: {
    width: 250,
    height: 250,
    borderRadius: 125,
    backgroundColor: '#1a251e',
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 10,
    borderWidth: 2,
    borderColor: '#2e4235',
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 8,
    position: 'relative',
  },
  dpadArrow: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
  dpadUp: {
    top: 10,
    width: 80,
    height: 60,
  },
  dpadDown: {
    bottom: 10,
    width: 80,
    height: 60,
  },
  dpadLeft: {
    left: 10,
    width: 60,
    height: 80,
  },
  dpadRight: {
    right: 10,
    width: 60,
    height: 80,
  },
  dpadCenter: {
    width: 90,
    height: 90,
    borderRadius: 45,
    backgroundColor: '#26372b',
    borderWidth: 2,
    borderColor: '#3c5744',
    alignItems: 'center',
    justifyContent: 'center',
  },
  dpadCenterText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '800',
  },
  systemNavRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
    marginTop: 20,
    paddingHorizontal: 8,
  },
  roundActionBtn: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#1d2a21',
    width: 68,
    height: 68,
    borderRadius: 34,
    borderWidth: 1,
    borderColor: '#2a3d30',
  },
  homeActionBtn: {
    backgroundColor: '#1b3f27',
    borderColor: '#27633b',
  },
  actionBtnLabel: {
    color: '#c2d6c7',
    fontSize: 10,
    fontWeight: '700',
    marginTop: 2,
  },
  volumeAndMediaRow: {
    flexDirection: 'row',
    width: '100%',
    marginTop: 20,
    justifyContent: 'space-between',
  },
  rockerCard: {
    width: '28%',
    backgroundColor: '#1b271f',
    borderRadius: 24,
    alignItems: 'center',
    paddingVertical: 10,
    borderWidth: 1,
    borderColor: '#2a3b2f',
  },
  rockerBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#25362b',
    alignItems: 'center',
    justifyContent: 'center',
  },
  rockerTitle: {
    color: '#8fa896',
    fontSize: 12,
    fontWeight: '700',
    marginVertical: 12,
  },
  mediaBox: {
    width: '68%',
    backgroundColor: '#1b271f',
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 12,
    borderWidth: 1,
    borderColor: '#2a3b2f',
  },
  mediaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
    width: '100%',
  },
  mediaBtn: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#25362b',
    alignItems: 'center',
    justifyContent: 'center',
  },
  playPauseBtn: {
    width: 58,
    height: 58,
    borderRadius: 29,
    backgroundColor: '#b9f3cb',
  },
  quickAppsSection: {
    width: '100%',
    marginTop: 24,
  },
  quickAppsTitle: {
    color: '#8fa896',
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.5,
    marginBottom: 10,
    textTransform: 'uppercase',
  },
  quickAppsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  quickAppBtn: {
    flex: 1,
    paddingVertical: 12,
    marginHorizontal: 3,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  quickAppText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '800',
  },
  keyboardContainer: {
    width: '100%',
    paddingTop: 10,
  },
  kbTitle: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '700',
  },
  kbSubtitle: {
    color: '#8fa896',
    fontSize: 13,
    marginTop: 4,
    lineHeight: 18,
  },
  inputWrap: {
    flexDirection: 'row',
    marginTop: 18,
    backgroundColor: '#1b271f',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#2c3e32',
    paddingHorizontal: 12,
    alignItems: 'center',
  },
  textInput: {
    flex: 1,
    color: '#ffffff',
    paddingVertical: 14,
    fontSize: 15,
  },
  sendBtn: {
    backgroundColor: '#b9f3cb',
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  quickTextRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 16,
  },
  quickChip: {
    backgroundColor: '#1f2e24',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    marginRight: 8,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#2e4435',
  },
  quickChipText: {
    color: '#b9f3cb',
    fontSize: 12,
  },
  keyboardActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 24,
  },
  kbActionBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#1b271f',
    paddingVertical: 14,
    marginHorizontal: 4,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#2c3e32',
  },
  kbActionText: {
    color: '#ffffff',
    fontSize: 13,
    fontWeight: '600',
    marginLeft: 6,
  },
  numpadContainer: {
    width: '100%',
    alignItems: 'center',
    paddingTop: 10,
  },
  numpadGrid: {
    width: 280,
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  numBtn: {
    width: 80,
    height: 70,
    backgroundColor: '#1b271f',
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
    borderWidth: 1,
    borderColor: '#2c3e32',
  },
  numBtnText: {
    color: '#ffffff',
    fontSize: 24,
    fontWeight: '700',
  },
  statusBar: {
    marginTop: 24,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 12,
    backgroundColor: '#152119',
    borderWidth: 1,
    borderColor: '#233327',
  },
  statusText: {
    color: '#8fa896',
    fontSize: 12,
    textAlign: 'center',
  },
});
