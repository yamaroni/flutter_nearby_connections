part of flutter_nearby_connections;

const _startAdvertisingPeer = 'start_advertising_peer';
const _stopAdvertisingPeer = 'stop_advertising_peer';
const _startBrowsingForPeers = 'start_browsing_for_peers';
const _stopBrowsingForPeers = 'stop_browsing_for_peers';
const _invitePeer = 'invite_peer';
const _disconnectPeer = 'disconnect_peer';
const _sendMessage = 'send_message';
const _stopAllConnections = 'stop_all_connections';

class NearbyService {
  static const MethodChannel _channel = const MethodChannel('flutter_nearby_connections');

  /// Begins advertising the service provided by a local peer.
  /// The [startAdvertisingPeer] publishes an advertisement for a specific service
  /// that your app provides through the flutter_nearby_connections plugin and
  /// notifies its delegate about invitations from nearby peers.
  FutureOr<dynamic> startAdvertisingPeer() async {
    await _channel.invokeMethod(_startAdvertisingPeer);
  }

  /// Starts browsing for peers.
  /// Searches (by [serviceType]) for services offered by nearby devices using
  /// infrastructure Wi-Fi, peer-to-peer Wi-Fi, and Bluetooth or Ethernet, and
  /// provides the ability to easily invite those [Device] to a earby connections
  /// session [SessionState].
  FutureOr<dynamic> startBrowsingForPeers() async {
    await _channel.invokeMethod(_startBrowsingForPeers);
  }

  /// Stops advertising this peer device for connection.
  FutureOr<dynamic> stopAdvertisingPeer() async {
    await _channel.invokeMethod(_stopAdvertisingPeer);
  }

  /// Stops browsing for peers.
  FutureOr<dynamic> stopBrowsingForPeers() async {
    await _channel.invokeMethod(_stopBrowsingForPeers);
  }

  /// Invites a discovered peer to join a nearby connections session.
  /// the [deviceID] is current Device
  FutureOr<dynamic> invitePeer({
    required String deviceID,
    required String? outletName,
  }) async {
    await _channel.invokeMethod(
      _invitePeer,
      <String, dynamic>{
        'deviceId': deviceID,
        'outletName': outletName,
      },
    );
  }

  /// Disconnects the local peer from the session.
  /// the [deviceID] is current Device
  FutureOr<dynamic> disconnectPeer({
    required String? deviceID,
  }) async {
    await _channel.invokeMethod(_disconnectPeer, <String, dynamic>{
      'deviceId': deviceID,
    });
  }

  /// Sends a message encapsulated in a Data instance to nearby peers.
  FutureOr<dynamic> sendMessage(String deviceID, String message) async {
    await _channel.invokeMethod(_sendMessage, <String, dynamic>{
      'deviceId': deviceID,
      'message': message,
    });
  }

  /// Stops all connections.
  FutureOr<dynamic> stopAllConnections() async {
    await _channel.invokeMethod(_stopAllConnections);
  }
}
