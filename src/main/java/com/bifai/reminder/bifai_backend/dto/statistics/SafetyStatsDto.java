package com.bifai.reminder.bifai_backend.dto.statistics;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 안전 통계 DTO
 */
@Builder
public class SafetyStatsDto {
  public final Long userId;
  public final LocalDate startDate;
  public final LocalDate endDate;
  public final int totalSosAlerts;
  public final int resolvedSosAlerts;
  public final int pendingSosAlerts;
  public final int geofenceViolations;
  public final int safetyScore;
  public final List<SafetyIncident> recentIncidents;
  public final List<DailySafetyStats> dailyStats;

  public SafetyStatsDto(Long userId, LocalDate startDate, LocalDate endDate, int totalSosAlerts,
                       int resolvedSosAlerts, int pendingSosAlerts, int geofenceViolations,
                       int safetyScore, List<SafetyIncident> recentIncidents, List<DailySafetyStats> dailyStats) {
    this.userId = userId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.totalSosAlerts = totalSosAlerts;
    this.resolvedSosAlerts = resolvedSosAlerts;
    this.pendingSosAlerts = pendingSosAlerts;
    this.geofenceViolations = geofenceViolations;
    this.safetyScore = safetyScore;
    this.recentIncidents = recentIncidents;
    this.dailyStats = dailyStats;
  }

  @Builder
  public static class SafetyIncident {
    public final Long incidentId;
    public final String incidentType;
    public final String description;
    public final LocalDateTime occurredAt;
    public final String status;
    public final String severity;
    public final double latitude;
    public final double longitude;
    public final String address;

    public SafetyIncident(Long incidentId, String incidentType, String description, LocalDateTime occurredAt,
                         String status, String severity, double latitude, double longitude, String address) {
      this.incidentId = incidentId;
      this.incidentType = incidentType;
      this.description = description;
      this.occurredAt = occurredAt;
      this.status = status;
      this.severity = severity;
      this.latitude = latitude;
      this.longitude = longitude;
      this.address = address;
    }
  }

  @Builder
  public static class DailySafetyStats {
    public final LocalDate date;
    public final int sosAlerts;
    public final int geofenceViolations;
    public final int safetyScore;
    public final boolean hasIncidents;

    public DailySafetyStats(LocalDate date, int sosAlerts, int geofenceViolations, int safetyScore, boolean hasIncidents) {
      this.date = date;
      this.sosAlerts = sosAlerts;
      this.geofenceViolations = geofenceViolations;
      this.safetyScore = safetyScore;
      this.hasIncidents = hasIncidents;
    }
  }
}