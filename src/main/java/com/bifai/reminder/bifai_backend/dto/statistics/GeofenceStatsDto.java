package com.bifai.reminder.bifai_backend.dto.statistics;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * 지오펜스 통계 DTO
 */
@Builder
public class GeofenceStatsDto {
  public final Long userId;
  public final LocalDate startDate;
  public final LocalDate endDate;
  public final int totalGeofences;
  public final int totalEntries;
  public final int totalExits;
  public final int totalViolations;
  public final double avgDailyEntries;
  public final List<GeofenceEntry> topGeofences;
  public final List<DailyGeofenceActivity> dailyActivity;

  public GeofenceStatsDto(Long userId, LocalDate startDate, LocalDate endDate, int totalGeofences,
                         int totalEntries, int totalExits, int totalViolations, double avgDailyEntries,
                         List<GeofenceEntry> topGeofences, List<DailyGeofenceActivity> dailyActivity) {
    this.userId = userId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.totalGeofences = totalGeofences;
    this.totalEntries = totalEntries;
    this.totalExits = totalExits;
    this.totalViolations = totalViolations;
    this.avgDailyEntries = avgDailyEntries;
    this.topGeofences = topGeofences;
    this.dailyActivity = dailyActivity;
  }

  @Builder
  public static class GeofenceEntry {
    public final Long geofenceId;
    public final String geofenceName;
    public final int entryCount;
    public final int exitCount;
    public final int violations;

    public GeofenceEntry(Long geofenceId, String geofenceName, int entryCount, int exitCount, int violations) {
      this.geofenceId = geofenceId;
      this.geofenceName = geofenceName;
      this.entryCount = entryCount;
      this.exitCount = exitCount;
      this.violations = violations;
    }
  }

  @Builder
  public static class DailyGeofenceActivity {
    public final LocalDate date;
    public final int entries;
    public final int exits;
    public final int violations;

    public DailyGeofenceActivity(LocalDate date, int entries, int exits, int violations) {
      this.date = date;
      this.entries = entries;
      this.exits = exits;
      this.violations = violations;
    }
  }
}