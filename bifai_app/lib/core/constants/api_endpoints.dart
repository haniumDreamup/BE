class ApiEndpoints {
  // Base paths
  static const String basePath = '/mobile';
  
  // Auth endpoints
  static const String login = '$basePath/auth/login';
  static const String logout = '$basePath/auth/logout';
  static const String refresh = '$basePath/auth/refresh';
  static const String register = '$basePath/auth/register';
  
  // Home endpoints
  static const String dashboard = '$basePath/home/dashboard';
  
  // Medication endpoints
  static const String medicationsToday = '$basePath/medications/today';
  static const String medicationDetail = '$basePath/medications';
  static const String takeMedication = '$basePath/medications/{id}/take';
  
  // Schedule endpoints
  static const String schedulesToday = '$basePath/schedules/today';
  static const String scheduleDetail = '$basePath/schedules';
  static const String completeSchedule = '$basePath/schedules/{id}/complete';
  
  // Notification endpoints
  static const String registerToken = '$basePath/notifications/register';
  static const String notificationSettings = '$basePath/notifications/settings';
  static const String notificationHistory = '$basePath/notifications/history';
  
  // Guardian endpoints
  static const String guardians = '$basePath/guardians';
  static const String emergency = '$basePath/guardians/emergency';
  
  // Location endpoints
  static const String updateLocation = '$basePath/location/update';
  static const String safeZones = '$basePath/location/safe-zones';
  
  // Activity endpoints
  static const String recentActivities = '$basePath/activities/recent';
  
  // Media endpoints
  static const String prepareUpload = '$basePath/media/upload/prepare';
  static const String completeUpload = '$basePath/media/upload/complete';
  
  // User endpoints
  static const String userProfile = '$basePath/user/profile';
  static const String updateProfile = '$basePath/user/profile';
}