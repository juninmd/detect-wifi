class Device {
  final String id;
  final String macAddress;
  final String ipAddress;
  final String vendor;
  final bool isKnown;
  final String category;
  final DateTime lastSeen;

  Device({
    required this.id,
    required this.macAddress,
    required this.ipAddress,
    required this.vendor,
    required this.isKnown,
    required this.category,
    required this.lastSeen,
  });

  Device copyWith({
    String? id,
    String? macAddress,
    String? ipAddress,
    String? vendor,
    bool? isKnown,
    String? category,
    DateTime? lastSeen,
  }) {
    return Device(
      id: id ?? this.id,
      macAddress: macAddress ?? this.macAddress,
      ipAddress: ipAddress ?? this.ipAddress,
      vendor: vendor ?? this.vendor,
      isKnown: isKnown ?? this.isKnown,
      category: category ?? this.category,
      lastSeen: lastSeen ?? this.lastSeen,
    );
  }
}
