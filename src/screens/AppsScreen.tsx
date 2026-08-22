import React, { useState, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  TextInput,
  ToastAndroid,
  Platform,
} from 'react-native';
import { Ionicons, MaterialCommunityIcons, Feather } from '@expo/vector-icons';
import { Colors } from '../theme/colors';
import { HeaderBar } from '../components/HeaderBar';
import { AppActionModal } from '../components/AppActionModal';
import { DiscoveredDevice, TVApp } from '../types';
import { MOCK_APPS } from '../services/AdbService';

interface AppsScreenProps {
  device: DiscoveredDevice;
  onDisconnect: () => void;
}

export const AppsScreen: React.FC<AppsScreenProps> = ({ device, onDisconnect }) => {
  const [apps, setApps] = useState<TVApp[]>(MOCK_APPS);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'size' | 'name'>('size');
  const [filterRunningOnly, setFilterRunningOnly] = useState(false);
  const [filterType, setFilterType] = useState<'all' | 'user' | 'system'>('all');
  const [selectedApp, setSelectedApp] = useState<TVApp | null>(null);

  const filteredAndSortedApps = useMemo(() => {
    let result = [...apps];

    // Filter search
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter(
        (a) =>
          a.name.toLowerCase().includes(q) ||
          a.packageName.toLowerCase().includes(q)
      );
    }

    // Filter running
    if (filterRunningOnly) {
      result = result.filter((a) => a.isRunning);
    }

    // Filter user/system
    if (filterType === 'user') {
      result = result.filter((a) => !a.isSystem);
    } else if (filterType === 'system') {
      result = result.filter((a) => a.isSystem);
    }

    // Sorting
    result.sort((a, b) => {
      if (sortBy === 'size') {
        return b.sizeBytes - a.sizeBytes;
      }
      return a.name.localeCompare(b.name);
    });

    return result;
  }, [apps, searchQuery, sortBy, filterRunningOnly, filterType]);

  const handleAppUpdated = (updated: TVApp) => {
    setApps(apps.map((a) => (a.id === updated.id ? updated : a)));
  };

  const toggleSort = () => {
    setSortBy(sortBy === 'size' ? 'name' : 'size');
    if (Platform.OS === 'android') {
      ToastAndroid.show(
        `Sorting by ${sortBy === 'size' ? 'Name' : 'Size'}`,
        ToastAndroid.SHORT
      );
    }
  };

  return (
    <View style={styles.container}>
      <HeaderBar
        title={device.name}
        subtitle="DEVICE"
        showBack={true}
        onBack={onDisconnect}
      />

      <View style={styles.content}>
        {/* Search Bar matching Screenshot 3 */}
        <View style={styles.searchBarContainer}>
          <Ionicons name="search" size={20} color={Colors.textSecondary} style={styles.searchIcon} />
          <TextInput
            style={styles.searchInput}
            placeholder="Search"
            placeholderTextColor={Colors.textTertiary}
            value={searchQuery}
            onChangeText={setSearchQuery}
            clearButtonMode="while-editing"
          />
          {searchQuery ? (
            <TouchableOpacity onPress={() => setSearchQuery('')} style={styles.clearSearchBtn}>
              <Ionicons name="close-circle" size={18} color={Colors.textSecondary} />
            </TouchableOpacity>
          ) : null}
        </View>

        {/* Filter Pills matching Screenshot 3 */}
        <View style={styles.filterRow}>
          <TouchableOpacity
            style={[styles.filterPill, sortBy === 'size' && styles.activeFilterPill]}
            onPress={toggleSort}
          >
            <MaterialCommunityIcons 
              name="sort-variant" 
              size={16} 
              color={sortBy === 'size' ? Colors.onPrimaryContainer : Colors.textPrimary} 
            />
            <Text style={[styles.filterPillText, sortBy === 'size' && styles.activeFilterPillText]}>
              Sort by {sortBy}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.filterPill, filterRunningOnly && styles.activeFilterPill]}
            onPress={() => setFilterRunningOnly(!filterRunningOnly)}
          >
            <MaterialCommunityIcons 
              name="application" 
              size={16} 
              color={filterRunningOnly ? Colors.onPrimaryContainer : Colors.textPrimary} 
            />
            <Text style={[styles.filterPillText, filterRunningOnly && styles.activeFilterPillText]}>
              Running apps
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.filterPill, filterType !== 'all' && styles.activeFilterPill]}
            onPress={() => {
              if (filterType === 'all') setFilterType('user');
              else if (filterType === 'user') setFilterType('system');
              else setFilterType('all');
            }}
          >
            <Text style={[styles.filterPillText, filterType !== 'all' && styles.activeFilterPillText]}>
              {filterType === 'all' ? 'All' : filterType === 'user' ? 'User only' : 'System only'}
            </Text>
          </TouchableOpacity>
        </View>

        {/* App List */}
        <FlatList
          data={filteredAndSortedApps}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.listContent}
          showsVerticalScrollIndicator={false}
          renderItem={({ item }) => {
            return (
              <View style={styles.appCard}>
                {/* App Icon */}
                <View style={[styles.appIconBox, { backgroundColor: item.iconBg }]}>
                  <MaterialCommunityIcons
                    name={(item.iconType as any) || 'application'}
                    size={24}
                    color="#ffffff"
                  />
                </View>

                {/* App Details */}
                <View style={styles.appInfo}>
                  <Text style={styles.appName} numberOfLines={1}>
                    {item.name}
                  </Text>
                  <Text style={styles.appMeta} numberOfLines={1}>
                    {item.version} - {item.size}
                  </Text>
                  {!item.isEnabled && (
                    <Text style={styles.disabledBadge}>Disabled</Text>
                  )}
                </View>

                {/* 3-dots Context Menu Button */}
                <TouchableOpacity
                  style={styles.moreActionBtn}
                  onPress={() => setSelectedApp(item)}
                  hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
                >
                  <Ionicons name="ellipsis-vertical" size={20} color={Colors.textPrimary} />
                </TouchableOpacity>
              </View>
            );
          }}
          ListEmptyComponent={
            <View style={styles.emptyState}>
              <MaterialCommunityIcons name="application-outline" size={48} color={Colors.textTertiary} />
              <Text style={styles.emptyText}>No applications found</Text>
            </View>
          }
        />
      </View>

      {/* App Context Menu Popup Modal */}
      <AppActionModal
        visible={!!selectedApp}
        app={selectedApp}
        onClose={() => setSelectedApp(null)}
        onAppUpdated={handleAppUpdated}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  content: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 12,
  },
  searchBarContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ffffff',
    borderRadius: 24,
    paddingHorizontal: 14,
    height: 48,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: 10,
  },
  searchIcon: {
    marginRight: 8,
  },
  searchInput: {
    flex: 1,
    fontSize: 15,
    color: Colors.textPrimary,
  },
  clearSearchBtn: {
    padding: 4,
  },
  filterRow: {
    flexDirection: 'row',
    marginBottom: 10,
  },
  filterPill: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#e1ede2',
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 18,
    marginRight: 8,
    borderWidth: 1,
    borderColor: '#d2e4d4',
  },
  activeFilterPill: {
    backgroundColor: Colors.primaryContainer,
    borderColor: '#a3e8b8',
  },
  filterPillText: {
    fontSize: 12,
    color: Colors.textPrimary,
    fontWeight: '600',
    marginLeft: 4,
  },
  activeFilterPillText: {
    color: Colors.onPrimaryContainer,
    fontWeight: '700',
  },
  listContent: {
    paddingBottom: 90,
  },
  appCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#e1ede2',
    borderRadius: 16,
    paddingVertical: 14,
    paddingHorizontal: 16,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#d2e4d4',
  },
  appIconBox: {
    width: 44,
    height: 44,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 14,
  },
  appInfo: {
    flex: 1,
  },
  appName: {
    fontSize: 15,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  appMeta: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  disabledBadge: {
    fontSize: 10,
    color: Colors.danger,
    fontWeight: '700',
    marginTop: 2,
  },
  moreActionBtn: {
    padding: 8,
  },
  emptyState: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 60,
  },
  emptyText: {
    color: Colors.textSecondary,
    fontSize: 14,
    marginTop: 10,
  },
});
