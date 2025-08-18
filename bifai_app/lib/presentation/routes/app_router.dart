import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../screens/auth/login_screen.dart';
import '../screens/auth/splash_screen.dart';
import '../screens/home/home_screen.dart';
import '../screens/home/toss_home_screen.dart';
import '../screens/medication/medication_list_screen.dart';
import '../screens/schedule/schedule_screen.dart';
import '../screens/emergency/emergency_screen.dart';
import '../screens/network/network_discovery_screen.dart';

class AppRouter {
  static final GoRouter router = GoRouter(
    initialLocation: '/home',
    routes: [
      GoRoute(
        path: '/splash',
        builder: (context, state) => const SplashScreen(),
      ),
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: '/home',
        builder: (context, state) => const TossHomeScreen(), // 토스 스타일 홈
      ),
      GoRoute(
        path: '/medications',
        builder: (context, state) => const MedicationListScreen(),
      ),
      GoRoute(
        path: '/schedule',
        builder: (context, state) => const ScheduleScreen(),
      ),
      GoRoute(
        path: '/emergency',
        builder: (context, state) => const EmergencyScreen(),
      ),
      GoRoute(
        path: '/network-discovery',
        builder: (context, state) => const NetworkDiscoveryScreen(),
      ),
    ],
    errorBuilder: (context, state) => Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.error_outline,
              size: 64,
              color: Colors.red,
            ),
            const SizedBox(height: 16),
            const Text(
              '페이지를 찾을 수 없어요',
              style: TextStyle(fontSize: 18),
            ),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: () => context.go('/home'),
              child: const Text('홈으로 가기'),
            ),
          ],
        ),
      ),
    ),
  );
}