package com.bifai.reminder.bifai_backend.exception;

/**
 * 인지 과부하 예외
 * BIF 사용자에게 너무 복잡하거나 많은 정보가 제공되었을 때
 */
public class CognitiveOverloadException extends BifException {
    
    private final int informationCount;
    private final String context;
    
    public CognitiveOverloadException(int informationCount, String context) {
        super(
            String.format("Cognitive overload detected: %d items in %s", informationCount, context),
            "정보가 너무 많아요. 하나씩 천천히 볼게요.",
            "잠시 쉬었다가 다시 해보세요."
        );
        this.informationCount = informationCount;
        this.context = context;
    }
    
    public CognitiveOverloadException(String simpleMessage) {
        super(
            "Cognitive overload: " + simpleMessage,
            "이해하기 어려운 내용이에요.",
            "보호자에게 도움을 요청해 보세요."
        );
        this.informationCount = 0;
        this.context = simpleMessage;
    }
    
    public int getInformationCount() {
        return informationCount;
    }
    
    public String getContext() {
        return context;
    }
}