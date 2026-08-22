export interface DiscoveredDevice {
  id: string;
  name: string;
  ip: string;
  port: number;
  type: 'android_tv' | 'fire_tv' | 'chromecast' | 'other';
  lastConnected?: number;
  isOnline: boolean;
  model?: string;
  manufacturer?: string;
  androidVersion?: string;
}

export interface TVApp {
  id: string;
  packageName: string;
  name: string;
  version: string;
  size: string;
  sizeBytes: number;
  iconType: string;
  iconBg: string;
  isSystem: boolean;
  isRunning: boolean;
  isEnabled: boolean;
  category?: string;
}

export interface DeviceInfoData {
  model: string;
  manufacturer: string;
  brand: string;
  device: string;
  androidVersion: string;
  sdkVersion: number;
  securityPatch: string;
  buildNumber: string;
  resolution: string;
  density: string;
  uptime: string;
  ramTotal: string;
  ramUsed: string;
  ramFree: string;
  ramPercent: number;
  storageTotal: string;
  storageUsed: string;
  storageFree: string;
  storagePercent: number;
  ipAddress: string;
  macAddress: string;
  wifiSsid: string;
  adbPort: number;
}

export interface RemoteFile {
  name: string;
  path: string;
  isDirectory: boolean;
  size: string;
  modified: string;
  permissions: string;
}

export interface ShellCommandHistory {
  id: string;
  command: string;
  output: string;
  exitCode: number;
  timestamp: number;
}
