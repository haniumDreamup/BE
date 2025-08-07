package com.bifai.reminder.bifai_backend.exception;

/**
 * 디바이스 연결 예외
 * BIF 사용자의 웨어러블 기기나 스마트폰 연결 문제
 */
public class DeviceConnectionException extends BifException {
    
    private final Long deviceId;
    private final String deviceName;
    private final ConnectionProblem problem;
    
    public enum ConnectionProblem {
        BLUETOOTH_OFF("블루투스가 꺼짐"),
        WIFI_DISCONNECTED("와이파이 연결 끊김"),
        LOW_BATTERY("배터리 부족"),
        DEVICE_NOT_FOUND("기기를 찾을 수 없음"),
        PAIRING_FAILED("연결 실패"),
        SIGNAL_WEAK("신호가 약함");
        
        private final String description;
        
        ConnectionProblem(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public DeviceConnectionException(Long deviceId, String deviceName, ConnectionProblem problem) {
        super(
            String.format("Device connection problem: %s - %s", deviceName, problem.getDescription()),
            getConnectionMessage(problem, deviceName),
            getConnectionAction(problem)
        );
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.problem = problem;
    }
    
    private static String getConnectionMessage(ConnectionProblem problem, String deviceName) {
        switch (problem) {
            case BLUETOOTH_OFF:
                return deviceName + "의 블루투스가 꺼져있어요.";
            case LOW_BATTERY:
                return deviceName + "의 배터리가 부족해요.";
            case DEVICE_NOT_FOUND:
                return deviceName + "을(를) 찾을 수 없어요.";
            default:
                return deviceName + " 연결에 문제가 있어요.";
        }
    }
    
    private static String getConnectionAction(ConnectionProblem problem) {
        switch (problem) {
            case BLUETOOTH_OFF:
                return "설정에서 블루투스를 켜주세요.";
            case LOW_BATTERY:
                return "충전기에 연결해 주세요.";
            case WIFI_DISCONNECTED:
                return "와이파이를 다시 연결해 주세요.";
            default:
                return "기기를 다시 시작해 보세요.";
        }
    }
    
    public Long getDeviceId() {
        return deviceId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public ConnectionProblem getProblem() {
        return problem;
    }
}