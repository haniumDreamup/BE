// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'cached_medication.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class CachedMedicationAdapter extends TypeAdapter<CachedMedication> {
  @override
  final int typeId = 0;

  @override
  CachedMedication read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return CachedMedication(
      id: fields[0] as int,
      name: fields[1] as String,
      simpleDescription: fields[2] as String,
      time: fields[3] as String,
      taken: fields[4] as bool,
      dosage: fields[5] as String,
      color: fields[6] as String,
      icon: fields[7] as String,
      image: fields[8] as String?,
      important: fields[9] as bool,
      takenAt: fields[10] as DateTime?,
      note: fields[11] as String?,
      lastSynced: fields[12] as DateTime,
      pendingSync: fields[13] as bool,
    );
  }

  @override
  void write(BinaryWriter writer, CachedMedication obj) {
    writer
      ..writeByte(14)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.name)
      ..writeByte(2)
      ..write(obj.simpleDescription)
      ..writeByte(3)
      ..write(obj.time)
      ..writeByte(4)
      ..write(obj.taken)
      ..writeByte(5)
      ..write(obj.dosage)
      ..writeByte(6)
      ..write(obj.color)
      ..writeByte(7)
      ..write(obj.icon)
      ..writeByte(8)
      ..write(obj.image)
      ..writeByte(9)
      ..write(obj.important)
      ..writeByte(10)
      ..write(obj.takenAt)
      ..writeByte(11)
      ..write(obj.note)
      ..writeByte(12)
      ..write(obj.lastSynced)
      ..writeByte(13)
      ..write(obj.pendingSync);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is CachedMedicationAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
