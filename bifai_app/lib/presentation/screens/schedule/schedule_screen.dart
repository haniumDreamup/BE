import 'package:flutter/material.dart';
import '../../../core/config/theme_config.dart';
import '../../widgets/common/bottom_nav_bar.dart';

class ScheduleScreen extends StatefulWidget {
  const ScheduleScreen({super.key});

  @override
  State<ScheduleScreen> createState() => _ScheduleScreenState();
}

class _ScheduleScreenState extends State<ScheduleScreen> {
  int _selectedIndex = 2;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: ThemeConfig.backgroundColor,
      appBar: AppBar(
        title: const Text('일정'),
        automaticallyImplyLeading: false,
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Calendar Preview
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 10,
                  ),
                ],
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        '${DateTime.now().year}년 ${DateTime.now().month}월 ${DateTime.now().day}일',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const Text(
                        '오늘',
                        style: TextStyle(
                          color: ThemeConfig.primaryColor,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: List.generate(7, (index) {
                      final date = DateTime.now().add(Duration(days: index - 3));
                      final isToday = index == 3;
                      return Column(
                        children: [
                          Text(
                            _getWeekdayName(date.weekday),
                            style: TextStyle(
                              fontSize: 12,
                              color: isToday ? ThemeConfig.primaryColor : Colors.grey,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Container(
                            width: 40,
                            height: 40,
                            decoration: BoxDecoration(
                              color: isToday ? ThemeConfig.primaryColor : Colors.transparent,
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Center(
                              child: Text(
                                date.day.toString(),
                                style: TextStyle(
                                  color: isToday ? Colors.white : Colors.black,
                                  fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
                                ),
                              ),
                            ),
                          ),
                        ],
                      );
                    }),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            
            // Today's Schedule
            const Text(
              '오늘의 일정',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            
            _ScheduleItem(
              time: '09:00',
              title: '병원 진료',
              location: '서울 대학병원',
              type: ScheduleType.appointment,
              completed: false,
            ),
            
            _ScheduleItem(
              time: '14:00',
              title: '물리치료',
              location: '재활센터',
              type: ScheduleType.therapy,
              completed: false,
            ),
            
            _ScheduleItem(
              time: '16:00',
              title: '가족 모임',
              location: '집',
              type: ScheduleType.family,
              completed: false,
            ),
          ],
        ),
      ),
      bottomNavigationBar: BottomNavBar(
        selectedIndex: _selectedIndex,
        onItemSelected: (index) {
          setState(() {
            _selectedIndex = index;
          });
        },
      ),
    );
  }
  
  String _getWeekdayName(int weekday) {
    switch (weekday) {
      case 1:
        return '월';
      case 2:
        return '화';
      case 3:
        return '수';
      case 4:
        return '목';
      case 5:
        return '금';
      case 6:
        return '토';
      case 7:
        return '일';
      default:
        return '';
    }
  }
}

enum ScheduleType { appointment, therapy, family, other }

class _ScheduleItem extends StatelessWidget {
  final String time;
  final String title;
  final String location;
  final ScheduleType type;
  final bool completed;

  const _ScheduleItem({
    required this.time,
    required this.title,
    required this.location,
    required this.type,
    required this.completed,
  });

  @override
  Widget build(BuildContext context) {
    final color = _getTypeColor();
    final icon = _getTypeIcon();
    
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: completed ? Colors.grey.shade100 : Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: completed ? Colors.grey.shade300 : Colors.grey.shade200,
        ),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            // TODO: Show schedule detail
          },
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: color.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    icon,
                    color: color,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text(
                            time,
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              color: color,
                            ),
                          ),
                          const SizedBox(width: 8),
                          if (completed)
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 8,
                                vertical: 2,
                              ),
                              decoration: BoxDecoration(
                                color: ThemeConfig.successColor.withOpacity(0.1),
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: const Text(
                                '완료',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: ThemeConfig.successColor,
                                ),
                              ),
                            ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        title,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          Icon(
                            Icons.location_on_outlined,
                            size: 16,
                            color: Colors.grey.shade600,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            location,
                            style: TextStyle(
                              fontSize: 14,
                              color: Colors.grey.shade600,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: Icon(
                    completed ? Icons.check_circle : Icons.circle_outlined,
                    color: completed ? ThemeConfig.successColor : Colors.grey,
                  ),
                  onPressed: () {
                    // TODO: Toggle completion
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
  
  Color _getTypeColor() {
    switch (type) {
      case ScheduleType.appointment:
        return ThemeConfig.primaryColor;
      case ScheduleType.therapy:
        return ThemeConfig.successColor;
      case ScheduleType.family:
        return ThemeConfig.warningColor;
      case ScheduleType.other:
        return Colors.grey;
    }
  }
  
  IconData _getTypeIcon() {
    switch (type) {
      case ScheduleType.appointment:
        return Icons.local_hospital;
      case ScheduleType.therapy:
        return Icons.accessibility_new;
      case ScheduleType.family:
        return Icons.family_restroom;
      case ScheduleType.other:
        return Icons.event;
    }
  }
}