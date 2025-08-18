import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../../../core/config/toss_theme.dart';
import '../../providers/accessibility_provider.dart';

class TossHomeScreen extends StatefulWidget {
  const TossHomeScreen({Key? key}) : super(key: key);

  @override
  State<TossHomeScreen> createState() => _TossHomeScreenState();
}

class _TossHomeScreenState extends State<TossHomeScreen> 
    with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  
  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: TossTheme.tossAnimationDuration,
    );
    _animationController.forward();
  }
  
  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }
  
  @override
  Widget build(BuildContext context) {
    final accessibility = context.watch<AccessibilityProvider>();
    
    return Scaffold(
      backgroundColor: TossTheme.tossGray50,
      body: SafeArea(
        child: CustomScrollView(
          physics: const BouncingScrollPhysics(),
          slivers: [
            // 앱바
            SliverAppBar(
              backgroundColor: TossTheme.tossGray50,
              elevation: 0,
              pinned: true,
              expandedHeight: 120,
              flexibleSpace: FlexibleSpaceBar(
                titlePadding: const EdgeInsets.only(left: 24, bottom: 16),
                title: Column(
                  mainAxisAlignment: MainAxisAlignment.end,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _getGreeting(),
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                        color: TossTheme.tossGray500,
                      ),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      '홍길동님',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.w700,
                        color: TossTheme.tossGray900,
                      ),
                    ),
                  ],
                ),
                background: Container(
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [
                        TossTheme.tossWhite,
                        TossTheme.tossGray50,
                      ],
                    ),
                  ),
                ),
              ),
              actions: [
                _buildAppBarAction(
                  icon: Icons.radar_rounded,
                  onTap: () {
                    accessibility.speak('네트워크 디바이스 검색 화면으로 이동합니다');
                    context.push('/network-discovery');
                  },
                ),
                const SizedBox(width: 8),
                _buildAppBarAction(
                  icon: Icons.notifications_none_rounded,
                  onTap: () {
                    accessibility.speak('알림 화면으로 이동합니다');
                  },
                ),
                const SizedBox(width: 8),
              ],
            ),
            
            // 컨텐츠
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              sliver: SliverList(
                delegate: SliverChildListDelegate([
                  // 오늘의 요약 카드
                  _buildTodaySummaryCard(context),
                  const SizedBox(height: 24),
                  
                  // 빠른 실행 섹션
                  _buildSectionTitle('빠른 실행'),
                  const SizedBox(height: 16),
                  _buildQuickActions(context, accessibility),
                  const SizedBox(height: 32),
                  
                  // 다음 일정 섹션
                  _buildSectionTitle('다음 일정'),
                  const SizedBox(height: 16),
                  _buildNextScheduleCard(context),
                  const SizedBox(height: 32),
                  
                  // 건강 상태 섹션
                  _buildSectionTitle('오늘의 건강'),
                  const SizedBox(height: 16),
                  _buildHealthStatusCard(context),
                  const SizedBox(height: 32),
                ]),
              ),
            ),
          ],
        ),
      ),
      
      // 토스 스타일 플로팅 버튼
      floatingActionButton: _buildFloatingButton(context, accessibility),
    );
  }
  
  String _getGreeting() {
    final hour = DateTime.now().hour;
    if (hour < 12) return '좋은 아침이에요';
    if (hour < 18) return '좋은 오후예요';
    return '좋은 저녁이에요';
  }
  
  Widget _buildAppBarAction({
    required IconData icon,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      borderRadius: BorderRadius.circular(12),
      child: Container(
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: TossTheme.tossWhite,
          borderRadius: BorderRadius.circular(12),
          boxShadow: TossTheme.tossShadowSmall,
        ),
        child: Icon(icon, color: TossTheme.tossGray700, size: 24),
      ),
    );
  }
  
  Widget _buildTodaySummaryCard(BuildContext context) {
    return AnimatedBuilder(
      animation: _animationController,
      builder: (context, child) {
        return SlideTransition(
          position: Tween<Offset>(
            begin: const Offset(0, 0.1),
            end: Offset.zero,
          ).animate(CurvedAnimation(
            parent: _animationController,
            curve: TossTheme.tossAnimationCurve,
          )),
          child: FadeTransition(
            opacity: _animationController,
            child: Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: TossTheme.tossWhite,
                borderRadius: BorderRadius.circular(24),
                boxShadow: TossTheme.tossShadowMedium,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 48,
                        height: 48,
                        decoration: BoxDecoration(
                          color: TossTheme.tossBlue.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: const Icon(
                          Icons.calendar_today_rounded,
                          color: TossTheme.tossBlue,
                          size: 24,
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '${DateTime.now().month}월 ${DateTime.now().day}일 ${_getDayOfWeek()}',
                              style: const TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w500,
                                color: TossTheme.tossGray500,
                              ),
                            ),
                            const SizedBox(height: 4),
                            const Text(
                              '오늘 할 일 3개',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w700,
                                color: TossTheme.tossGray900,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  const Divider(height: 1),
                  const SizedBox(height: 20),
                  _buildSummaryItem(
                    icon: Icons.medication_rounded,
                    title: '약 복용',
                    value: '2/3',
                    color: TossTheme.tossGreen,
                  ),
                  const SizedBox(height: 12),
                  _buildSummaryItem(
                    icon: Icons.event_rounded,
                    title: '일정',
                    value: '1개 남음',
                    color: TossTheme.tossPurple,
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
  
  Widget _buildSummaryItem({
    required IconData icon,
    required String title,
    required String value,
    required Color color,
  }) {
    return Row(
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: color, size: 20),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            title,
            style: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w500,
              color: TossTheme.tossGray700,
            ),
          ),
        ),
        Text(
          value,
          style: const TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: TossTheme.tossGray900,
          ),
        ),
      ],
    );
  }
  
  Widget _buildSectionTitle(String title) {
    return Text(
      title,
      style: const TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.w700,
        color: TossTheme.tossGray900,
      ),
    );
  }
  
  Widget _buildQuickActions(BuildContext context, AccessibilityProvider accessibility) {
    return Container(
      decoration: BoxDecoration(
        color: TossTheme.tossWhite,
        borderRadius: BorderRadius.circular(20),
        boxShadow: TossTheme.tossShadowSmall,
      ),
      child: Column(
        children: [
          _buildQuickActionItem(
            icon: Icons.medication_liquid_rounded,
            title: '약 먹기',
            subtitle: '아침 약 8:00',
            color: TossTheme.tossBlue,
            onTap: () {
              accessibility.speak('약 복용 화면으로 이동합니다');
              context.push('/medications');
            },
          ),
          const Divider(height: 1, indent: 68),
          _buildQuickActionItem(
            icon: Icons.emergency_rounded,
            title: 'SOS',
            subtitle: '긴급 도움 요청',
            color: TossTheme.tossRed,
            onTap: () {
              accessibility.speakImportant('긴급 도움을 요청합니다');
              context.push('/emergency');
            },
          ),
          const Divider(height: 1, indent: 68),
          _buildQuickActionItem(
            icon: Icons.phone_in_talk_rounded,
            title: '보호자 연락',
            subtitle: '김보호 (아들)',
            color: TossTheme.tossGreen,
            onTap: () {
              accessibility.speak('보호자에게 전화를 걸겠습니다');
            },
          ),
        ],
      ),
    );
  }
  
  Widget _buildQuickActionItem({
    required IconData icon,
    required String title,
    required String subtitle,
    required Color color,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      borderRadius: BorderRadius.circular(20),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(icon, color: color, size: 24),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: TossTheme.tossGray900,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w400,
                      color: TossTheme.tossGray500,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(
              Icons.chevron_right_rounded,
              color: TossTheme.tossGray400,
              size: 24,
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildNextScheduleCard(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            TossTheme.tossPurple.withOpacity(0.1),
            TossTheme.tossPurple.withOpacity(0.05),
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: TossTheme.tossPurple.withOpacity(0.2),
          width: 1,
        ),
      ),
      child: Row(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: TossTheme.tossWhite,
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  '14:00',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: TossTheme.tossPurple,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '병원 진료',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w600,
                    color: TossTheme.tossGray900,
                  ),
                ),
                SizedBox(height: 4),
                Text(
                  '서울대병원 신경과',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w400,
                    color: TossTheme.tossGray600,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
  
  Widget _buildHealthStatusCard(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: TossTheme.tossWhite,
        borderRadius: BorderRadius.circular(20),
        boxShadow: TossTheme.tossShadowSmall,
      ),
      child: Column(
        children: [
          _buildHealthItem(
            title: '복약 순응도',
            value: '92%',
            icon: Icons.trending_up_rounded,
            color: TossTheme.tossGreen,
            isGood: true,
          ),
          const Divider(height: 1, indent: 20, endIndent: 20),
          _buildHealthItem(
            title: '오늘 걸음 수',
            value: '3,420',
            icon: Icons.directions_walk_rounded,
            color: TossTheme.tossBlue,
            isGood: true,
          ),
          const Divider(height: 1, indent: 20, endIndent: 20),
          _buildHealthItem(
            title: '수면 시간',
            value: '7시간 30분',
            icon: Icons.bedtime_rounded,
            color: TossTheme.tossPurple,
            isGood: true,
          ),
        ],
      ),
    );
  }
  
  Widget _buildHealthItem({
    required String title,
    required String value,
    required IconData icon,
    required Color color,
    required bool isGood,
  }) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: color, size: 20),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Text(
              title,
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w500,
                color: TossTheme.tossGray700,
              ),
            ),
          ),
          Text(
            value,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: isGood ? TossTheme.tossGreen : TossTheme.tossGray900,
            ),
          ),
        ],
      ),
    );
  }
  
  Widget _buildFloatingButton(BuildContext context, AccessibilityProvider accessibility) {
    return Container(
      decoration: BoxDecoration(
        boxShadow: TossTheme.tossShadowLarge,
        borderRadius: BorderRadius.circular(20),
      ),
      child: FloatingActionButton.extended(
        onPressed: () {
          HapticFeedback.mediumImpact();
          accessibility.speak('AI 도우미와 대화를 시작합니다');
        },
        backgroundColor: TossTheme.tossBlue,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
        ),
        icon: const Icon(Icons.assistant_rounded, size: 24),
        label: const Text(
          'AI 도우미',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            letterSpacing: -0.3,
          ),
        ),
      ),
    );
  }
  
  String _getDayOfWeek() {
    final days = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];
    return days[DateTime.now().weekday % 7];
  }
}