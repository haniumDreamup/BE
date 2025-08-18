import 'package:flutter/material.dart';
import '../../../core/config/theme_config.dart';
import '../../../core/services/offline_service.dart';

class NetworkStatusBanner extends StatefulWidget {
  final Widget child;
  
  const NetworkStatusBanner({
    super.key,
    required this.child,
  });

  @override
  State<NetworkStatusBanner> createState() => _NetworkStatusBannerState();
}

class _NetworkStatusBannerState extends State<NetworkStatusBanner> 
    with SingleTickerProviderStateMixin {
  final OfflineService _offlineService = OfflineService();
  late AnimationController _animationController;
  late Animation<double> _animation;
  bool _isOnline = true;
  bool _showBanner = false;
  
  @override
  void initState() {
    super.initState();
    
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    
    _animation = CurvedAnimation(
      parent: _animationController,
      curve: Curves.easeInOut,
    );
    
    // Get initial status
    _isOnline = _offlineService.isOnline;
    
    // Listen to connection changes
    _offlineService.connectionStatus.listen((isOnline) {
      if (_isOnline != isOnline) {
        setState(() {
          _isOnline = isOnline;
          _showBanner = true;
        });
        
        if (isOnline) {
          // Show "back online" message briefly
          _animationController.forward();
          Future.delayed(const Duration(seconds: 3), () {
            if (mounted) {
              _animationController.reverse().then((_) {
                if (mounted) {
                  setState(() {
                    _showBanner = false;
                  });
                }
              });
            }
          });
        } else {
          // Show offline banner
          _animationController.forward();
        }
      }
    });
  }
  
  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }
  
  @override
  Widget build(BuildContext context) {
    return Directionality(
      textDirection: TextDirection.ltr,
      child: Stack(
        children: [
          widget.child,
          if (_showBanner)
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: SafeArea(
              child: SizeTransition(
                sizeFactor: _animation,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 12,
                  ),
                  color: _isOnline 
                      ? ThemeConfig.successColor 
                      : ThemeConfig.warningColor,
                  child: Row(
                    children: [
                      Icon(
                        _isOnline 
                            ? Icons.wifi 
                            : Icons.wifi_off,
                        color: Colors.white,
                        size: 20,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          _isOnline 
                              ? '인터넷에 연결되었어요' 
                              : '인터넷 연결이 끊어졌어요',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 14,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                      if (!_isOnline)
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 2,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.2),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Text(
                            '오프라인 모드',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 12,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}