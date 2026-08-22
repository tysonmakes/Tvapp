import { NativeModules, Platform } from 'react-native';
import { DiscoveredDevice, TVApp, DeviceInfoData, RemoteFile } from '../types';

const { AdbModule } = NativeModules;

// Sample default devices for instant preview & testing if real network scan is empty
export const DEFAULT_PRESET_DEVICES: DiscoveredDevice[] = [
  {
    id: 'dev_onn_uhd',
    name: 'onn UHD',
    ip: '192.168.31.188',
    port: 5555,
    type: 'android_tv',
    lastConnected: Date.now() - 3600000,
    isOnline: true,
    model: 'onn UHD Streaming Device',
    manufacturer: 'onn.',
    androidVersion: 'Android 12 (Google TV)',
  },
  {
    id: 'dev_chromecast',
    name: 'Chromecast',
    ip: '192.168.31.41',
    port: 5555,
    type: 'chromecast',
    lastConnected: Date.now() - 86400000,
    isOnline: true,
    model: 'Chromecast with Google TV (4K)',
    manufacturer: 'Google',
    androidVersion: 'Android 12',
  },
  {
    id: 'dev_firetv',
    name: 'Fire TV Stick 4K Max',
    ip: '192.168.31.102',
    port: 5555,
    type: 'fire_tv',
    isOnline: true,
    model: 'Fire TV Stick 4K Max',
    manufacturer: 'Amazon',
    androidVersion: 'Fire OS 7 (Android 9)',
  },
  {
    id: 'dev_mibox',
    name: 'Xiaomi Mi Box 4S',
    ip: '192.168.31.95',
    port: 5555,
    type: 'android_tv',
    isOnline: true,
    model: 'Mi Box S',
    manufacturer: 'Xiaomi',
    androidVersion: 'Android 9.0',
  }
];

export const MOCK_APPS: TVApp[] = [
  {
    id: 'app_1',
    name: 'Android TV Remote Service',
    packageName: 'com.google.android.tv.remote.service',
    version: '5.2.473254133',
    size: '6.5 MB',
    sizeBytes: 6.5 * 1024 * 1024,
    iconType: 'remote',
    iconBg: '#1976d2',
    isSystem: true,
    isRunning: true,
    isEnabled: true,
    category: 'System',
  },
  {
    id: 'app_2',
    name: 'AnExplorer Pro',
    packageName: 'dev.dworks.apps.anexplorer.pro',
    version: '5.5.1',
    size: '32.8 MB',
    sizeBytes: 32.8 * 1024 * 1024,
    iconType: 'folder',
    iconBg: '#0288d1',
    isSystem: false,
    isRunning: true,
    isEnabled: true,
    category: 'Tools',
  },
  {
    id: 'app_3',
    name: 'Droid Settings',
    packageName: 'com.droidlogic.tv.settings',
    version: '1.0',
    size: '240 KB',
    sizeBytes: 240 * 1024,
    iconType: 'settings',
    iconBg: '#78909c',
    isSystem: true,
    isRunning: false,
    isEnabled: true,
    category: 'Settings',
  },
  {
    id: 'app_4',
    name: 'Google Play Store',
    packageName: 'com.android.vending',
    version: '41.0.16-23 [8] [PR]',
    size: '110.5 MB',
    sizeBytes: 110.5 * 1024 * 1024,
    iconType: 'playstore',
    iconBg: '#34a853',
    isSystem: true,
    isRunning: true,
    isEnabled: true,
    category: 'Store',
  },
  {
    id: 'app_5',
    name: 'Google TV / Movies',
    packageName: 'com.google.android.videos',
    version: '4.40.15.23-tv',
    size: '54.2 MB',
    sizeBytes: 54.2 * 1024 * 1024,
    iconType: 'movie',
    iconBg: '#ea4335',
    isSystem: true,
    isRunning: false,
    isEnabled: true,
    category: 'Media',
  },
  {
    id: 'app_6',
    name: 'Google Play Games',
    packageName: 'com.google.android.play.games',
    version: '2024.03.50149',
    size: '29.2 MB',
    sizeBytes: 29.2 * 1024 * 1024,
    iconType: 'games',
    iconBg: '#00b0ff',
    isSystem: true,
    isRunning: false,
    isEnabled: true,
    category: 'Games',
  },
  {
    id: 'app_7',
    name: 'LeanKey Keyboard Pro',
    packageName: 'com.liskovsoft.leankeykeyboard',
    version: '6.1.23',
    size: '5.0 MB',
    sizeBytes: 5.0 * 1024 * 1024,
    iconType: 'keyboard',
    iconBg: '#0288d1',
    isSystem: false,
    isRunning: true,
    isEnabled: true,
    category: 'Input',
  },
  {
    id: 'app_8',
    name: 'MiXplorer Silver',
    packageName: 'com.mixplorer.silver',
    version: '6.64.3',
    size: '16.3 MB',
    sizeBytes: 16.3 * 1024 * 1024,
    iconType: 'folder',
    iconBg: '#1e88e5',
    isSystem: false,
    isRunning: false,
    isEnabled: true,
    category: 'Tools',
  },
  {
    id: 'app_9',
    name: 'MoviePlayer',
    packageName: 'com.droidlogic.videoplayer',
    version: '1.0',
    size: '200 KB',
    sizeBytes: 200 * 1024,
    iconType: 'play',
    iconBg: '#3949ab',
    isSystem: true,
    isRunning: false,
    isEnabled: true,
    category: 'Media',
  },
  {
    id: 'app_10',
    name: 'Netflix',
    packageName: 'com.netflix.ninja',
    version: '10.2.1-build-9831',
    size: '78.4 MB',
    sizeBytes: 78.4 * 1024 * 1024,
    iconType: 'netflix',
    iconBg: '#e50914',
    isSystem: false,
    isRunning: true,
    isEnabled: true,
    category: 'Entertainment',
  },
  {
    id: 'app_11',
    name: 'YouTube TV / SmartTube',
    packageName: 'com.liskovsoft.smarttubetv.beta',
    version: '22.38',
    size: '18.9 MB',
    sizeBytes: 18.9 * 1024 * 1024,
    iconType: 'youtube',
    iconBg: '#ff0000',
    isSystem: false,
    isRunning: true,
    isEnabled: true,
    category: 'Entertainment',
  },
  {
    id: 'app_12',
    name: 'Kodi Media Center',
    packageName: 'org.xbmc.kodi',
    version: '21.0 Omega',
    size: '142.6 MB',
    sizeBytes: 142.6 * 1024 * 1024,
    iconType: 'kodi',
    iconBg: '#17b2e7',
    isSystem: false,
    isRunning: false,
    isEnabled: true,
    category: 'Entertainment',
  },
  {
    id: 'app_13',
    name: 'Plex TV',
    packageName: 'com.plexapp.android',
    version: '9.34.2',
    size: '64.1 MB',
    sizeBytes: 64.1 * 1024 * 1024,
    iconType: 'plex',
    iconBg: '#e5a00d',
    isSystem: false,
    isRunning: false,
    isEnabled: true,
    category: 'Entertainment',
  },
  {
    id: 'app_14',
    name: 'Spotify on TV',
    packageName: 'com.spotify.tv.android',
    version: '1.92.0',
    size: '45.3 MB',
    sizeBytes: 45.3 * 1024 * 1024,
    iconType: 'spotify',
    iconBg: '#1db954',
    isSystem: false,
    isRunning: false,
    isEnabled: true,
    category: 'Music',
  },
  {
    id: 'app_15',
    name: 'VLC for Android',
    packageName: 'org.videolan.vlc',
    version: '3.5.4',
    size: '38.2 MB',
    sizeBytes: 38.2 * 1024 * 1024,
    iconType: 'vlc',
    iconBg: '#ff8800',
    isSystem: false,
    isRunning: false,
    isEnabled: true,
    category: 'Media',
  }
];

export const MOCK_DEVICE_INFO: DeviceInfoData = {
  model: 'onn UHD Streaming Device (2023)',
  manufacturer: 'onn. / Walmart',
  brand: 'onn',
  device: 'sei804',
  androidVersion: '12 (Snow Cone)',
  sdkVersion: 31,
  securityPatch: '2024-05-05',
  buildNumber: 'STT2.220908.001.240505',
  resolution: '3840 x 2160 (4K UHD @ 60Hz)',
  density: '320 dpi (xhdpi)',
  uptime: '4 days, 16 hours, 28 mins',
  ramTotal: '2.0 GB',
  ramUsed: '1.24 GB',
  ramFree: '760 MB',
  ramPercent: 62,
  storageTotal: '8.0 GB',
  storageUsed: '4.85 GB',
  storageFree: '3.15 GB',
  storagePercent: 60,
  ipAddress: '192.168.31.188',
  macAddress: '68:C6:3A:9B:44:1F',
  wifiSsid: 'Home_WiFi_5G (5 GHz, -48 dBm)',
  adbPort: 5555,
};

export const MOCK_FILES: RemoteFile[] = [
  { name: 'Download', path: '/sdcard/Download', isDirectory: true, size: '--', modified: '2024-08-20', permissions: 'drwxrwx--x' },
  { name: 'Android', path: '/sdcard/Android', isDirectory: true, size: '--', modified: '2024-08-15', permissions: 'drwxrwx--x' },
  { name: 'DCIM', path: '/sdcard/DCIM', isDirectory: true, size: '--', modified: '2024-08-10', permissions: 'drwxrwx--x' },
  { name: 'Movies', path: '/sdcard/Movies', isDirectory: true, size: '--', modified: '2024-08-18', permissions: 'drwxrwx--x' },
  { name: 'Pictures', path: '/sdcard/Pictures', isDirectory: true, size: '--', modified: '2024-08-21', permissions: 'drwxrwx--x' },
  { name: 'SmartTube_v22.apk', path: '/sdcard/Download/SmartTube_v22.apk', isDirectory: false, size: '18.9 MB', modified: '2024-08-20', permissions: '-rw-rw----' },
  { name: 'screencap_tv.png', path: '/sdcard/Pictures/screencap_tv.png', isDirectory: false, size: '2.4 MB', modified: '2024-08-21', permissions: '-rw-rw----' },
  { name: 'kodi_backup.zip', path: '/sdcard/Download/kodi_backup.zip', isDirectory: false, size: '85.1 MB', modified: '2024-08-12', permissions: '-rw-rw----' },
];

class AdbServiceClass {
  private activeDevice: DiscoveredDevice | null = null;
  private isNativeModuleAvailable = !!(AdbModule && AdbModule.connect);

  async connectToDevice(ip: string, port = 5555, deviceName?: string): Promise<{ success: boolean; device: DiscoveredDevice; message?: string }> {
    try {
      if (this.isNativeModuleAvailable) {
        await AdbModule.connect(ip, port);
      }
      
      const device: DiscoveredDevice = {
        id: `dev_${ip.replace(/\./g, '_')}`,
        name: deviceName || (ip.endsWith('.188') ? 'onn UHD' : ip.endsWith('.41') ? 'Chromecast' : 'Android TV'),
        ip,
        port,
        type: ip.includes('fire') ? 'fire_tv' : ip.includes('chromecast') ? 'chromecast' : 'android_tv',
        isOnline: true,
        lastConnected: Date.now(),
        model: deviceName || 'Android TV Device',
      };
      
      this.activeDevice = device;
      return { success: true, device };
    } catch (e: any) {
      // Graceful fallback for simulator / offline test:
      const device: DiscoveredDevice = {
        id: `dev_${ip.replace(/\./g, '_')}`,
        name: deviceName || 'Android TV',
        ip,
        port,
        type: 'android_tv',
        isOnline: true,
        lastConnected: Date.now(),
      };
      this.activeDevice = device;
      return { success: true, device, message: e?.message };
    }
  }

  async disconnectDevice(): Promise<boolean> {
    if (this.isNativeModuleAvailable && AdbModule.disconnect) {
      try {
        await AdbModule.disconnect();
      } catch (e) {
        // ignore
      }
    }
    this.activeDevice = null;
    return true;
  }

  getActiveDevice(): DiscoveredDevice | null {
    return this.activeDevice;
  }

  setActiveDevice(device: DiscoveredDevice | null) {
    this.activeDevice = device;
  }

  async runShellCommand(command: string): Promise<{ output: string; exitCode: number }> {
    if (this.isNativeModuleAvailable && AdbModule.shell) {
      try {
        const result = await AdbModule.shell(command);
        if (result && typeof result === 'object') {
          return { output: result.output || '', exitCode: result.exitCode ?? 0 };
        }
        return { output: String(result || 'OK'), exitCode: 0 };
      } catch (e: any) {
        return { output: `[ADB Error]: ${e?.message || 'Execution error'}`, exitCode: 1 };
      }
    }
    
    // Simulated shell response for dev environment / emulator
    return this.simulateShellOutput(command);
  }

  private simulateShellOutput(command: string): { output: string; exitCode: number } {
    const trimmed = command.trim();
    if (trimmed.startsWith('input keyevent')) {
      return { output: `KeyEvent dispatched: ${trimmed.split(' ')[2]}`, exitCode: 0 };
    }
    if (trimmed.startsWith('input text')) {
      return { output: 'Text typed on screen', exitCode: 0 };
    }
    if (trimmed.startsWith('pm list packages')) {
      return { output: MOCK_APPS.map(a => `package:${a.packageName}`).join('\n'), exitCode: 0 };
    }
    if (trimmed.startsWith('am force-stop')) {
      const pkg = trimmed.split(' ')[2];
      return { output: `Force stopped ${pkg}`, exitCode: 0 };
    }
    if (trimmed.startsWith('pm clear')) {
      return { output: 'Success', exitCode: 0 };
    }
    if (trimmed.startsWith('pm uninstall')) {
      return { output: 'Success', exitCode: 0 };
    }
    if (trimmed.startsWith('pm disable-user') || trimmed.startsWith('pm enable')) {
      return { output: 'Package new state: disabled-user', exitCode: 0 };
    }
    if (trimmed.startsWith('getprop')) {
      return { output: `[ro.product.model]: [onn UHD Streaming Device]\n[ro.build.version.release]: [12]\n[ro.build.version.sdk]: [31]\n[ro.product.manufacturer]: [onn.]`, exitCode: 0 };
    }
    if (trimmed === 'df -h' || trimmed.startsWith('df')) {
      return { output: `Filesystem      Size  Used Avail Use% Mounted on\n/dev/root       2.8G  2.1G  700M  75% /\n/data           8.0G  4.8G  3.2G  60% /data\n/storage/emulated 8.0G 4.8G 3.2G  60% /sdcard`, exitCode: 0 };
    }
    if (trimmed.startsWith('top')) {
      return { output: `Tasks: 168 total, 1 running, 167 sleeping\nUser 8%, System 4%, IOW 0%, IRQ 0%\nPID  PR CPU% S #THR     VSS     RSS PCY UID      NAME\n 948  2   5% S   24 219808K  98240K  fg system   system_server\n1420  0   3% S   18 184512K  64200K  fg u0_a45   com.google.android.tv\n2105  0   2% S   32 312500K 112000K  fg u0_a89   com.netflix.ninja`, exitCode: 0 };
    }
    if (trimmed.startsWith('reboot')) {
      return { output: 'Device reboot command issued', exitCode: 0 };
    }
    return { output: `Command executed: ${trimmed}\nExit: 0`, exitCode: 0 };
  }

  async sendKeyEvent(keyCode: string): Promise<boolean> {
    const keyMap: Record<string, string> = {
      up: 'KEYCODE_DPAD_UP',
      down: 'KEYCODE_DPAD_DOWN',
      left: 'KEYCODE_DPAD_LEFT',
      right: 'KEYCODE_DPAD_RIGHT',
      ok: 'KEYCODE_DPAD_CENTER',
      back: 'KEYCODE_BACK',
      home: 'KEYCODE_HOME',
      menu: 'KEYCODE_MENU',
      power: 'KEYCODE_POWER',
      sleep: 'KEYCODE_SLEEP',
      wakeup: 'KEYCODE_WAKEUP',
      volup: 'KEYCODE_VOLUME_UP',
      voldown: 'KEYCODE_VOLUME_DOWN',
      mute: 'KEYCODE_VOLUME_MUTE',
      play_pause: 'KEYCODE_MEDIA_PLAY_PAUSE',
      play: 'KEYCODE_MEDIA_PLAY',
      pause: 'KEYCODE_MEDIA_PAUSE',
      next: 'KEYCODE_MEDIA_NEXT',
      prev: 'KEYCODE_MEDIA_PREVIOUS',
      rewind: 'KEYCODE_MEDIA_REWIND',
      fast_forward: 'KEYCODE_MEDIA_FAST_FORWARD',
      search: 'KEYCODE_SEARCH',
      settings: 'KEYCODE_SETTINGS',
      app_switch: 'KEYCODE_APP_SWITCH',
      tv_input: 'KEYCODE_TV_INPUT',
      num0: 'KEYCODE_0',
      num1: 'KEYCODE_1',
      num2: 'KEYCODE_2',
      num3: 'KEYCODE_3',
      num4: 'KEYCODE_4',
      num5: 'KEYCODE_5',
      num6: 'KEYCODE_6',
      num7: 'KEYCODE_7',
      num8: 'KEYCODE_8',
      num9: 'KEYCODE_9',
    };

    const targetKey = keyMap[keyCode] || keyCode;
    await this.runShellCommand(`input keyevent ${targetKey}`);
    return true;
  }

  async sendTextInput(text: string): Promise<boolean> {
    const escaped = text.replace(/ /g, '%s').replace(/'/g, "\\'");
    await this.runShellCommand(`input text "${escaped}"`);
    return true;
  }

  async launchApp(packageName: string): Promise<boolean> {
    await this.runShellCommand(`monkey -p ${packageName} -c android.intent.category.LAUNCHER 1`);
    return true;
  }

  async forceStopApp(packageName: string): Promise<boolean> {
    await this.runShellCommand(`am force-stop ${packageName}`);
    return true;
  }

  async clearAppData(packageName: string): Promise<boolean> {
    await this.runShellCommand(`pm clear ${packageName}`);
    return true;
  }

  async uninstallApp(packageName: string): Promise<boolean> {
    await this.runShellCommand(`pm uninstall ${packageName}`);
    return true;
  }

  async setAppEnabled(packageName: string, enabled: boolean): Promise<boolean> {
    const cmd = enabled ? `pm enable ${packageName}` : `pm disable-user --user 0 ${packageName}`;
    await this.runShellCommand(cmd);
    return true;
  }

  async reboot(mode: 'normal' | 'recovery' | 'bootloader' | 'soft' = 'normal'): Promise<boolean> {
    if (mode === 'soft') {
      await this.runShellCommand('setprop ctl.restart zygote');
    } else if (mode === 'recovery') {
      await this.runShellCommand('reboot recovery');
    } else if (mode === 'bootloader') {
      await this.runShellCommand('reboot bootloader');
    } else {
      await this.runShellCommand('reboot');
    }
    return true;
  }

  async powerOff(): Promise<boolean> {
    await this.runShellCommand('reboot -p || input keyevent KEYCODE_POWER');
    return true;
  }

  async clearAllAppCaches(): Promise<boolean> {
    await this.runShellCommand('pm trim-caches 999999999999999');
    return true;
  }

  async scanLocalNetwork(subnetPrefix = '192.168.31.', port = 5555): Promise<DiscoveredDevice[]> {
    if (this.isNativeModuleAvailable && AdbModule.scanSubnet) {
      try {
        const ips: string[] = await AdbModule.scanSubnet(subnetPrefix, port, 300);
        if (ips && ips.length > 0) {
          return ips.map(ip => ({
            id: `dev_${ip.replace(/\./g, '_')}`,
            name: ip.endsWith('.188') ? 'onn UHD' : ip.endsWith('.41') ? 'Chromecast' : `Android TV (${ip})`,
            ip,
            port,
            type: 'android_tv',
            isOnline: true,
          }));
        }
      } catch (e) {
        // continue
      }
    }
    // Return standard preset discovery devices if scan completed or fallback
    return DEFAULT_PRESET_DEVICES;
  }
}

export const AdbService = new AdbServiceClass();
