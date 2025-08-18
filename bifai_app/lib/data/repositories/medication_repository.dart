import 'package:dio/dio.dart';
import '../../core/constants/api_endpoints.dart';
import '../../core/network/api_client.dart';
import '../../core/services/offline_service.dart';
import '../../domain/entities/medication.dart';
import '../models/cached_medication.dart';

class MedicationRepository {
  final ApiClient _apiClient;
  final OfflineService _offlineService;
  
  MedicationRepository({
    ApiClient? apiClient,
    OfflineService? offlineService,
  }) : _apiClient = apiClient ?? ApiClient(),
       _offlineService = offlineService ?? OfflineService();
  
  Future<List<Medication>> getTodayMedications({bool forceRefresh = false}) async {
    // Check if we're online and should fetch from server
    if (_offlineService.isOnline && 
        (forceRefresh || _offlineService.needsSync('medications'))) {
      try {
        final response = await _apiClient.get(ApiEndpoints.medicationsToday);
        
        if (response.data['success'] == true) {
          final medicationsData = response.data['data']['medications'] as List;
          
          // Convert to cached models and save
          final cachedMedications = medicationsData.map((json) => 
            CachedMedication.fromJson(json)
          ).toList();
          
          await _offlineService.saveMedications(cachedMedications);
          
          // Return as domain entities
          return cachedMedications.map(_toDomainEntity).toList();
        }
      } on DioException catch (e) {
        print('Failed to fetch medications from server: $e');
        // Fall through to offline data
      }
    }
    
    // Return offline data
    final cachedMedications = _offlineService.getMedications();
    return cachedMedications.map(_toDomainEntity).toList();
  }
  
  Future<bool> takeMedication(int id, {String? note}) async {
    // Update locally first (optimistic update)
    await _offlineService.updateMedicationStatus(id, true, note: note);
    
    // If online, sync immediately
    if (_offlineService.isOnline) {
      try {
        final response = await _apiClient.post(
          ApiEndpoints.takeMedication.replaceAll('{id}', id.toString()),
          data: {
            'takenAt': DateTime.now().toIso8601String(),
            'note': note,
          },
        );
        
        if (response.data['success'] == true) {
          // Mark as synced
          final medication = _offlineService.getMedications()
              .firstWhere((m) => m.id == id);
          medication.pendingSync = false;
          await medication.save();
          return true;
        }
      } catch (e) {
        print('Failed to sync medication status: $e');
        // Keep local update with pendingSync = true
      }
    }
    
    return true; // Return true for optimistic update
  }
  
  Future<bool> untakeMedication(int id) async {
    // Update locally first
    await _offlineService.updateMedicationStatus(id, false);
    
    // If online, sync immediately
    if (_offlineService.isOnline) {
      try {
        final response = await _apiClient.put(
          '${ApiEndpoints.medicationDetail}/$id/untake',
        );
        
        if (response.data['success'] == true) {
          // Mark as synced
          final medication = _offlineService.getMedications()
              .firstWhere((m) => m.id == id);
          medication.pendingSync = false;
          await medication.save();
          return true;
        }
      } catch (e) {
        print('Failed to sync medication status: $e');
      }
    }
    
    return true;
  }
  
  Future<void> syncPendingMedications() async {
    if (!_offlineService.isOnline) return;
    
    final pendingMedications = _offlineService.getPendingSyncMedications();
    
    for (var medication in pendingMedications) {
      try {
        if (medication.taken) {
          await _apiClient.post(
            ApiEndpoints.takeMedication.replaceAll('{id}', medication.id.toString()),
            data: {
              'takenAt': medication.takenAt?.toIso8601String(),
              'note': medication.note,
            },
          );
        } else {
          await _apiClient.put(
            '${ApiEndpoints.medicationDetail}/${medication.id}/untake',
          );
        }
        
        // Mark as synced
        medication.pendingSync = false;
        await medication.save();
      } catch (e) {
        print('Failed to sync medication ${medication.id}: $e');
      }
    }
  }
  
  Medication _toDomainEntity(CachedMedication cached) {
    return Medication(
      id: cached.id,
      name: cached.name,
      simpleDescription: cached.simpleDescription,
      time: cached.time,
      taken: cached.taken,
      dosage: cached.dosage,
      color: cached.color,
      icon: cached.icon,
      image: cached.image,
      important: cached.important,
      takenAt: cached.takenAt,
      note: cached.note,
    );
  }
}