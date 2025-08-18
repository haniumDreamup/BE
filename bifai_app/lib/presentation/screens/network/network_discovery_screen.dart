import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/config/toss_theme.dart';
import '../../../core/services/network_discovery_service.dart';
import 'package:provider/provider.dart';
import '../../providers/accessibility_provider.dart';

class NetworkDiscoveryScreen extends StatefulWidget {
  const NetworkDiscoveryScreen({Key? key}) : super(key: key);

  @override
  State<NetworkDiscoveryScreen> createState() => _NetworkDiscoveryScreenState();
}

class _NetworkDiscoveryScreenState extends State<NetworkDiscoveryScreen> 
    with SingleTickerProviderStateMixin {
  final NetworkDiscoveryService _discoveryService = NetworkDiscoveryService();
  late AnimationController _animationController;
  bool _isScanning = false;
  Map<String, String?> _networkInfo = {};
  
  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat();
    
    _initializeService();
  }
  
  Future<void> _initializeService() async {
    await _discoveryService.initialize();
    await _loadNetworkInfo();
  }
  
  Future<void> _loadNetworkInfo() async {
    final info = await _discoveryService.getNetworkInfo();
    setState(() {
      _networkInfo = info;
    });
  }
  
  void _startDiscovery() {
    setState(() {
      _isScanning = true;
    });
    
    final accessibility = context.read<AccessibilityProvider>();
    accessibility.speak('네트워크 디바이스 검색을 시작합니다');
    
    // 여러 방법으로 디바이스 검색
    _discoveryService.startUdpBroadcastListener(port: 8888);
    _discoveryService.startMdnsDiscovery();
    _discoveryService.startMulticastDnsDiscovery();
    _discoveryService.scanNetwork();
  }
  
  void _stopDiscovery() {
    setState(() {
      _isScanning = false;
    });
    
    _discoveryService.stopAll();
    
    final accessibility = context.read<AccessibilityProvider>();
    accessibility.speak('검색을 중지했습니다');
  }
  
  @override
  void dispose() {
    _animationController.dispose();
    _discoveryService.dispose();
    super.dispose();
  }
  
  @override
  Widget build(BuildContext context) {
    final accessibility = context.watch<AccessibilityProvider>();
    
    return Scaffold(
      backgroundColor: TossTheme.tossGray50,
      appBar: AppBar(
        backgroundColor: TossTheme.tossWhite,
        title: const Text(
          '네트워크 디바이스 찾기',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: TossTheme.tossGray900,
          ),
        ),
        actions: [
          IconButton(
            icon: Icon(
              Icons.info_outline_rounded,
              color: TossTheme.tossGray700,
            ),
            onPressed: () {
              _showNetworkInfo(context);
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // 스캔 컨트롤
          Container(
            color: TossTheme.tossWhite,
            padding: const EdgeInsets.all(20),
            child: Column(
              children: [
                // 스캔 버튼
                SizedBox(
                  width: double.infinity,
                  height: 56,
                  child: ElevatedButton(
                    onPressed: _isScanning ? _stopDiscovery : _startDiscovery,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _isScanning ? TossTheme.tossRed : TossTheme.tossBlue,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        if (_isScanning) ...[
                          RotationTransition(
                            turns: _animationController,
                            child: const Icon(Icons.radar_rounded, size: 24),
                          ),
                          const SizedBox(width: 12),
                          const Text(
                            '검색 중지',
                            style: TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ] else ...[
                          const Icon(Icons.search_rounded, size: 24),
                          const SizedBox(width: 12),
                          const Text(
                            '디바이스 검색',
                            style: TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                // 네트워크 정보
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: TossTheme.tossGray50,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: TossTheme.tossBlue.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Icon(
                          Icons.wifi_rounded,
                          color: TossTheme.tossBlue,
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              _networkInfo['wifiName'] ?? '연결 안됨',
                              style: const TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.w600,
                                color: TossTheme.tossGray900,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              _networkInfo['localIP'] ?? 'IP 없음',
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w400,
                                color: TossTheme.tossGray500,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          
          const SizedBox(height: 8),
          
          // 발견된 디바이스 목록
          Expanded(
            child: StreamBuilder<List<DiscoveredDevice>>(
              stream: _discoveryService.devicesStream,
              builder: (context, snapshot) {
                final devices = snapshot.data ?? [];
                
                if (devices.isEmpty && !_isScanning) {
                  return Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.devices_other_rounded,
                          size: 64,
                          color: TossTheme.tossGray300,
                        ),
                        const SizedBox(height: 16),
                        const Text(
                          '디바이스를 검색해주세요',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                            color: TossTheme.tossGray500,
                          ),
                        ),
                      ],
                    ),
                  );
                }
                
                if (devices.isEmpty && _isScanning) {
                  return Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        RotationTransition(
                          turns: _animationController,
                          child: Icon(
                            Icons.radar_rounded,
                            size: 64,
                            color: TossTheme.tossBlue.withOpacity(0.5),
                          ),
                        ),
                        const SizedBox(height: 16),
                        const Text(
                          '디바이스를 찾는 중...',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                            color: TossTheme.tossGray500,
                          ),
                        ),
                        const SizedBox(height: 8),
                        const Text(
                          'UDP, mDNS, TCP 스캔 진행 중',
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w400,
                            color: TossTheme.tossGray400,
                          ),
                        ),
                      ],
                    ),
                  );
                }
                
                return ListView.separated(
                  padding: const EdgeInsets.all(20),
                  itemCount: devices.length,
                  separatorBuilder: (context, index) => const SizedBox(height: 12),
                  itemBuilder: (context, index) {
                    final device = devices[index];
                    return _buildDeviceCard(device, accessibility);
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
  
  Widget _buildDeviceCard(DiscoveredDevice device, AccessibilityProvider accessibility) {
    final iconData = _getDeviceIcon(device);
    final color = _getDeviceColor(device);
    
    return InkWell(
      onTap: () {
        HapticFeedback.lightImpact();
        accessibility.speak('${device.name} 디바이스 선택됨. IP 주소는 ${device.ip}입니다');
        _showDeviceDetails(device);
      },
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: TossTheme.tossWhite,
          borderRadius: BorderRadius.circular(16),
          boxShadow: TossTheme.tossShadowSmall,
        ),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(iconData, color: color, size: 24),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          device.name,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                            color: TossTheme.tossGray900,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: color.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          device.type,
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: color,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '${device.ip}${device.port > 0 ? ':${device.port}' : ''}',
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                      color: TossTheme.tossGray600,
                    ),
                  ),
                  if (device.attributes.isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text(
                      device.attributes.entries.map((e) => '${e.key}: ${e.value}').first,
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w400,
                        color: TossTheme.tossGray500,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
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
  
  IconData _getDeviceIcon(DiscoveredDevice device) {
    final os = device.attributes['os']?.toLowerCase() ?? '';
    if (os.contains('android')) return Icons.android_rounded;
    if (os.contains('ios')) return Icons.phone_iphone_rounded;
    if (os.contains('windows')) return Icons.desktop_windows_rounded;
    if (os.contains('mac')) return Icons.desktop_mac_rounded;
    if (os.contains('linux')) return Icons.computer_rounded;
    if (device.type == 'mDNS') return Icons.dns_rounded;
    if (device.type == 'UDP') return Icons.cell_tower_rounded;
    return Icons.devices_other_rounded;
  }
  
  Color _getDeviceColor(DiscoveredDevice device) {
    switch (device.type) {
      case 'UDP':
        return TossTheme.tossBlue;
      case 'mDNS':
        return TossTheme.tossPurple;
      case 'TCP Scan':
        return TossTheme.tossGreen;
      default:
        return TossTheme.tossGray600;
    }
  }
  
  void _showDeviceDetails(DiscoveredDevice device) {
    showModalBottomSheet(
      context: context,
      backgroundColor: TossTheme.tossWhite,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (context) {
        return Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 56,
                    height: 56,
                    decoration: BoxDecoration(
                      color: _getDeviceColor(device).withOpacity(0.1),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Icon(
                      _getDeviceIcon(device),
                      color: _getDeviceColor(device),
                      size: 28,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          device.name,
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                            color: TossTheme.tossGray900,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '발견 시간: ${_formatTime(device.discoveredAt)}',
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w400,
                            color: TossTheme.tossGray500,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              _buildDetailRow('IP 주소', device.ip),
              if (device.port > 0) _buildDetailRow('포트', device.port.toString()),
              _buildDetailRow('검색 방법', device.type),
              if (device.attributes.isNotEmpty) ...[
                const SizedBox(height: 16),
                const Text(
                  '추가 정보',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: TossTheme.tossGray900,
                  ),
                ),
                const SizedBox(height: 8),
                ...device.attributes.entries.map((e) => 
                  _buildDetailRow(e.key, e.value, small: true),
                ),
              ],
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: () {
                    Navigator.pop(context);
                    // TODO: 디바이스 연결 로직
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: TossTheme.tossBlue,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Text(
                    '이 디바이스와 연결',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
  
  Widget _buildDetailRow(String label, String value, {bool small = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: small ? 80 : 100,
            child: Text(
              label,
              style: TextStyle(
                fontSize: small ? 13 : 14,
                fontWeight: FontWeight.w500,
                color: TossTheme.tossGray600,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: TextStyle(
                fontSize: small ? 13 : 14,
                fontWeight: FontWeight.w600,
                color: TossTheme.tossGray900,
              ),
            ),
          ),
        ],
      ),
    );
  }
  
  void _showNetworkInfo(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: TossTheme.tossWhite,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          title: const Text(
            '네트워크 정보',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: TossTheme.tossGray900,
            ),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildInfoRow('Wi-Fi 이름', _networkInfo['wifiName']),
              _buildInfoRow('로컬 IP', _networkInfo['localIP']),
              _buildInfoRow('게이트웨이', _networkInfo['wifiGateway']),
              _buildInfoRow('서브넷 마스크', _networkInfo['wifiSubmask']),
              _buildInfoRow('브로드캐스트', _networkInfo['wifiBroadcast']),
              _buildInfoRow('BSSID', _networkInfo['wifiBSSID']),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('닫기'),
            ),
          ],
        );
      },
    );
  }
  
  Widget _buildInfoRow(String label, String? value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w500,
              color: TossTheme.tossGray600,
            ),
          ),
          Text(
            value ?? '-',
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: TossTheme.tossGray900,
            ),
          ),
        ],
      ),
    );
  }
  
  String _formatTime(DateTime time) {
    final now = DateTime.now();
    final difference = now.difference(time);
    
    if (difference.inSeconds < 60) {
      return '방금 전';
    } else if (difference.inMinutes < 60) {
      return '${difference.inMinutes}분 전';
    } else if (difference.inHours < 24) {
      return '${difference.inHours}시간 전';
    } else {
      return '${difference.inDays}일 전';
    }
  }
}