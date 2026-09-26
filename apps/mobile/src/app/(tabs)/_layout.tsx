import { Redirect, Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, View, type ColorValue } from 'react-native';

import { useAuthStore } from '@/store';
import { colors, radius, spacing, typography } from '@/theme';

type TabIconProps = {
  color: ColorValue;
  focused: boolean;
  name: keyof typeof Ionicons.glyphMap;
  selectedName: keyof typeof Ionicons.glyphMap;
  size: number;
};

function TabIcon({ color, focused, name, selectedName, size }: TabIconProps) {
  return <Ionicons color={color} name={focused ? selectedName : name} size={size} />;
}

function PostTabIcon() {
  return (
    <View style={styles.postIconContainer}>
      <Ionicons color={colors.surface} name="add" size={28} />
    </View>
  );
}

export default function TabsLayout() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const isLoading = useAuthStore((state) => state.isLoading);

  if (!isLoading && !isAuthenticated) {
    return <Redirect href="/login" />;
  }

  return (
    <Tabs
      initialRouteName="home"
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.secondaryText,
        tabBarLabelStyle: styles.tabLabel,
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopColor: colors.border,
          height: 92,
          paddingBottom: spacing.md,
          paddingTop: spacing.xs,
        },
      }}
    >
      <Tabs.Screen
        name="home"
        options={{
          title: 'Home',
          tabBarIcon: ({ color, focused, size }) => (
            <TabIcon
              color={color}
              focused={focused}
              name="home-outline"
              selectedName="home"
              size={size}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="favorites"
        options={{
          title: 'Saved',
          tabBarIcon: ({ color, focused, size }) => (
            <TabIcon
              color={color}
              focused={focused}
              name="heart-outline"
              selectedName="heart"
              size={size}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="create-listing"
        options={{
          title: 'Post',
          tabBarIcon: () => <PostTabIcon />,
          tabBarItemStyle: styles.postTabItem,
          tabBarLabelStyle: styles.postTabLabel,
        }}
      />
      <Tabs.Screen
        name="messages"
        options={{
          title: 'Messages',
          tabBarIcon: ({ color, focused, size }) => (
            <TabIcon
              color={color}
              focused={focused}
              name="chatbubble-ellipses-outline"
              selectedName="chatbubble-ellipses"
              size={size}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profile',
          tabBarIcon: ({ color, focused, size }) => (
            <TabIcon
              color={color}
              focused={focused}
              name="person-circle-outline"
              selectedName="person-circle"
              size={size}
            />
          ),
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  tabLabel: {
    ...typography.caption,
    fontSize: 11,
    fontWeight: '600',
  },
  postIconContainer: {
    alignItems: 'center',
    backgroundColor: colors.primary,
    borderRadius: radius.lg,
    height: 38,
    justifyContent: 'center',
    marginBottom: 2,
    width: 38,
  },
  postTabItem: {
    paddingTop: 0,
  },
  postTabLabel: {
    ...typography.caption,
    color: colors.primary,
    fontSize: 11,
    fontWeight: '700',
  },
});
