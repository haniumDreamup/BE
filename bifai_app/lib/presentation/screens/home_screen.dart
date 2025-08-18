import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/config/theme_config.dart';
import '../providers/accessibility_provider.dart';
import '../widgets/common/accessible_button.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final accessibility = context.watch<AccessibilityProvider>();
    
    return Scaffold(
      backgroundColor: ThemeConfig.backgroundColor,
      appBar: AppBar(
        title: const Text('BIF-AI 도우미'),
        centerTitle: true,
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 환영 메시지
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: Column(
                    children: [
                      Icon(
                        Icons.waving_hand,
                        size: 48,
                        color: ThemeConfig.primaryColor,
                      ),
                      const SizedBox(height: 12),
                      Text(
                        '안녕하세요!',
                        style: Theme.of(context).textTheme.headlineMedium,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        '오늘도 좋은 하루 보내세요',
                        style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                          color: ThemeConfig.lightTextColor,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),
              
              // 주요 기능 버튼들
              Expanded(
                child: GridView.count(
                  crossAxisCount: 2,
                  mainAxisSpacing: 16,
                  crossAxisSpacing: 16,
                  children: [
                    _buildFeatureCard(
                      context: context,
                      icon: Icons.medication,
                      title: '약 관리',
                      subtitle: '오늘의 약',
                      color: ThemeConfig.primaryColor,
                      onTap: () {
                        accessibility.speak('약 관리 화면으로 이동합니다');
                      },
                    ),
                    _buildFeatureCard(
                      context: context,
                      icon: Icons.calendar_today,
                      title: '일정',
                      subtitle: '오늘 할 일',
                      color: ThemeConfig.secondaryColor,
                      onTap: () {
                        accessibility.speak('일정 화면으로 이동합니다');
                      },
                    ),
                    _buildFeatureCard(
                      context: context,
                      icon: Icons.emergency,
                      title: 'SOS',
                      subtitle: '긴급 도움',
                      color: ThemeConfig.errorColor,
                      onTap: () {
                        accessibility.speakImportant('긴급 도움을 요청합니다');
                      },
                    ),
                    _buildFeatureCard(
                      context: context,
                      icon: Icons.settings,
                      title: '설정',
                      subtitle: '앱 설정',
                      color: ThemeConfig.lightTextColor,
                      onTap: () {
                        accessibility.speak('설정 화면으로 이동합니다');
                      },
                    ),
                  ],
                ),
              ),
              
              // 음성 안내 토글
              Card(
                child: SwitchListTile(
                  title: const Text('음성 안내'),
                  subtitle: Text(
                    accessibility.voiceGuidanceEnabled ? '켜짐' : '꺼짐',
                  ),
                  value: accessibility.voiceGuidanceEnabled,
                  onChanged: (value) {
                    accessibility.setVoiceGuidanceEnabled(value);
                  },
                  secondary: Icon(
                    accessibility.voiceGuidanceEnabled 
                        ? Icons.volume_up 
                        : Icons.volume_off,
                    color: ThemeConfig.primaryColor,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
      
      // 플로팅 버튼
      floatingActionButton: AccessibleFloatingButton(
        label: '도움말',
        icon: Icons.help_outline,
        onPressed: () {
          accessibility.speak('도움이 필요하시면 보호자에게 연락하세요');
        },
        helpText: '길게 누르면 자세한 도움말을 들을 수 있어요',
      ),
    );
  }
  
  Widget _buildFeatureCard({
    required BuildContext context,
    required IconData icon,
    required String title,
    required String subtitle,
    required Color color,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: color.withOpacity(0.1),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  icon,
                  size: 32,
                  color: color,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                title,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                subtitle,
                style: TextStyle(
                  fontSize: 14,
                  color: ThemeConfig.lightTextColor,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}