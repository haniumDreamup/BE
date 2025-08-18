import 'package:bifai_app/domain/entities/user.dart';

class UserModel extends User {
  const UserModel({
    required super.id,
    required super.name,
    super.email,
    super.profileImage,
    required super.cognitiveLevel,
    super.birthDate,
    super.phone,
  });
  
  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'],
      name: json['name'],
      email: json['email'],
      profileImage: json['profileImage'],
      cognitiveLevel: json['cognitiveLevel'] ?? 'MODERATE',
      birthDate: json['birthDate'] != null 
          ? DateTime.parse(json['birthDate']) 
          : null,
      phone: json['phone'],
    );
  }
  
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'email': email,
      'profileImage': profileImage,
      'cognitiveLevel': cognitiveLevel,
      'birthDate': birthDate?.toIso8601String(),
      'phone': phone,
    };
  }
  
  User toEntity() {
    return User(
      id: id,
      name: name,
      email: email,
      profileImage: profileImage,
      cognitiveLevel: cognitiveLevel,
      birthDate: birthDate,
      phone: phone,
    );
  }
}