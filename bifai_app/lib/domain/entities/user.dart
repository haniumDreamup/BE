import 'package:equatable/equatable.dart';

class User extends Equatable {
  final int id;
  final String name;
  final String? email;
  final String? profileImage;
  final String cognitiveLevel;
  final DateTime? birthDate;
  final String? phone;
  
  const User({
    required this.id,
    required this.name,
    this.email,
    this.profileImage,
    required this.cognitiveLevel,
    this.birthDate,
    this.phone,
  });
  
  @override
  List<Object?> get props => [
    id,
    name,
    email,
    profileImage,
    cognitiveLevel,
    birthDate,
    phone,
  ];
  
  User copyWith({
    int? id,
    String? name,
    String? email,
    String? profileImage,
    String? cognitiveLevel,
    DateTime? birthDate,
    String? phone,
  }) {
    return User(
      id: id ?? this.id,
      name: name ?? this.name,
      email: email ?? this.email,
      profileImage: profileImage ?? this.profileImage,
      cognitiveLevel: cognitiveLevel ?? this.cognitiveLevel,
      birthDate: birthDate ?? this.birthDate,
      phone: phone ?? this.phone,
    );
  }
}