import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// 토스 스타일 테마 설정
/// 미니멀하고 깔끔한 디자인 with BIF 접근성
class TossTheme {
  // 토스 컬러 시스템
  static const Color tossBlue = Color(0xFF0064FF);
  static const Color tossBlack = Color(0xFF1A1A1A);
  static const Color tossGray900 = Color(0xFF191F28);
  static const Color tossGray800 = Color(0xFF333D4B);
  static const Color tossGray700 = Color(0xFF4E5968);
  static const Color tossGray600 = Color(0xFF6B7684);
  static const Color tossGray500 = Color(0xFF8B95A1);
  static const Color tossGray400 = Color(0xFFB0B8C1);
  static const Color tossGray300 = Color(0xFFD1D6DB);
  static const Color tossGray200 = Color(0xFFE5E8EB);
  static const Color tossGray100 = Color(0xFFF2F4F6);
  static const Color tossGray50 = Color(0xFFF9FAFB);
  static const Color tossWhite = Color(0xFFFFFFFF);
  
  // 시맨틱 컬러
  static const Color tossRed = Color(0xFFF04452);
  static const Color tossGreen = Color(0xFF00C896);
  static const Color tossYellow = Color(0xFFFFB800);
  static const Color tossPurple = Color(0xFF8B5CF6);
  
  // 토스 스타일 그림자
  static List<BoxShadow> tossShadowSmall = [
    BoxShadow(
      color: Colors.black.withOpacity(0.04),
      blurRadius: 4,
      offset: const Offset(0, 2),
    ),
  ];
  
  static List<BoxShadow> tossShadowMedium = [
    BoxShadow(
      color: Colors.black.withOpacity(0.08),
      blurRadius: 16,
      offset: const Offset(0, 8),
    ),
  ];
  
  static List<BoxShadow> tossShadowLarge = [
    BoxShadow(
      color: Colors.black.withOpacity(0.12),
      blurRadius: 32,
      offset: const Offset(0, 16),
    ),
  ];
  
  // 토스 스타일 테마
  static ThemeData get lightTheme => ThemeData(
    useMaterial3: true,
    fontFamily: 'Pretendard', // 토스 폰트 (설치 필요)
    
    // 컬러 스킴
    colorScheme: const ColorScheme.light(
      primary: tossBlue,
      onPrimary: tossWhite,
      secondary: tossGray700,
      onSecondary: tossWhite,
      surface: tossWhite,
      onSurface: tossGray900,
      background: tossGray50,
      onBackground: tossGray900,
      error: tossRed,
      onError: tossWhite,
    ),
    
    // 앱바 테마
    appBarTheme: AppBarTheme(
      elevation: 0,
      scrolledUnderElevation: 0,
      systemOverlayStyle: SystemUiOverlayStyle.dark,
      backgroundColor: tossWhite,
      foregroundColor: tossGray900,
      centerTitle: false,
      titleTextStyle: const TextStyle(
        color: tossGray900,
        fontSize: 20,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.5,
      ),
      iconTheme: const IconThemeData(
        color: tossGray900,
        size: 24,
      ),
    ),
    
    // 카드 테마
    cardTheme: CardThemeData(
      elevation: 0,
      color: tossWhite,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
      ),
      clipBehavior: Clip.antiAliasWithSaveLayer,
    ),
    
    // 버튼 테마
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        elevation: 0,
        backgroundColor: tossBlue,
        foregroundColor: tossWhite,
        minimumSize: const Size(double.infinity, 56),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        textStyle: const TextStyle(
          fontSize: 17,
          fontWeight: FontWeight.w600,
          letterSpacing: -0.3,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      ),
    ),
    
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: tossBlue,
        textStyle: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          letterSpacing: -0.3,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      ),
    ),
    
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: tossGray700,
        side: const BorderSide(color: tossGray200, width: 1.5),
        minimumSize: const Size(double.infinity, 56),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
        textStyle: const TextStyle(
          fontSize: 17,
          fontWeight: FontWeight.w600,
          letterSpacing: -0.3,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      ),
    ),
    
    // 텍스트 테마
    textTheme: const TextTheme(
      // Display
      displayLarge: TextStyle(
        fontSize: 32,
        fontWeight: FontWeight.w700,
        letterSpacing: -1.0,
        color: tossGray900,
        height: 1.3,
      ),
      displayMedium: TextStyle(
        fontSize: 28,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.8,
        color: tossGray900,
        height: 1.3,
      ),
      displaySmall: TextStyle(
        fontSize: 24,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.6,
        color: tossGray900,
        height: 1.3,
      ),
      
      // Headline
      headlineLarge: TextStyle(
        fontSize: 22,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.5,
        color: tossGray900,
        height: 1.4,
      ),
      headlineMedium: TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.4,
        color: tossGray900,
        height: 1.4,
      ),
      headlineSmall: TextStyle(
        fontSize: 18,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.3,
        color: tossGray900,
        height: 1.4,
      ),
      
      // Title
      titleLarge: TextStyle(
        fontSize: 17,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.3,
        color: tossGray900,
        height: 1.5,
      ),
      titleMedium: TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.2,
        color: tossGray900,
        height: 1.5,
      ),
      titleSmall: TextStyle(
        fontSize: 15,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.1,
        color: tossGray900,
        height: 1.5,
      ),
      
      // Body
      bodyLarge: TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w400,
        letterSpacing: -0.2,
        color: tossGray700,
        height: 1.6,
      ),
      bodyMedium: TextStyle(
        fontSize: 15,
        fontWeight: FontWeight.w400,
        letterSpacing: -0.1,
        color: tossGray700,
        height: 1.6,
      ),
      bodySmall: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w400,
        letterSpacing: 0,
        color: tossGray600,
        height: 1.6,
      ),
      
      // Label
      labelLarge: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w500,
        letterSpacing: 0,
        color: tossGray600,
        height: 1.4,
      ),
      labelMedium: TextStyle(
        fontSize: 13,
        fontWeight: FontWeight.w500,
        letterSpacing: 0,
        color: tossGray600,
        height: 1.4,
      ),
      labelSmall: TextStyle(
        fontSize: 12,
        fontWeight: FontWeight.w500,
        letterSpacing: 0.1,
        color: tossGray500,
        height: 1.4,
      ),
    ),
    
    // Input Decoration Theme
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: tossGray50,
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: BorderSide.none,
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: BorderSide.none,
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: tossBlue, width: 2),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: tossRed, width: 1.5),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: tossRed, width: 2),
      ),
      labelStyle: const TextStyle(
        fontSize: 15,
        fontWeight: FontWeight.w500,
        color: tossGray600,
      ),
      hintStyle: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w400,
        color: tossGray400,
      ),
      errorStyle: const TextStyle(
        fontSize: 13,
        fontWeight: FontWeight.w500,
        color: tossRed,
      ),
    ),
    
    // 바텀시트 테마
    bottomSheetTheme: const BottomSheetThemeData(
      backgroundColor: tossWhite,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      dragHandleColor: tossGray300,
    ),
    
    // 스낵바 테마
    snackBarTheme: SnackBarThemeData(
      backgroundColor: tossGray800,
      contentTextStyle: const TextStyle(
        color: tossWhite,
        fontSize: 15,
        fontWeight: FontWeight.w500,
      ),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
      behavior: SnackBarBehavior.floating,
      elevation: 0,
    ),
    
    // Divider 테마
    dividerTheme: const DividerThemeData(
      color: tossGray100,
      thickness: 1,
      space: 1,
    ),
    
    // Scaffold Background
    scaffoldBackgroundColor: tossGray50,
  );
  
  // 토스 스타일 애니메이션 커브
  static const Curve tossAnimationCurve = Curves.easeInOutCubic;
  static const Duration tossAnimationDuration = Duration(milliseconds: 300);
  static const Duration tossAnimationDurationFast = Duration(milliseconds: 200);
  static const Duration tossAnimationDurationSlow = Duration(milliseconds: 500);
}