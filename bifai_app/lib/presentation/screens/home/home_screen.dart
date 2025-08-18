import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/config/theme_config.dart';
import '../../widgets/common/bottom_nav_bar.dart';
import '../../widgets/home/quick_action_card.dart';
import '../../widgets/home/today_summary_card.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: ThemeConfig.backgroundColor,
      appBar: AppBar(
        title: const Text('BIF-AI'),
        actions: [
          IconButton(
            icon: const Icon(Icons.notifications_outlined),
            onPressed: () {
              // TODO: Show notifications
            },
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Greeting
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [
                      ThemeConfig.primaryColor,
                      ThemeConfig.primaryColor.withOpacity(0.8),
                    ],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '좋은 아침이에요! 👋',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '${DateTime.now().year}년 ${DateTime.now().month}월 ${DateTime.now().day}일',
                      style: const TextStyle(
                        fontSize: 16,
                        color: Colors.white,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              
              // Today's Summary
              const Text(
                '오늘의 할 일',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 12),
              const TodaySummaryCard(
                medicationsToTake: 3,
                schedulesToday: 2,
                nextEventTitle: '아침 약 먹기',
                nextEventTime: '08:00',
              ),
              const SizedBox(height: 24),
              
              // Quick Actions
              const Text(
                '빠른 실행',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 12),
              GridView.count(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisCount: 2,
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                childAspectRatio: 1.5,
                children: [
                  QuickActionCard(
                    title: '약 복용',
                    icon: Icons.medication,
                    color: ThemeConfig.successColor,
                    onTap: () => context.push('/medications'),
                  ),
                  QuickActionCard(
                    title: '일정 확인',
                    icon: Icons.calendar_today,
                    color: ThemeConfig.primaryColor,
                    onTap: () => context.push('/schedule'),
                  ),
                  QuickActionCard(
                    title: '긴급 연락',
                    icon: Icons.emergency,
                    color: ThemeConfig.errorColor,
                    onTap: () => context.push('/emergency'),
                  ),
                  QuickActionCard(
                    title: '보호자 연락',
                    icon: Icons.phone,
                    color: ThemeConfig.warningColor,
                    onTap: () {
                      // TODO: Call guardian
                    },
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: BottomNavBar(
        selectedIndex: _selectedIndex,
        onItemSelected: (index) {
          setState(() {
            _selectedIndex = index;
          });
          switch (index) {
            case 0:
              context.go('/home');
              break;
            case 1:
              context.go('/medications');
              break;
            case 2:
              context.go('/schedule');
              break;
            case 3:
              // TODO: Profile screen
              break;
          }
        },
      ),
    );
  }
}