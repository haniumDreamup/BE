package com.bifai.reminder.bifai_backend.dto.mobile.home;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeDashboardResponse {
  private String greeting;
  private TodayInfo today;
  private DailySummary summary;
  private List<UrgentAlert> urgentAlerts;
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TodayInfo {
    private String date;
    private String weather;
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DailySummary {
    private int medicationsToTake;
    private int schedulesToday;
    private NextEvent nextEvent;
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NextEvent {
    private String title;
    private String time;
    private String icon;
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UrgentAlert {
    private String id;
    private String message;
    private String type;
  }
}