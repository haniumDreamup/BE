import 'package:flutter/material.dart';
import '../../../core/config/theme_config.dart';
import '../../widgets/common/bottom_nav_bar.dart';

class MedicationListScreen extends StatefulWidget {
  const MedicationListScreen({super.key});

  @override
  State<MedicationListScreen> createState() => _MedicationListScreenState();
}

class _MedicationListScreenState extends State<MedicationListScreen> {
  int _selectedIndex = 1;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: ThemeConfig.backgroundColor,
      appBar: AppBar(
        title: const Text('오늘의 약'),
        automaticallyImplyLeading: false,
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Progress Summary
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    ThemeConfig.successColor,
                    ThemeConfig.successColor.withOpacity(0.8),
                  ],
                ),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: const [
                      Text(
                        '오늘의 진행 상황',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                        ),
                      ),
                      SizedBox(height: 4),
                      Text(
                        '1 / 3 완료',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  Container(
                    width: 60,
                    height: 60,
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(30),
                    ),
                    child: const Center(
                      child: Text(
                        '33%',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            
            // Time Sections
            _buildTimeSection('아침 (08:00)', [
              _MedicationItem(
                name: '혈압약',
                description: '심장을 건강하게 해주는 약',
                dosage: '1알',
                taken: true,
                color: ThemeConfig.errorColor,
                icon: Icons.favorite,
              ),
              _MedicationItem(
                name: '비타민',
                description: '몸을 튼튼하게 해주는 약',
                dosage: '1알',
                taken: false,
                color: ThemeConfig.warningColor,
                icon: Icons.wb_sunny,
              ),
            ]),
            
            const SizedBox(height: 24),
            
            _buildTimeSection('점심 (12:00)', [
              _MedicationItem(
                name: '소화제',
                description: '밥을 잘 소화시켜주는 약',
                dosage: '1포',
                taken: false,
                color: ThemeConfig.primaryColor,
                icon: Icons.restaurant,
              ),
            ]),
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
  
  Widget _buildTimeSection(String title, List<Widget> medications) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        ...medications,
      ],
    );
  }
}

class _MedicationItem extends StatelessWidget {
  final String name;
  final String description;
  final String dosage;
  final bool taken;
  final Color color;
  final IconData icon;

  const _MedicationItem({
    required this.name,
    required this.description,
    required this.dosage,
    required this.taken,
    required this.color,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: taken ? Colors.grey.shade100 : Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: taken ? Colors.grey.shade300 : Colors.grey.shade200,
        ),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: taken ? null : () {
            // TODO: Mark as taken
            showDialog(
              context: context,
              builder: (context) => AlertDialog(
                title: Text('$name 복용'),
                content: Text('$name을(를) 복용하셨나요?'),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: const Text('아니요'),
                  ),
                  ElevatedButton(
                    onPressed: () {
                      Navigator.pop(context);
                      // TODO: Update medication status
                    },
                    child: const Text('네, 먹었어요'),
                  ),
                ],
              ),
            );
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
                    color: color.withOpacity(taken ? 0.3 : 0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    icon,
                    color: taken ? Colors.grey : color,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        name,
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w600,
                          decoration: taken ? TextDecoration.lineThrough : null,
                          color: taken ? Colors.grey : null,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        description,
                        style: TextStyle(
                          fontSize: 14,
                          color: taken ? Colors.grey : Colors.grey.shade600,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        dosage,
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w500,
                          color: taken ? Colors.grey : color,
                        ),
                      ),
                    ],
                  ),
                ),
                if (taken)
                  const Icon(
                    Icons.check_circle,
                    color: ThemeConfig.successColor,
                    size: 32,
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}