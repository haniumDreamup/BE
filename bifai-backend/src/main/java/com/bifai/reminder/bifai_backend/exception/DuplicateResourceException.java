package com.bifai.reminder.bifai_backend.exception;

/**
 * 중복 리소스 예외
 * 이미 존재하는 리소스를 다시 생성하려 할 때 발생
 */
public class DuplicateResourceException extends RuntimeException {
    
    private final String resourceType;
    private final String fieldName;
    private final Object fieldValue;
    
    public DuplicateResourceException(String resourceType, String fieldName, Object fieldValue) {
        super(String.format("%s with %s '%s' already exists", resourceType, fieldName, fieldValue));
        this.resourceType = resourceType;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    public DuplicateResourceException(String message) {
        super(message);
        this.resourceType = null;
        this.fieldName = null;
        this.fieldValue = null;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getFieldValue() {
        return fieldValue;
    }
}