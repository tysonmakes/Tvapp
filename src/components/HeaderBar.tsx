import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, StatusBar } from 'react-native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Colors } from '../theme/colors';

interface HeaderBarProps {
  title: string;
  subtitle?: string;
  showBack?: boolean;
  onBack?: () => void;
  onMenuPress?: () => void;
  showMenu?: boolean;
  isBrandTitle?: boolean;
}

export const HeaderBar: React.FC<HeaderBarProps> = ({
  title,
  subtitle,
  showBack = false,
  onBack,
  onMenuPress,
  showMenu = true,
  isBrandTitle = false,
}) => {
  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={Colors.primary} />
      <View style={styles.content}>
        <View style={styles.leftRow}>
          {showBack ? (
            <TouchableOpacity 
              style={styles.iconButton} 
              onPress={onBack}
              hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
            >
              <Ionicons name="arrow-back" size={24} color={Colors.textOnDark} />
            </TouchableOpacity>
          ) : (
            <View style={styles.brandIconWrap}>
              <MaterialCommunityIcons name="television-guide" size={26} color="#b9f3cb" />
            </View>
          )}

          <View style={styles.titleContainer}>
            <Text style={[styles.title, isBrandTitle && styles.brandTitle]} numberOfLines={1}>
              {title}
            </Text>
            {subtitle ? (
              <Text style={styles.subtitle}>{subtitle.toUpperCase()}</Text>
            ) : null}
          </View>
        </View>

        {showMenu && (
          <TouchableOpacity 
            style={styles.iconButton} 
            onPress={onMenuPress}
            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
          >
            <Ionicons name="ellipsis-vertical" size={22} color={Colors.textOnDark} />
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.primary,
    paddingTop: StatusBar.currentHeight ? StatusBar.currentHeight + 8 : 42,
    paddingBottom: 14,
    paddingHorizontal: 16,
    borderBottomLeftRadius: 4,
    borderBottomRightRadius: 4,
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 3,
  },
  content: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  leftRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  iconButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 6,
  },
  brandIconWrap: {
    marginRight: 10,
    width: 32,
    height: 32,
    borderRadius: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.12)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  titleContainer: {
    flex: 1,
    justifyContent: 'center',
  },
  title: {
    color: Colors.textOnDark,
    fontSize: 19,
    fontWeight: '700',
    letterSpacing: 0.2,
  },
  brandTitle: {
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: 0.5,
    fontFamily: 'monospace',
  },
  subtitle: {
    color: '#a3d9b4',
    fontSize: 11,
    fontWeight: '600',
    letterSpacing: 1.2,
    marginTop: 1,
  },
});
