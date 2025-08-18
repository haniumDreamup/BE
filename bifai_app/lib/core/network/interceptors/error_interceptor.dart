import 'package:dio/dio.dart';
import 'package:logger/logger.dart';

class ErrorInterceptor extends Interceptor {
  final Logger _logger = Logger();
  
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    String errorMessage = '문제가 발생했어요';
    String userAction = '다시 시도해주세요';
    
    switch (err.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        errorMessage = '연결 시간이 초과되었어요';
        userAction = '인터넷 연결을 확인해주세요';
        break;
        
      case DioExceptionType.connectionError:
        errorMessage = '인터넷에 연결할 수 없어요';
        userAction = '인터넷 연결을 확인해주세요';
        break;
        
      case DioExceptionType.badResponse:
        final statusCode = err.response?.statusCode;
        final responseData = err.response?.data;
        
        if (responseData != null && responseData['error'] != null) {
          errorMessage = responseData['error']['message'] ?? errorMessage;
          userAction = responseData['error']['userAction'] ?? userAction;
        } else {
          switch (statusCode) {
            case 400:
              errorMessage = '입력한 정보를 확인해주세요';
              break;
            case 401:
              errorMessage = '다시 로그인해주세요';
              userAction = '앱을 다시 열어주세요';
              break;
            case 403:
              errorMessage = '권한이 없어요';
              break;
            case 404:
              errorMessage = '찾을 수 없어요';
              break;
            case 500:
            case 502:
            case 503:
              errorMessage = '서버에 문제가 있어요';
              userAction = '잠시 후 다시 시도해주세요';
              break;
            default:
              errorMessage = '오류가 발생했어요';
          }
        }
        break;
        
      case DioExceptionType.cancel:
        errorMessage = '요청이 취소되었어요';
        break;
        
      default:
        errorMessage = '알 수 없는 오류가 발생했어요';
    }
    
    // Log error
    _logger.e('API Error', error: err, stackTrace: err.stackTrace);
    
    // Create custom error response
    final customError = DioException(
      requestOptions: err.requestOptions,
      response: err.response,
      type: err.type,
      error: {
        'message': errorMessage,
        'userAction': userAction,
        'originalError': err.message,
      },
    );
    
    return handler.next(customError);
  }
}