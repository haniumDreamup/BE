import 'package:flutter/material.dart';
import '../../data/repositories/auth_repository.dart';
import '../../domain/entities/user.dart';

class AuthProvider extends ChangeNotifier {
  final AuthRepository _authRepository;
  
  User? _currentUser;
  bool _isAuthenticated = false;
  bool _isLoading = false;
  String? _errorMessage;
  
  AuthProvider({AuthRepository? authRepository})
      : _authRepository = authRepository ?? AuthRepository();
  
  User? get currentUser => _currentUser;
  bool get isAuthenticated => _isAuthenticated;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  
  Future<bool> login(String email, String password) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();
    
    try {
      _currentUser = await _authRepository.login(email, password);
      _isAuthenticated = true;
      _isLoading = false;
      notifyListeners();
      
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      _isLoading = false;
      _isAuthenticated = false;
      notifyListeners();
      return false;
    }
  }
  
  Future<void> logout() async {
    _isLoading = true;
    notifyListeners();
    
    try {
      await _authRepository.logout();
      
      _currentUser = null;
      _isAuthenticated = false;
      _errorMessage = null;
    } catch (e) {
      // Handle error silently
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
  
  Future<bool> checkAuthStatus() async {
    _isLoading = true;
    notifyListeners();
    
    try {
      _currentUser = await _authRepository.checkAuthStatus();
      _isAuthenticated = _currentUser != null;
    } catch (e) {
      _isAuthenticated = false;
      _currentUser = null;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
    
    return _isAuthenticated;
  }
}