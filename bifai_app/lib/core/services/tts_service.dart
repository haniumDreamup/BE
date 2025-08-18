import 'dart:io';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// 음성 안내 서비스 (Text-to-Speech)
/// BIF 사용자를 위한 간단하고 명확한 음성 안내 제공
class TtsService {
  static final TtsService _instance = TtsService._internal();
  factory TtsService() => _instance;
  TtsService._internal();

  final FlutterTts _flutterTts = FlutterTts();
  bool _isInitialized = false;
  bool _isEnabled = true;
  bool _isSpeaking = false;
  
  // 음성 설정
  double _speechRate = 0.4; // 느린 속도 (0.0 ~ 1.0)
  double _volume = 1.0; // 최대 음량
  double _pitch = 1.0; // 일반 톤
  
  Future<void> initialize() async {
    if (_isInitialized) return;
    
    try {
      // 한국어 설정
      await _flutterTts.setLanguage("ko-KR");
      
      // iOS/Android 플랫폼별 설정
      if (Platform.isIOS) {
        await _flutterTts.setSharedInstance(true);
        await _flutterTts.setIosAudioCategory(
          IosTextToSpeechAudioCategory.playback,
          [
            IosTextToSpeechAudioCategoryOptions.allowBluetooth,
            IosTextToSpeechAudioCategoryOptions.allowBluetoothA2DP,
            IosTextToSpeechAudioCategoryOptions.mixWithOthers,
          ],
        );
      }
      
      // 음성 속도, 볼륨, 피치 설정
      await _flutterTts.setSpeechRate(_speechRate);
      await _flutterTts.setVolume(_volume);
      await _flutterTts.setPitch(_pitch);
      
      // 이벤트 리스너 설정
      _flutterTts.setStartHandler(() {
        _isSpeaking = true;
      });
      
      _flutterTts.setCompletionHandler(() {
        _isSpeaking = false;
      });
      
      _flutterTts.setErrorHandler((msg) {
        _isSpeaking = false;
        print('TTS 에러: $msg');
      });
      
      // 저장된 설정 불러오기
      await _loadSettings();
      
      _isInitialized = true;
      print('음성 안내 서비스 초기화 완료');
    } catch (e) {
      print('음성 안내 서비스 초기화 실패: $e');
    }
  }
  
  /// 텍스트를 음성으로 읽기
  Future<void> speak(String text, {bool priority = false}) async {
    if (!_isInitialized) await initialize();
    if (!_isEnabled) return;
    
    // 우선순위가 높으면 현재 음성 중단
    if (priority && _isSpeaking) {
      await stop();
    }
    
    // 이미 말하고 있으면 대기
    if (_isSpeaking && !priority) {
      return;
    }
    
    try {
      // BIF 사용자를 위한 텍스트 단순화
      String simplifiedText = _simplifyText(text);
      
      // 문장 사이에 잠시 멈춤 추가
      simplifiedText = simplifiedText.replaceAll('. ', '... ');
      
      await _flutterTts.speak(simplifiedText);
    } catch (e) {
      print('음성 출력 실패: $e');
    }
  }
  
  /// 중요 알림 음성 (즉시 재생)
  Future<void> speakImportant(String text) async {
    await speak(text, priority: true);
  }
  
  /// 약물 복용 알림
  Future<void> speakMedicationReminder(String medicationName, String time) async {
    String message = "$medicationName 드실 시간이에요. $time에 드셔야 해요.";
    await speakImportant(message);
  }
  
  /// 일정 알림
  Future<void> speakScheduleReminder(String scheduleName, String time, String? location) async {
    String message = "$scheduleName 시간이에요. $time에 시작해요.";
    if (location != null && location.isNotEmpty) {
      message += " 장소는 $location 이에요.";
    }
    await speakImportant(message);
  }
  
  /// 네비게이션 안내
  Future<void> speakNavigation(String instruction) async {
    await speak(instruction, priority: true);
  }
  
  /// 버튼 클릭 피드백
  Future<void> speakButtonFeedback(String buttonName) async {
    await speak("$buttonName 버튼을 눌렀어요");
  }
  
  /// 성공 메시지
  Future<void> speakSuccess(String action) async {
    await speak("$action 완료했어요. 잘하셨어요!");
  }
  
  /// 에러 메시지
  Future<void> speakError(String error) async {
    String simpleError = _simplifyError(error);
    await speak("문제가 생겼어요. $simpleError");
  }
  
  /// 도움말 읽기
  Future<void> speakHelp(String helpText) async {
    await speak("도움말을 읽어드릴게요. $helpText");
  }
  
  /// 화면 내용 읽기
  Future<void> speakScreenContent(String title, String content) async {
    await speak("$title 화면이에요. $content");
  }
  
  /// 음성 중단
  Future<void> stop() async {
    if (_isSpeaking) {
      await _flutterTts.stop();
      _isSpeaking = false;
    }
  }
  
  /// 일시 정지
  Future<void> pause() async {
    if (_isSpeaking) {
      await _flutterTts.pause();
    }
  }
  
  /// 음성 안내 활성화/비활성화
  Future<void> setEnabled(bool enabled) async {
    _isEnabled = enabled;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('tts_enabled', enabled);
    
    if (enabled) {
      await speak("음성 안내를 켰어요");
    }
  }
  
  /// 음성 속도 설정 (0.1 ~ 1.0)
  Future<void> setSpeechRate(double rate) async {
    _speechRate = rate.clamp(0.1, 1.0);
    await _flutterTts.setSpeechRate(_speechRate);
    
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble('tts_speech_rate', _speechRate);
    
    await speak("음성 속도를 변경했어요");
  }
  
  /// 음량 설정 (0.0 ~ 1.0)
  Future<void> setVolume(double volume) async {
    _volume = volume.clamp(0.0, 1.0);
    await _flutterTts.setVolume(_volume);
    
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble('tts_volume', _volume);
  }
  
  /// 음성 톤 설정 (0.5 ~ 2.0)
  Future<void> setPitch(double pitch) async {
    _pitch = pitch.clamp(0.5, 2.0);
    await _flutterTts.setPitch(_pitch);
    
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble('tts_pitch', _pitch);
  }
  
  /// 사용 가능한 음성 목록 가져오기
  Future<List<String>> getAvailableVoices() async {
    try {
      final voices = await _flutterTts.getVoices;
      return voices
          .where((voice) => voice['locale'] == 'ko-KR')
          .map<String>((voice) => voice['name'] as String)
          .toList();
    } catch (e) {
      print('음성 목록 가져오기 실패: $e');
      return [];
    }
  }
  
  /// 특정 음성 선택
  Future<void> setVoice(String voiceName) async {
    try {
      await _flutterTts.setVoice({"name": voiceName, "locale": "ko-KR"});
      
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('tts_voice', voiceName);
    } catch (e) {
      print('음성 설정 실패: $e');
    }
  }
  
  /// 설정 불러오기
  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    
    _isEnabled = prefs.getBool('tts_enabled') ?? true;
    _speechRate = prefs.getDouble('tts_speech_rate') ?? 0.4;
    _volume = prefs.getDouble('tts_volume') ?? 1.0;
    _pitch = prefs.getDouble('tts_pitch') ?? 1.0;
    
    await _flutterTts.setSpeechRate(_speechRate);
    await _flutterTts.setVolume(_volume);
    await _flutterTts.setPitch(_pitch);
    
    final voiceName = prefs.getString('tts_voice');
    if (voiceName != null) {
      await setVoice(voiceName);
    }
  }
  
  /// 텍스트 단순화 (BIF 사용자를 위한)
  String _simplifyText(String text) {
    // 어려운 단어를 쉬운 단어로 변경
    text = text.replaceAll('설정', '세팅');
    text = text.replaceAll('완료', '끝');
    text = text.replaceAll('실패', '안 됨');
    text = text.replaceAll('성공', '됨');
    text = text.replaceAll('확인', '네');
    text = text.replaceAll('취소', '아니요');
    
    // 긴 문장 단순화
    if (text.length > 30) {
      // 문장을 짧게 나누기
      text = text.replaceAll(', ', '. ');
    }
    
    return text;
  }
  
  /// 에러 메시지 단순화
  String _simplifyError(String error) {
    if (error.contains('network') || error.contains('인터넷')) {
      return "인터넷이 안 돼요";
    } else if (error.contains('permission') || error.contains('권한')) {
      return "허락이 필요해요";
    } else if (error.contains('not found') || error.contains('찾을 수 없')) {
      return "찾을 수 없어요";
    } else if (error.contains('timeout') || error.contains('시간')) {
      return "시간이 너무 오래 걸려요";
    } else {
      return "다시 해보세요";
    }
  }
  
  /// 현재 상태 가져오기
  bool get isEnabled => _isEnabled;
  bool get isSpeaking => _isSpeaking;
  double get speechRate => _speechRate;
  double get volume => _volume;
  double get pitch => _pitch;
  
  /// 리소스 정리
  void dispose() {
    stop();
    _isInitialized = false;
  }
}