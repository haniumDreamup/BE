import 'dart:async';
import 'dart:io';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:multicast_dns/multicast_dns.dart';
import 'package:bonsoir/bonsoir.dart';
import 'package:network_info_plus/network_info_plus.dart';

/// 네트워크 디스커버리 서비스
/// UDP 브로드캐스트, mDNS, Bonjour/Zeroconf를 사용하여 디바이스 검색
class NetworkDiscoveryService {
  static final NetworkDiscoveryService _instance = NetworkDiscoveryService._internal();
  factory NetworkDiscoveryService() => _instance;
  NetworkDiscoveryService._internal();

  final _discoveredDevices = <DiscoveredDevice>[];
  final _deviceStreamController = StreamController<List<DiscoveredDevice>>.broadcast();
  
  Stream<List<DiscoveredDevice>> get devicesStream => _deviceStreamController.stream;
  List<DiscoveredDevice> get devices => List.unmodifiable(_discoveredDevices);
  
  Timer? _broadcastTimer;
  BonsoirDiscovery? _bonsoirDiscovery;
  RawDatagramSocket? _udpSocket;
  
  // 네트워크 정보
  final _networkInfo = NetworkInfo();
  String? _localIP;
  String? _wifiName;
  
  /// 초기화 및 네트워크 정보 가져오기
  Future<void> initialize() async {
    try {
      _localIP = await _networkInfo.getWifiIP();
      _wifiName = await _networkInfo.getWifiName();
      
      debugPrint('📡 Network Info - IP: $_localIP, WiFi: $_wifiName');
    } catch (e) {
      debugPrint('네트워크 정보 가져오기 실패: $e');
    }
  }
  
  /// UDP 브로드캐스트 리스너 시작
  Future<void> startUdpBroadcastListener({int port = 8888}) async {
    if (kIsWeb) {
      debugPrint('⚠️ UDP 브로드캐스트는 웹에서 지원되지 않습니다');
      // 웹에서는 시뮬레이션 데이터 추가
      _addSimulatedDevices();
      return;
    }
    
    try {
      // UDP 소켓 생성
      _udpSocket = await RawDatagramSocket.bind(InternetAddress.anyIPv4, port);
      
      debugPrint('🎯 UDP 브로드캐스트 리스너 시작 (포트: $port)');
      
      // 브로드캐스트 수신 대기
      _udpSocket!.listen((RawSocketEvent event) {
        if (event == RawSocketEvent.read) {
          final datagram = _udpSocket!.receive();
          if (datagram != null) {
            final message = String.fromCharCodes(datagram.data);
            final senderAddress = datagram.address.address;
            final senderPort = datagram.port;
            
            debugPrint('📨 UDP 메시지 수신: $message from $senderAddress:$senderPort');
            
            // 디바이스 정보 파싱
            _parseAndAddDevice(message, senderAddress, 'UDP');
          }
        }
      });
      
      // 자체 브로드캐스트 전송 (디바이스 알림)
      _startBroadcasting(port);
      
    } catch (e) {
      debugPrint('UDP 브로드캐스트 리스너 시작 실패: $e');
    }
  }
  
  /// 주기적으로 브로드캐스트 전송
  void _startBroadcasting(int port) {
    _broadcastTimer?.cancel();
    _broadcastTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      _sendBroadcast(port);
    });
    
    // 즉시 한 번 전송
    _sendBroadcast(port);
  }
  
  /// UDP 브로드캐스트 전송
  Future<void> _sendBroadcast(int port) async {
    if (kIsWeb) return; // 웹에서는 지원 안함
    
    try {
      final socket = await RawDatagramSocket.bind(InternetAddress.anyIPv4, 0);
      socket.broadcastEnabled = true;
      
      // 디바이스 정보 전송
      final message = 'BIFAI_DEVICE|${_localIP ?? "unknown"}|${Platform.operatingSystem}|BIF-AI 앱';
      final data = message.codeUnits;
      
      // 브로드캐스트 주소로 전송
      socket.send(data, InternetAddress('255.255.255.255'), port);
      
      debugPrint('📤 브로드캐스트 전송: $message');
      
      socket.close();
    } catch (e) {
      debugPrint('브로드캐스트 전송 실패: $e');
    }
  }
  
  /// mDNS/Bonjour 디스커버리 시작
  Future<void> startMdnsDiscovery({String serviceType = '_bifai._tcp'}) async {
    try {
      // Bonjour 디스커버리 생성
      _bonsoirDiscovery = BonsoirDiscovery(type: serviceType);
      
      debugPrint('🔍 mDNS 디스커버리 시작: $serviceType');
      
      // 이벤트 리스너 설정
      _bonsoirDiscovery!.eventStream?.listen((event) {
        // Bonsoir 이벤트 처리
        if (event is BonsoirDiscoveryEvent) {
          final service = event.service;
          if (service != null) {
            debugPrint('🎉 서비스 발견: ${service.name} at ${service.host}:${service.port}');
            
            final device = DiscoveredDevice(
              name: service.name,
              ip: service.host ?? 'unknown',
              port: service.port,
              type: 'mDNS',
              attributes: service.attributes ?? {},
              discoveredAt: DateTime.now(),
            );
            
            _addDevice(device);
          }
        }
      });
      
      // 디스커버리 시작
      await _bonsoirDiscovery!.start();
      
    } catch (e) {
      debugPrint('mDNS 디스커버리 시작 실패: $e');
    }
  }
  
  /// 멀티캐스트 DNS를 사용한 디바이스 검색
  Future<void> startMulticastDnsDiscovery() async {
    try {
      const String name = '_bifai._tcp.local';
      final MDnsClient client = MDnsClient();
      
      debugPrint('🔎 Multicast DNS 검색 시작');
      
      await client.start();
      
      // PTR 레코드 조회
      await for (final PtrResourceRecord ptr in client.lookup<PtrResourceRecord>(
        ResourceRecordQuery.serverPointer(name),
      )) {
        debugPrint('📍 PTR 레코드 발견: ${ptr.domainName}');
        
        // SRV 레코드 조회
        await for (final SrvResourceRecord srv in client.lookup<SrvResourceRecord>(
          ResourceRecordQuery.service(ptr.domainName),
        )) {
          debugPrint('🎯 SRV 레코드: ${srv.target}:${srv.port}');
          
          // A 레코드 조회 (IP 주소)
          await for (final IPAddressResourceRecord ip in client.lookup<IPAddressResourceRecord>(
            ResourceRecordQuery.addressIPv4(srv.target),
          )) {
            debugPrint('🌐 IP 주소: ${ip.address.address}');
            
            final device = DiscoveredDevice(
              name: ptr.domainName,
              ip: ip.address.address,
              port: srv.port,
              type: 'mDNS',
              attributes: {'priority': srv.priority.toString(), 'weight': srv.weight.toString()},
              discoveredAt: DateTime.now(),
            );
            
            _addDevice(device);
          }
        }
      }
      
      client.stop();
    } catch (e) {
      debugPrint('Multicast DNS 검색 실패: $e');
    }
  }
  
  /// TCP 포트 스캔을 통한 디바이스 검색
  Future<void> scanNetwork({int startPort = 8080, int endPort = 8090}) async {
    if (kIsWeb) {
      debugPrint('⚠️ 네트워크 스캔은 웹에서 지원되지 않습니다');
      return;
    }
    
    if (_localIP == null) {
      debugPrint('로컬 IP를 찾을 수 없음');
      return;
    }
    
    // 네트워크 범위 계산 (예: 192.168.1.x)
    final parts = _localIP!.split('.');
    if (parts.length != 4) return;
    
    final subnet = '${parts[0]}.${parts[1]}.${parts[2]}';
    
    debugPrint('🔍 네트워크 스캔 시작: $subnet.1-254');
    
    for (int i = 1; i <= 254; i++) {
      final ip = '$subnet.$i';
      
      // 자기 자신은 제외
      if (ip == _localIP) continue;
      
      // 비동기로 각 IP 스캔
      _scanHost(ip, startPort, endPort);
    }
  }
  
  /// 특정 호스트의 포트 스캔
  Future<void> _scanHost(String host, int startPort, int endPort) async {
    for (int port = startPort; port <= endPort; port++) {
      try {
        final socket = await Socket.connect(
          host, 
          port,
          timeout: const Duration(milliseconds: 500),
        );
        
        debugPrint('✅ 열린 포트 발견: $host:$port');
        
        // 간단한 HTTP 요청으로 서비스 확인
        socket.write('GET /info HTTP/1.1\r\nHost: $host\r\n\r\n');
        
        // 응답 대기 (바이트 스트림으로 받아서 문자열로 변환)
        final bytes = <int>[];
        await socket.listen(
          bytes.addAll,
          onDone: () {},
          onError: (_) {},
        ).asFuture();
        
        final response = utf8.decode(bytes, allowMalformed: true);
        
        socket.destroy();
        
        final device = DiscoveredDevice(
          name: 'Device at $host',
          ip: host,
          port: port,
          type: 'TCP Scan',
          attributes: {'response': response.substring(0, response.length.clamp(0, 100))},
          discoveredAt: DateTime.now(),
        );
        
        _addDevice(device);
        
      } catch (e) {
        // 연결 실패는 무시 (포트가 닫혀있음)
      }
    }
  }
  
  /// UDP 메시지 파싱 및 디바이스 추가
  void _parseAndAddDevice(String message, String ip, String type) {
    try {
      // 메시지 포맷: "BIFAI_DEVICE|IP|OS|이름"
      if (message.startsWith('BIFAI_DEVICE')) {
        final parts = message.split('|');
        if (parts.length >= 4) {
          final device = DiscoveredDevice(
            name: parts[3],
            ip: ip,
            port: 0,
            type: type,
            attributes: {
              'os': parts[2],
              'reportedIP': parts[1],
            },
            discoveredAt: DateTime.now(),
          );
          
          _addDevice(device);
        }
      } else {
        // 다른 형식의 메시지 처리
        final device = DiscoveredDevice(
          name: 'Unknown Device',
          ip: ip,
          port: 0,
          type: type,
          attributes: {'message': message},
          discoveredAt: DateTime.now(),
        );
        
        _addDevice(device);
      }
    } catch (e) {
      debugPrint('메시지 파싱 실패: $e');
    }
  }
  
  /// 디바이스 추가
  void _addDevice(DiscoveredDevice device) {
    // 중복 체크
    final existingIndex = _discoveredDevices.indexWhere(
      (d) => d.ip == device.ip && d.name == device.name,
    );
    
    if (existingIndex >= 0) {
      // 기존 디바이스 업데이트
      _discoveredDevices[existingIndex] = device;
    } else {
      // 새 디바이스 추가
      _discoveredDevices.add(device);
    }
    
    // 스트림 업데이트
    _deviceStreamController.add(_discoveredDevices);
  }
  
  /// 디바이스 제거
  void _removeDevice(String name) {
    _discoveredDevices.removeWhere((d) => d.name == name);
    _deviceStreamController.add(_discoveredDevices);
  }
  
  /// 모든 디스커버리 중지
  void stopAll() {
    _broadcastTimer?.cancel();
    _bonsoirDiscovery?.stop();
    _udpSocket?.close();
    _discoveredDevices.clear();
    _deviceStreamController.add(_discoveredDevices);
  }
  
  /// 리소스 정리
  void dispose() {
    stopAll();
    _deviceStreamController.close();
  }
  
  /// 웹용 시뮬레이션 디바이스 추가
  void _addSimulatedDevices() {
    Timer(const Duration(seconds: 1), () {
      _addDevice(DiscoveredDevice(
        name: 'BIF-AI 스마트워치',
        ip: '192.168.1.100',
        port: 8080,
        type: 'UDP',
        attributes: {'os': 'WearOS', 'battery': '85%'},
        discoveredAt: DateTime.now(),
      ));
    });
    
    Timer(const Duration(seconds: 2), () {
      _addDevice(DiscoveredDevice(
        name: '보호자 폰 (김보호)',
        ip: '192.168.1.101',
        port: 8888,
        type: 'UDP',
        attributes: {'os': 'Android', 'version': '13'},
        discoveredAt: DateTime.now(),
      ));
    });
    
    Timer(const Duration(seconds: 3), () {
      _addDevice(DiscoveredDevice(
        name: 'BIF-AI 허브',
        ip: '192.168.1.200',
        port: 80,
        type: 'mDNS',
        attributes: {'service': 'HTTP', 'status': 'online'},
        discoveredAt: DateTime.now(),
      ));
    });
  }
  
  /// 현재 네트워크 정보 가져오기
  Future<Map<String, String?>> getNetworkInfo() async {
    return {
      'localIP': _localIP,
      'wifiName': _wifiName,
      'wifiBSSID': await _networkInfo.getWifiBSSID(),
      'wifiIPv6': await _networkInfo.getWifiIPv6(),
      'wifiGateway': await _networkInfo.getWifiGatewayIP(),
      'wifiBroadcast': await _networkInfo.getWifiBroadcast(),
      'wifiSubmask': await _networkInfo.getWifiSubmask(),
    };
  }
}

/// 발견된 디바이스 모델
class DiscoveredDevice {
  final String name;
  final String ip;
  final int port;
  final String type;
  final Map<String, String> attributes;
  final DateTime discoveredAt;
  
  DiscoveredDevice({
    required this.name,
    required this.ip,
    required this.port,
    required this.type,
    required this.attributes,
    required this.discoveredAt,
  });
  
  @override
  String toString() {
    return 'Device: $name ($ip:$port) via $type';
  }
}