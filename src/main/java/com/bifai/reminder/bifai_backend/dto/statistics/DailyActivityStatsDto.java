package com.bifai.reminder.bifai_backend.dto.statistics;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 일일 활동 통계 DTO
 */
@Builder
public class DailyActivityStatsDto {
  public final Long userId;
  public final LocalDate date;
  public final int totalLocations;
  public final double totalDistanceKm;
  public final int movementCount;
  public final LocalDateTime firstActivity;
  public final LocalDateTime lastActivity;
  public final List<HourlyActivity> hourlyBreakdown;
  public final List<LocationActivity> topLocations;

  public DailyActivityStatsDto(Long userId, LocalDate date, int totalLocations, double totalDistanceKm,
                              int movementCount, LocalDateTime firstActivity, LocalDateTime lastActivity,
                              List<HourlyActivity> hourlyBreakdown, List<LocationActivity> topLocations) {
    this.userId = userId;
    this.date = date;
    this.totalLocations = totalLocations;
    this.totalDistanceKm = totalDistanceKm;
    this.movementCount = movementCount;
    this.firstActivity = firstActivity;
    this.lastActivity = lastActivity;
    this.hourlyBreakdown = hourlyBreakdown;
    this.topLocations = topLocations;
  }

  @Builder
  public static class HourlyActivity {
    public final int hour;
    public final int locationCount;
    public final double distanceKm;
    public final int movements;

    public HourlyActivity(int hour, int locationCount, double distanceKm, int movements) {
      this.hour = hour;
      this.locationCount = locationCount;
      this.distanceKm = distanceKm;
      this.movements = movements;
    }
  }

  @Builder
  public static class LocationActivity {
    public final double latitude;
    public final double longitude;
    public final String address;
    public final int visitCount;
    public final long totalTimeMinutes;

    public LocationActivity(double latitude, double longitude, String address, int visitCount, long totalTimeMinutes) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.address = address;
      this.visitCount = visitCount;
      this.totalTimeMinutes = totalTimeMinutes;
    }
  }
}