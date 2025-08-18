import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:provider/provider.dart';
import 'package:timezone/data/latest.dart' as tz;
import 'core/config/theme_config.dart';
import 'core/config/toss_theme.dart';
import 'core/services/offline_service.dart';
import 'core/services/notification_service.dart';
import 'core/services/tts_service.dart';
import 'presentation/routes/app_router.dart';
import 'presentation/providers/auth_provider.dart';
import 'presentation/providers/accessibility_provider.dart';
import 'presentation/widgets/common/network_status_banner.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Load environment variables
  await dotenv.load(fileName: ".env");
  
  // Initialize timezone
  tz.initializeTimeZones();
  
  // Initialize offline service
  final offlineService = OfflineService();
  await offlineService.initialize();
  
  // Initialize notification service (Firebase 설정 후 활성화)
  // final notificationService = NotificationService();
  // await notificationService.initialize();
  
  // Initialize TTS service
  final ttsService = TtsService();
  await ttsService.initialize();
  
  runApp(const BifAiApp());
}

class BifAiApp extends StatelessWidget {
  const BifAiApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ScreenUtilInit(
      designSize: const Size(375, 812), // iPhone X size
      minTextAdapt: true,
      splitScreenMode: true,
      builder: (context, child) {
        return MultiProvider(
          providers: [
            ChangeNotifierProvider(create: (_) => AuthProvider()),
            ChangeNotifierProvider(create: (_) => AccessibilityProvider()),
          ],
          child: NetworkStatusBanner(
            child: MaterialApp.router(
              title: 'BIF-AI',
              theme: TossTheme.lightTheme, // 토스 테마 적용
              routerConfig: AppRouter.router,
              debugShowCheckedModeBanner: false,
            ),
          ),
        );
      },
    );
  }
}