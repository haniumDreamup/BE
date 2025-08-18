import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/config/theme_config.dart';

class EmergencyScreen extends StatelessWidget {
  const EmergencyScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: ThemeConfig.errorColor,
      appBar: AppBar(
        backgroundColor: ThemeConfig.errorColor,
        foregroundColor: Colors.white,
        title: const Text('긴급 상황'),
        elevation: 0,
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              // Emergency Button
              Expanded(
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Container(
                        width: 200,
                        height: 200,
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(100),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.2),
                              blurRadius: 20,
                              offset: const Offset(0, 10),
                            ),
                          ],
                        ),
                        child: Material(
                          color: Colors.transparent,
                          child: InkWell(
                            onTap: () => _showEmergencyDialog(context),
                            borderRadius: BorderRadius.circular(100),
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: const [
                                Icon(
                                  Icons.sos,
                                  size: 60,
                                  color: ThemeConfig.errorColor,
                                ),
                                SizedBox(height: 8),
                                Text(
                                  'SOS',
                                  style: TextStyle(
                                    fontSize: 32,
                                    fontWeight: FontWeight.bold,
                                    color: ThemeConfig.errorColor,
                                  ),
                                ),
                                Text(
                                  '누르세요',
                                  style: TextStyle(
                                    fontSize: 18,
                                    color: ThemeConfig.errorColor,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 32),
                      const Text(
                        '긴급 상황이면 버튼을 누르세요',
                        style: TextStyle(
                          fontSize: 20,
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        '보호자에게 알림이 갑니다',
                        style: TextStyle(
                          fontSize: 16,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              
              // Quick Contact Options
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.9),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  children: [
                    const Text(
                      '빠른 연락',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        _QuickContactButton(
                          icon: Icons.call,
                          label: '119',
                          color: ThemeConfig.errorColor,
                          onTap: () => _makePhoneCall('119'),
                        ),
                        _QuickContactButton(
                          icon: Icons.local_hospital,
                          label: '병원',
                          color: ThemeConfig.primaryColor,
                          onTap: () => _makePhoneCall('02-1234-5678'),
                        ),
                        _QuickContactButton(
                          icon: Icons.family_restroom,
                          label: '보호자',
                          color: ThemeConfig.successColor,
                          onTap: () => _makePhoneCall('010-1234-5678'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
  
  void _showEmergencyDialog(BuildContext context) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text(
          '긴급 상황',
          style: TextStyle(
            color: ThemeConfig.errorColor,
            fontWeight: FontWeight.bold,
          ),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: const [
            Icon(
              Icons.warning_amber_rounded,
              size: 48,
              color: ThemeConfig.errorColor,
            ),
            SizedBox(height: 16),
            Text(
              '정말 긴급 상황인가요?',
              style: TextStyle(fontSize: 18),
            ),
            SizedBox(height: 8),
            Text(
              '보호자에게 알림이 전송됩니다',
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey,
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text(
              '취소',
              style: TextStyle(fontSize: 16),
            ),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              _sendEmergencyAlert(context);
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: ThemeConfig.errorColor,
            ),
            child: const Text(
              '긴급 알림 보내기',
              style: TextStyle(fontSize: 16),
            ),
          ),
        ],
      ),
    );
  }
  
  void _sendEmergencyAlert(BuildContext context) {
    // TODO: Implement emergency alert
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('긴급 알림을 보냈어요'),
        backgroundColor: ThemeConfig.successColor,
      ),
    );
  }
  
  Future<void> _makePhoneCall(String phoneNumber) async {
    final Uri launchUri = Uri(
      scheme: 'tel',
      path: phoneNumber,
    );
    if (await canLaunchUrl(launchUri)) {
      await launchUrl(launchUri);
    }
  }
}

class _QuickContactButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  const _QuickContactButton({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          padding: const EdgeInsets.all(12),
          child: Column(
            children: [
              Container(
                width: 56,
                height: 56,
                decoration: BoxDecoration(
                  color: color.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(28),
                ),
                child: Icon(
                  icon,
                  color: color,
                  size: 28,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                label,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}