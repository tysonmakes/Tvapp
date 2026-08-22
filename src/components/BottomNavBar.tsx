import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';

export type TabType = 'tools' | 'apps' | 'shell' | 'info';

interface BottomNavBarProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
}

export const BottomNavBar: React.FC<BottomNavBarProps> = ({ activeTab, onTabChange }) => {
  const tabs = [
    {
      id: 'tools' as TabType,
      label: 'Tools',
      icon: (active: boolean) => (
        <Ionicons 
          name={active ? 'build' : 'build-outline'} 
          size={20} 
          color={active ? Colors.onPrimaryContainer : Colors.textSecondary} 
        />
      ),
    },
    {
      id: 'apps' as TabType,
      label: 'Apps',
      icon: (active: boolean) => (
        <MaterialCommunityIcons 
          name={active ? 'view-grid' : 'view-grid-outline'} 
          size={22} 
          color={active ? Colors.onPrimaryContainer : Colors.textSecondary} 
        />
      ),
    },
    {
      id: 'shell' as TabType,
      label: 'Shell',
      icon: (active: boolean) => (
        <Ionicons 
          name={active ? 'logo-android' : 'logo-android'} 
          size={22} 
          color={active ? Colors.onPrimaryContainer : Colors.textSecondary} 
        />
      ),
    },
    {
      id: 'info' as TabType,
      label: 'Info',
      icon: (active: boolean) => (
        <Ionicons 
          name={active ? 'information-circle' : 'information-circle-outline'} 
          size={22} 
          color={active ? Colors.onPrimaryContainer : Colors.textSecondary} 
        />
      ),
    },
  ];

  return (
    <View style={styles.container}>
      <View style={styles.tabBar}>
        {tabs.map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <TouchableOpacity
              key={tab.id}
              style={styles.tabItem}
              onPress={() => onTabChange(tab.id)}
              activeOpacity={0.7}
            >
              <View style={[styles.iconContainer, isActive && styles.activeIconContainer]}>
                {tab.icon(isActive)}
              </View>
              <Text style={[styles.label, isActive ? styles.activeLabel : styles.inactiveLabel]}>
                {tab.label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#d8ebd9',
    borderTopWidth: 1,
    borderTopColor: Colors.border,
    paddingBottom: 8,
    paddingTop: 6,
  },
  tabBar: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
  },
  tabItem: {
    alignItems: 'center',
    justifyContent: 'center',
    flex: 1,
    paddingVertical: 2,
  },
  iconContainer: {
    paddingHorizontal: 20,
    paddingVertical: 4,
    borderRadius: 16,
    marginBottom: 3,
    alignItems: 'center',
    justifyContent: 'center',
  },
  activeIconContainer: {
    backgroundColor: Colors.primaryContainer,
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
  },
  activeLabel: {
    color: Colors.textPrimary,
    fontWeight: '700',
  },
  inactiveLabel: {
    color: Colors.textSecondary,
  },
});
