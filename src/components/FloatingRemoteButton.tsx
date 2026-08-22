import React from 'react';
import { StyleSheet, TouchableOpacity } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';

interface FloatingRemoteButtonProps {
  onPress: () => void;
}

export const FloatingRemoteButton: React.FC<FloatingRemoteButtonProps> = ({ onPress }) => {
  return (
    <TouchableOpacity
      style={styles.container}
      onPress={onPress}
      activeOpacity={0.85}
      accessibilityLabel="Open TV Remote"
    >
      <MaterialCommunityIcons name="remote" size={26} color="#ffffff" />
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    right: 18,
    bottom: 74,
    width: 48,
    height: 48,
    borderRadius: 14,
    backgroundColor: Colors.fabGreen,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 6,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    zIndex: 99,
  },
});
