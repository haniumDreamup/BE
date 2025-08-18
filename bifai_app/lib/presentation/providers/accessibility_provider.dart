import 'package:flutter/material.dart';
import '../../core/services/tts_service.dart';

/// 접근성 기능 관리 Provider
/// BIF 사용자를 위한 음성 안내, 큰 글씨, 고대비 모드 등 관리
class AccessibilityProvider extends ChangeNotifier {
  final TtsService _ttsService = TtsService();
  
  // 접근성 설정
  bool _voiceGuidanceEnabled = true;
  bool _largeTextEnabled = false;
  bool _highContrastEnabled = false;
  bool _simplifiedUIEnabled = true;
  bool _vibrationEnabled = true;
  bool _autoReadScreenEnabled = false;
  
  // 음성 설정
  double _speechRate = 0.4;
  double _volume = 1.0;
  double _pitch = 1.0;
  
  // 글꼴 크기 배율
  double _textScaleFactor = 1.0;
  
  AccessibilityProvider() {
    _initialize();
  }
  
  Future<void> _initialize() async {
    await _ttsService.initialize();
    // TODO: SharedPreferences에서 설정 불러오기
  }
  
  // Getters
  bool get voiceGuidanceEnabled => _voiceGuidanceEnabled;
  bool get largeTextEnabled => _largeTextEnabled;
  bool get highContrastEnabled => _highContrastEnabled;
  bool get simplifiedUIEnabled => _simplifiedUIEnabled;
  bool get vibrationEnabled => _vibrationEnabled;
  bool get autoReadScreenEnabled => _autoReadScreenEnabled;
  double get speechRate => _speechRate;
  double get volume => _volume;
  double get pitch => _pitch;
  double get textScaleFactor => _textScaleFactor;
  
  /// 음성 안내 활성화/비활성화
  Future<void> setVoiceGuidanceEnabled(bool enabled) async {
    _voiceGuidanceEnabled = enabled;
    await _ttsService.setEnabled(enabled);
    notifyListeners();
  }
  
  /// 큰 글씨 모드 설정
  void setLargeTextEnabled(bool enabled) {
    _largeTextEnabled = enabled;
    _textScaleFactor = enabled ? 1.3 : 1.0;
    
    if (_voiceGuidanceEnabled) {
      _ttsService.speak(enabled ? "큰 글씨로 바꿨어요" : "보통 글씨로 바꿨어요");
    }
    
    notifyListeners();
  }
  
  /// 고대비 모드 설정
  void setHighContrastEnabled(bool enabled) {
    _highContrastEnabled = enabled;
    
    if (_voiceGuidanceEnabled) {
      _ttsService.speak(enabled ? "선명한 색으로 바꿨어요" : "보통 색으로 바꿨어요");
    }
    
    notifyListeners();
  }
  
  /// 단순화된 UI 설정
  void setSimplifiedUIEnabled(bool enabled) {
    _simplifiedUIEnabled = enabled;
    
    if (_voiceGuidanceEnabled) {
      _ttsService.speak(enabled ? "쉬운 화면으로 바꿨어요" : "모든 기능을 볼 수 있어요");
    }
    
    notifyListeners();
  }
  
  /// 진동 피드백 설정
  void setVibrationEnabled(bool enabled) {
    _vibrationEnabled = enabled;
    
    if (_voiceGuidanceEnabled) {
      _ttsService.speak(enabled ? "진동을 켰어요" : "진동을 껐어요");
    }
    
    notifyListeners();
  }
  
  /// 화면 자동 읽기 설정
  void setAutoReadScreenEnabled(bool enabled) {
    _autoReadScreenEnabled = enabled;
    
    if (_voiceGuidanceEnabled) {
      _ttsService.speak(enabled ? "화면을 자동으로 읽어드릴게요" : "자동 읽기를 껐어요");
    }
    
    notifyListeners();
  }
  
  /// 음성 속도 설정
  Future<void> setSpeechRate(double rate) async {
    _speechRate = rate;
    await _ttsService.setSpeechRate(rate);
    notifyListeners();
  }
  
  /// 음량 설정
  Future<void> setVolume(double volume) async {
    _volume = volume;
    await _ttsService.setVolume(volume);
    notifyListeners();
  }
  
  /// 음성 톤 설정
  Future<void> setPitch(double pitch) async {
    _pitch = pitch;
    await _ttsService.setPitch(pitch);
    notifyListeners();
  }
  
  /// 텍스트 크기 설정
  void setTextScaleFactor(double factor) {
    _textScaleFactor = factor.clamp(0.8, 2.0);
    _largeTextEnabled = _textScaleFactor > 1.2;
    
    if (_voiceGuidanceEnabled) {
      if (_textScaleFactor > 1.5) {
        _ttsService.speak("아주 큰 글씨예요");
      } else if (_textScaleFactor > 1.2) {
        _ttsService.speak("큰 글씨예요");
      } else if (_textScaleFactor < 0.9) {
        _ttsService.speak("작은 글씨예요");
      } else {
        _ttsService.speak("보통 글씨예요");
      }
    }
    
    notifyListeners();
  }
  
  /// 음성으로 텍스트 읽기
  Future<void> speak(String text, {bool priority = false}) async {
    if (_voiceGuidanceEnabled) {
      await _ttsService.speak(text, priority: priority);
    }
  }
  
  /// 중요 알림 읽기
  Future<void> speakImportant(String text) async {
    if (_voiceGuidanceEnabled) {
      await _ttsService.speakImportant(text);
    }
  }
  
  /// 버튼 클릭 피드백
  Future<void> speakButtonFeedback(String buttonName) async {
    if (_voiceGuidanceEnabled) {
      await _ttsService.speakButtonFeedback(buttonName);
    }
  }
  
  /// 화면 내용 읽기
  Future<void> speakScreenContent(String title, String content) async {
    if (_voiceGuidanceEnabled && _autoReadScreenEnabled) {
      await _ttsService.speakScreenContent(title, content);
    }
  }
  
  /// 네비게이션 안내
  Future<void> speakNavigation(String instruction) async {
    if (_voiceGuidanceEnabled) {
      await _ttsService.speakNavigation(instruction);
    }
  }
  
  /// 성공 메시지
  Future<void> speakSuccess(String action) async {
    if (_voiceGuidanceEnabled) {
      await _ttsService.speakSuccess(action);
    }
  }
  
  /// 에러 메시지
  Future<void> speakError(String error) async {
    if (_voiceGuidanceEnabled) {
      await _ttsService.speakError(error);
    }
  }
  
  /// 음성 중단
  Future<void> stopSpeaking() async {
    await _ttsService.stop();
  }
  
  /// 모든 설정 초기화
  Future<void> resetSettings() async {
    _voiceGuidanceEnabled = true;
    _largeTextEnabled = false;
    _highContrastEnabled = false;
    _simplifiedUIEnabled = true;
    _vibrationEnabled = true;
    _autoReadScreenEnabled = false;
    _speechRate = 0.4;
    _volume = 1.0;
    _pitch = 1.0;
    _textScaleFactor = 1.0;
    
    await _ttsService.setSpeechRate(_speechRate);
    await _ttsService.setVolume(_volume);
    await _ttsService.setPitch(_pitch);
    await _ttsService.setEnabled(_voiceGuidanceEnabled);
    
    if (_voiceGuidanceEnabled) {
      await _ttsService.speak("설정을 처음으로 되돌렸어요");
    }
    
    notifyListeners();
  }
  
  @override
  void dispose() {
    _ttsService.dispose();
    super.dispose();
  }
}