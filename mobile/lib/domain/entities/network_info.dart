class NetworkInfo {
  final String ssid;
  final String bssid;
  final bool isSecure;
  final int connectedDevicesCount;

  NetworkInfo({
    required this.ssid,
    required this.bssid,
    required this.isSecure,
    required this.connectedDevicesCount,
  });
}
