import React, { useState } from 'react';
import { View, Text, StyleSheet, Modal, TouchableOpacity, FlatList, Alert, ToastAndroid, Platform } from 'react-native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { RemoteFile } from '../types';
import { MOCK_FILES, AdbService } from '../services/AdbService';

interface FileManagerModalProps {
  visible: boolean;
  onClose: () => void;
}

export const FileManagerModal: React.FC<FileManagerModalProps> = ({ visible, onClose }) => {
  const [currentPath, setCurrentPath] = useState('/sdcard');
  const [files, setFiles] = useState<RemoteFile[]>(MOCK_FILES);

  const showToast = (msg: string) => {
    if (Platform.OS === 'android') {
      ToastAndroid.show(msg, ToastAndroid.SHORT);
    }
  };

  const handleFilePress = (item: RemoteFile) => {
    if (item.isDirectory) {
      setCurrentPath(item.path);
      showToast(`Opened folder ${item.name}`);
    } else {
      Alert.alert(
        item.name,
        `Path: ${item.path}\nSize: ${item.size}\nPermissions: ${item.permissions}`,
        [
          { text: 'Cancel', style: 'cancel' },
          {
            text: 'Delete',
            style: 'destructive',
            onPress: () => {
              setFiles(files.filter(f => f.path !== item.path));
              showToast(`Deleted ${item.name}`);
            },
          },
          {
            text: 'Download',
            onPress: () => showToast(`Downloaded ${item.name} to phone`),
          },
        ]
      );
    }
  };

  const handleUploadFile = () => {
    const newFile: RemoteFile = {
      name: `uploaded_file_${Date.now().toString().slice(-4)}.mp4`,
      path: `${currentPath}/uploaded_file.mp4`,
      isDirectory: false,
      size: '12.4 MB',
      modified: 'Just now',
      permissions: '-rw-rw----',
    };
    setFiles([newFile, ...files]);
    showToast('File uploaded to TV successfully!');
  };

  const handleGoUp = () => {
    if (currentPath === '/sdcard') return;
    const parts = currentPath.split('/');
    parts.pop();
    setCurrentPath(parts.join('/') || '/sdcard');
  };

  return (
    <Modal visible={visible} animationType="slide" onRequestClose={onClose}>
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={onClose} style={styles.backBtn}>
            <Ionicons name="arrow-back" size={24} color="#ffffff" />
          </TouchableOpacity>
          <View style={styles.headerTextWrap}>
            <Text style={styles.headerTitle}>TV File Manager</Text>
            <Text style={styles.headerPath} numberOfLines={1}>{currentPath}</Text>
          </View>
          <TouchableOpacity onPress={handleUploadFile} style={styles.uploadBtn}>
            <Ionicons name="cloud-upload-outline" size={22} color="#ffffff" />
          </TouchableOpacity>
        </View>

        {/* Path Nav */}
        <View style={styles.pathBar}>
          <TouchableOpacity onPress={handleGoUp} style={styles.upBtn} disabled={currentPath === '/sdcard'}>
            <Ionicons name="arrow-up" size={18} color={currentPath === '/sdcard' ? Colors.textTertiary : Colors.textPrimary} />
            <Text style={[styles.upText, currentPath === '/sdcard' && { color: Colors.textTertiary }]}>Up</Text>
          </TouchableOpacity>
          <Text style={styles.itemCount}>{files.length} items</Text>
        </View>

        {/* File list */}
        <FlatList
          data={files}
          keyExtractor={(item) => item.path}
          contentContainerStyle={styles.listContent}
          renderItem={({ item }) => (
            <TouchableOpacity style={styles.fileItem} onPress={() => handleFilePress(item)}>
              <View style={[styles.fileIcon, item.isDirectory ? styles.folderIconBg : styles.docIconBg]}>
                <MaterialCommunityIcons
                  name={item.isDirectory ? 'folder' : item.name.endsWith('.apk') ? 'android' : item.name.endsWith('.png') ? 'image' : 'file-document'}
                  size={24}
                  color={item.isDirectory ? '#f59e0b' : '#3b82f6'}
                />
              </View>
              <View style={styles.fileInfo}>
                <Text style={styles.fileName} numberOfLines={1}>{item.name}</Text>
                <Text style={styles.fileMeta}>{item.size} • {item.modified}</Text>
              </View>
              <Ionicons name="chevron-forward" size={18} color={Colors.textTertiary} />
            </TouchableOpacity>
          )}
        />
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
  },
  backBtn: {
    padding: 6,
    marginRight: 8,
  },
  headerTextWrap: {
    flex: 1,
  },
  headerTitle: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '700',
  },
  headerPath: {
    color: '#b9f3cb',
    fontSize: 12,
    fontFamily: 'monospace',
  },
  uploadBtn: {
    backgroundColor: 'rgba(255,255,255,0.15)',
    padding: 8,
    borderRadius: 20,
  },
  pathBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: Colors.surfaceVariant,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  upBtn: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  upText: {
    fontSize: 13,
    fontWeight: '600',
    color: Colors.textPrimary,
    marginLeft: 4,
  },
  itemCount: {
    fontSize: 12,
    color: Colors.textSecondary,
  },
  listContent: {
    padding: 12,
  },
  fileItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ffffff',
    padding: 12,
    borderRadius: 14,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: Colors.borderLight,
  },
  fileIcon: {
    width: 44,
    height: 44,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  folderIconBg: {
    backgroundColor: '#fef3c7',
  },
  docIconBg: {
    backgroundColor: '#eff6ff',
  },
  fileInfo: {
    flex: 1,
  },
  fileName: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  fileMeta: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
  },
});
