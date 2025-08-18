import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../../core/config/theme_config.dart';
import '../../providers/accessibility_provider.dart';

/// BIF 사용자를 위한 접근성이 강화된 버튼
/// - 큰 터치 영역 (최소 48dp)
/// - 음성 피드백
/// - 진동 피드백
/// - 명확한 시각적 피드백
class AccessibleButton extends StatefulWidget {
  final String label;
  final VoidCallback onPressed;
  final IconData? icon;
  final Color? backgroundColor;
  final Color? textColor;
  final double? width;
  final double? height;
  final bool isImportant;
  final bool showBorder;
  final String? helpText;
  
  const AccessibleButton({
    Key? key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.backgroundColor,
    this.textColor,
    this.width,
    this.height,
    this.isImportant = false,
    this.showBorder = false,
    this.helpText,
  }) : super(key: key);

  @override
  State<AccessibleButton> createState() => _AccessibleButtonState();
}

class _AccessibleButtonState extends State<AccessibleButton> 
    with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _scaleAnimation;
  bool _isPressed = false;
  
  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 150),
      vsync: this,
    );
    _scaleAnimation = Tween<double>(
      begin: 1.0,
      end: 0.95,
    ).animate(CurvedAnimation(
      parent: _animationController,
      curve: Curves.easeInOut,
    ));
  }
  
  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }
  
  void _handleTapDown(TapDownDetails details) {
    setState(() {
      _isPressed = true;
    });
    _animationController.forward();
  }
  
  void _handleTapUp(TapUpDetails details) {
    setState(() {
      _isPressed = false;
    });
    _animationController.reverse();
  }
  
  void _handleTapCancel() {
    setState(() {
      _isPressed = false;
    });
    _animationController.reverse();
  }
  
  void _handleTap() async {
    final accessibility = context.read<AccessibilityProvider>();
    
    // 음성 피드백
    await accessibility.speakButtonFeedback(widget.label);
    
    // 진동 피드백
    if (accessibility.vibrationEnabled) {
      HapticFeedback.mediumImpact();
    }
    
    // 버튼 액션 실행
    widget.onPressed();
  }
  
  void _handleLongPress() async {
    if (widget.helpText != null) {
      final accessibility = context.read<AccessibilityProvider>();
      await accessibility.speak(widget.helpText!);
      
      if (accessibility.vibrationEnabled) {
        HapticFeedback.heavyImpact();
      }
    }
  }
  
  @override
  Widget build(BuildContext context) {
    final accessibility = context.watch<AccessibilityProvider>();
    final theme = Theme.of(context);
    
    // 색상 결정
    Color bgColor = widget.backgroundColor ?? 
        (widget.isImportant ? ThemeConfig.primaryColor : ThemeConfig.secondaryColor);
    
    Color fgColor = widget.textColor ?? 
        (widget.isImportant ? Colors.white : ThemeConfig.textColor);
    
    // 고대비 모드
    if (accessibility.highContrastEnabled) {
      bgColor = widget.isImportant ? Colors.black : Colors.white;
      fgColor = widget.isImportant ? Colors.white : Colors.black;
    }
    
    // 버튼 크기 (최소 48dp)
    double buttonHeight = widget.height ?? 56.0;
    double buttonWidth = widget.width ?? double.infinity;
    
    // 텍스트 크기
    double fontSize = accessibility.largeTextEnabled ? 20.0 : 16.0;
    if (widget.isImportant) {
      fontSize += 2;
    }
    
    return Semantics(
      button: true,
      label: widget.label,
      hint: widget.helpText,
      child: GestureDetector(
        onTapDown: _handleTapDown,
        onTapUp: _handleTapUp,
        onTapCancel: _handleTapCancel,
        onTap: _handleTap,
        onLongPress: _handleLongPress,
        child: AnimatedBuilder(
          animation: _scaleAnimation,
          builder: (context, child) {
            return Transform.scale(
              scale: _scaleAnimation.value,
              child: Container(
                width: buttonWidth,
                height: buttonHeight,
                decoration: BoxDecoration(
                  color: _isPressed ? bgColor.withOpacity(0.8) : bgColor,
                  borderRadius: BorderRadius.circular(12),
                  border: widget.showBorder ? Border.all(
                    color: accessibility.highContrastEnabled ? 
                        Colors.black : ThemeConfig.borderColor,
                    width: 2,
                  ) : null,
                  boxShadow: [
                    if (!_isPressed && !accessibility.simplifiedUIEnabled)
                      BoxShadow(
                        color: Colors.black.withOpacity(0.1),
                        blurRadius: 8,
                        offset: const Offset(0, 4),
                      ),
                  ],
                ),
                child: Material(
                  color: Colors.transparent,
                  child: InkWell(
                    borderRadius: BorderRadius.circular(12),
                    splashColor: fgColor.withOpacity(0.2),
                    highlightColor: fgColor.withOpacity(0.1),
                    onTap: null, // GestureDetector에서 처리
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 12,
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          if (widget.icon != null) ...[
                            Icon(
                              widget.icon,
                              color: fgColor,
                              size: fontSize + 8,
                            ),
                            const SizedBox(width: 12),
                          ],
                          Flexible(
                            child: Text(
                              widget.label,
                              style: TextStyle(
                                color: fgColor,
                                fontSize: fontSize,
                                fontWeight: widget.isImportant ? 
                                    FontWeight.bold : FontWeight.w500,
                                letterSpacing: 0.5,
                              ),
                              textAlign: TextAlign.center,
                              overflow: TextOverflow.ellipsis,
                              maxLines: 2,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

/// 접근성 플로팅 액션 버튼
class AccessibleFloatingButton extends StatelessWidget {
  final String label;
  final VoidCallback onPressed;
  final IconData icon;
  final bool isExtended;
  final String? helpText;
  
  const AccessibleFloatingButton({
    Key? key,
    required this.label,
    required this.onPressed,
    required this.icon,
    this.isExtended = false,
    this.helpText,
  }) : super(key: key);
  
  @override
  Widget build(BuildContext context) {
    final accessibility = context.watch<AccessibilityProvider>();
    
    void handleTap() async {
      await accessibility.speakButtonFeedback(label);
      
      if (accessibility.vibrationEnabled) {
        HapticFeedback.mediumImpact();
      }
      
      onPressed();
    }
    
    if (isExtended || accessibility.largeTextEnabled) {
      return FloatingActionButton.extended(
        onPressed: handleTap,
        icon: Icon(icon, size: 28),
        label: Text(
          label,
          style: TextStyle(
            fontSize: accessibility.largeTextEnabled ? 18 : 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        backgroundColor: ThemeConfig.primaryColor,
        tooltip: helpText ?? label,
      );
    }
    
    return FloatingActionButton(
      onPressed: handleTap,
      child: Icon(icon, size: 32),
      backgroundColor: ThemeConfig.primaryColor,
      tooltip: helpText ?? label,
    );
  }
}