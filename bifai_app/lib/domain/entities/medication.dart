import 'package:equatable/equatable.dart';

class Medication extends Equatable {
  final int id;
  final String name;
  final String simpleDescription;
  final String time;
  final bool taken;
  final String dosage;
  final String color;
  final String icon;
  final String? image;
  final bool important;
  final DateTime? takenAt;
  final String? note;
  
  const Medication({
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
  });
  
  @override
  List<Object?> get props => [
    id,
    name,
    simpleDescription,
    time,
    taken,
    dosage,
    color,
    icon,
    image,
    important,
    takenAt,
    note,
  ];
  
  Medication copyWith({
    int? id,
    String? name,
    String? simpleDescription,
    String? time,
    bool? taken,
    String? dosage,
    String? color,
    String? icon,
    String? image,
    bool? important,
    DateTime? takenAt,
    String? note,
  }) {
    return Medication(
      id: id ?? this.id,
      name: name ?? this.name,
      simpleDescription: simpleDescription ?? this.simpleDescription,
      time: time ?? this.time,
      taken: taken ?? this.taken,
      dosage: dosage ?? this.dosage,
      color: color ?? this.color,
      icon: icon ?? this.icon,
      image: image ?? this.image,
      important: important ?? this.important,
      takenAt: takenAt ?? this.takenAt,
      note: note ?? this.note,
    );
  }
}