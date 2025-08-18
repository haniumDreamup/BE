class AppConstants {
  // App Info
  static const String appName = 'BIF-AI';
  static const String appDescription = '일상의 도우미';
  
  // Dimensions
  static const double minTouchTarget = 48.0; // 최소 터치 영역
  static const double defaultPadding = 16.0;
  static const double smallPadding = 8.0;
  static const double largePadding = 24.0;
  
  // Text Sizes
  static const double headingTextSize = 24.0;
  static const double subheadingTextSize = 20.0;
  static const double bodyTextSize = 16.0;
  static const double captionTextSize = 14.0;
  
  // Animation Durations
  static const Duration shortAnimation = Duration(milliseconds: 200);
  static const Duration mediumAnimation = Duration(milliseconds: 400);
  static const Duration longAnimation = Duration(milliseconds: 600);
  
  // Timeouts
  static const Duration apiTimeout = Duration(seconds: 30);
  static const Duration cacheExpiry = Duration(hours: 1);
  
  // Pagination
  static const int defaultPageSize = 20;
  static const int maxPageSize = 50;
  
  // Cognitive Level
  static const String cognitiveModerate = 'MODERATE';
  static const String cognitiveMild = 'MILD';
  
  // Korean Messages
  static const String welcomeMessage = '안녕하세요!';
  static const String loadingMessage = '잠시만 기다려주세요...';
  static const String errorMessage = '문제가 발생했어요';
  static const String retryMessage = '다시 시도해주세요';
  static const String successMessage = '완료되었어요!';
}