# WebRTC Video/Sesli Görüşme Uygulaması

Turkcell mülakat case'i kapsamında geliştirilen WebRTC tabanlı gerçek zamanlı video/sesli görüşme uygulaması.

---

## Özellikler

- Aynı Wi-Fi ağındaki iki cihaz arasında video ve sesli görüşme
- IP adresi ile arama başlatma
- Ön/arka kamera geçişi
- Mikrofon ve kamera kapatma/açma
- Aktif görüşme sırasında foreground service (bildirim)
- Kendini arama engeli
- MVI mimarisi ile reaktif UI

---

## Mimari

**Multi-module** yapı, **MVI (Model-View-Intent)** pattern ve **Hilt** dependency injection kullanılmıştır.

```
app/
core/
  ├── common/        # BaseViewModel, UiState, UiEffect, UiIntent
  └── webrtc/        # CallManager, BipRtcClient, Signaling
feature/
  ├── splash/        # SplashViewModel → 5sn sonra Home
  ├── home/          # IP girişi, arama başlatma
  ├── host/          # Gelen arama ekranı (sunucu tarafı)
  ├── client/        # Giden arama ekranı (istemci tarafı)
  └── call/          # Aktif görüşme ekranı
```

### Katmanlar

| Katman | Sorumluluk |
|--------|------------|
| `CallManager` | Tüm görüşme akışını yönetir (singleton) |
| `BipRtcClient` | WebRTC PeerConnection, ses/video track'leri |
| `CallSignalingServer` | Java-WebSocket sunucusu (host cihaz, port 3015) |
| `CallSignalingClient` | Java-WebSocket istemcisi (arayan cihaz) |
| `SignalingMessage` | Cihazlar arası JSON mesajları |

### Signaling Akışı

```
Arayan (Client)          Aranan (Host/Server)
     │                          │
     │──── CALL_REQUEST ───────>│
     │                          │── IncomingCall UI
     │<─── CALL_ACCEPT ─────────│
     │                          │
     │──── OFFER (SDP) ────────>│
     │<─── ANSWER (SDP) ────────│
     │<──> ICE Candidates ─────>│
     │                          │
     │    *** Görüşme Aktif ***  │
     │                          │
     │──── END_CALL ───────────>│
```

### Race Condition Çözümleri

- **Remote video geç bağlanma:** `remoteVideoTrack` alanı tutulur; yüzey ve track hangisi önce hazır olursa diğerini bekler.
- **ICE candidate yarışı:** Remote SDP set edilmeden gelen adaylar `pendingIceCandidates` listesinde bekletilir, SDP sonrası boşaltılır.
- **Tekrar arama:** Her görüşme sonunda `BipRtcClient.resetForNewCall()` ile taze `PeerConnection` oluşturulur.

---

## Teknolojiler

| Teknoloji | Kullanım                   |
|-----------|----------------------------|
| Kotlin | Yazılım dili               |
| Jetpack Compose | UI                         |
| WebRTC (mesibo) | Ses/video                  |
| Java-WebSocket | Signaling sunucu/istemci   |
| Hilt | Dependency injection       |
| Kotlin Coroutines + StateFlow | Asenkron akış              |
| Navigation 3 | Ekranlar arası geçiş       |
| Gson | JSON serialize/deserialize |
| MockK + Turbine | Unit test                  |

---
