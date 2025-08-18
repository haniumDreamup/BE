import 'package:hive/hive.dart';

part 'cached_medication.g.dart';

@HiveType(typeId: 0)
class CachedMedication extends HiveObject {
  @HiveField(0)
  final int id;
  
  @HiveField(1)
  final String name;
  
  @HiveField(2)
  final String simpleDescription;
  
  @HiveField(3)
  final String time;
  
  @HiveField(4)
  bool taken;
  
  @HiveField(5)
  final String dosage;
  
  @HiveField(6)
  final String color;
  
  @HiveField(7)
  final String icon;
  
  @HiveField(8)
  final String? image;
  
  @HiveField(9)
  final bool important;
  
  @HiveField(10)
  DateTime? takenAt;
  
  @HiveField(11)
  String? note;
  
  @HiveField(12)
  DateTime lastSynced;
  
  @HiveField(13)
  bool pendingSync;
  
  CachedMedication({
    required this.id,
    required this.name,
    required this.simpleDescription,
    required this.time,
    required this.taken,
    required this.dosage,
    required this.color,
    required this.icon,
    this.image,
    required this.important,
    this.takenAt,
    this.note,
    required this.lastSynced,
    this.pendingSync = false,
  });
  
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'simpleDescription': simpleDescription,
      'time': time,
      'taken': taken,
      'dosage': dosage,
      'color': color,
      'icon': icon,
      'image': image,
      'important': important,
      'takenAt': takenAt?.toIso8601String(),
      'note': note,
    };
  }
  
  factory CachedMedication.fromJson(Map<String, dynamic> json) {
    return CachedMedication(
      id: json['id'],
      name: json['name'],
      simpleDescription: json['simpleDescription'],
      time: json['time'],
      taken: json['taken'],
      dosage: json['dosage'],
      color: json['color'],
      icon: json['icon'],
      image: json['image'],
      important: json['important'],
      takenAt: json['takenAt'] != null ? DateTime.parse(json['takenAt']) : null,
      note: json['note'],
      lastSynced: DateTime.now(),
      pendingSync: false,
    );
  }
}