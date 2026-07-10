class Device {
  final String ipAddress;
  final String macAddress;
  final String? vendor;
  final bool isKnown;
  final String? customName;
  final String? category;

  Device({
    required this.ipAddress,
    required this.macAddress,
    this.vendor,
    this.isKnown = false,
    this.customName,
    this.category,
  });

  Device copyWith({
    String? ipAddress,
    String? macAddress,
    String? vendor,
    bool? isKnown,
    String? customName,
    String? category,
  }) {
    return Device(
      ipAddress: ipAddress ?? this.ipAddress,
      macAddress: macAddress ?? this.macAddress,
      vendor: vendor ?? this.vendor,
      isKnown: isKnown ?? this.isKnown,
      customName: customName ?? this.customName,
      category: category ?? this.category,
    );
  }
}
