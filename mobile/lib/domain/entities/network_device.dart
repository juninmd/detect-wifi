enum DeviceCategory { smartphone, iot, computer, unknown }
enum DeviceStatus { known, unknown, blocked }

class NetworkDevice {
  final String ip;
  final String mac;
  final String? vendor;
  final DeviceCategory category;
  final DeviceStatus status;
  final DateTime lastSeen;

  const NetworkDevice({
    required this.ip,
    required this.mac,
    this.vendor,
    this.category = DeviceCategory.unknown,
    this.status = DeviceStatus.unknown,
    required this.lastSeen,
  });

  NetworkDevice copyWith({
    String? ip,
    String? mac,
    String? vendor,
    DeviceCategory? category,
    DeviceStatus? status,
    DateTime? lastSeen,
  }) {
    return NetworkDevice(
      ip: ip ?? this.ip,
      mac: mac ?? this.mac,
      vendor: vendor ?? this.vendor,
      category: category ?? this.category,
      status: status ?? this.status,
      lastSeen: lastSeen ?? this.lastSeen,
    );
  }
}
