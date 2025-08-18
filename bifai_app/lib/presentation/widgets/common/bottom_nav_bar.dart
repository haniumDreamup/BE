import 'package:flutter/material.dart';
import '../../../core/config/theme_config.dart';

class BottomNavBar extends StatelessWidget {
  final int selectedIndex;
  final Function(int) onItemSelected;

  const BottomNavBar({
    super.key,
    required this.selectedIndex,
    required this.onItemSelected,
  });

  @override
  Widget build(BuildContext context) {
    return NavigationBar(
      selectedIndex: selectedIndex,
      onDestinationSelected: onItemSelected,
      height: 80,
      destinations: const [
        NavigationDestination(
          icon: Icon(Icons.home_outlined, size: 28),
          selectedIcon: Icon(Icons.home, size: 28),
          label: '홈',
        ),
        NavigationDestination(
          icon: Icon(Icons.medication_outlined, size: 28),
          selectedIcon: Icon(Icons.medication, size: 28),
          label: '약',
        ),
        NavigationDestination(
          icon: Icon(Icons.calendar_today_outlined, size: 28),
          selectedIcon: Icon(Icons.calendar_today, size: 28),
          label: '일정',
        ),
        NavigationDestination(
          icon: Icon(Icons.person_outline, size: 28),
          selectedIcon: Icon(Icons.person, size: 28),
          label: '내 정보',
        ),
      ],
    );
  }
}