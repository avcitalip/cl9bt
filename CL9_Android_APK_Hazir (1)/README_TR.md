# CL9 K24A3 OBD2 — Türkiye Honda Accord CL9 Topluluk Uygulaması (v1.0)

Bu proje, **2004 Honda Accord CL9 K24A3 Otomatik (5AT MCTA / Matsushita RBB ECU)** kullanıcıları için özel olarak tasarlanmış, **Car Scanner ELM OBD2** tarzında ergonomik ve adaptif ELM327 bağlantı motoruna sahip açık kaynaklı Android (Kotlin + Jetpack Compose) uygulamasıdır.

## Öne Çıkan Özellikler
1. **Adaptif ELM327 Bağlantı ve Protokol Motoru (`Elm327ConnectionManager.kt`)**
   - Bluetooth üzerinden bağlandıktan sonra adaptörü otomatik parmak izi testine (`ATI`, `ATPPS`, `ATAL`, `ATRV`) sokar ve **Sınıf 1 (Orijinal PIC18F25K80 v1.5)**, **Sınıf 2 (İyi Klon v1.5)** veya **Sınıf 3 (Kısıtlı/Sahte v2.1)** olarak sınıflandırır.
   - En kaliteli/hızlı protokolden en güvenli/uyumlu protokole doğru 6 kademeli otomatik deneme ve 3-ping gecikme/kararlılık ölçümü yapar:
     1. `ATSP3 + ATAT2 + ATST32` (ISO 9141-2 Hızlı — PIC18F25K80)
     2. `ATSP3 + ATAT1 + ATST64` (ISO 9141-2 Dengeli — Standart/İyi Klon)
     3. `ATSP3 + ATAT0 + ATST96` (ISO 9141-2 Güvenli — Zayıf Klon)
     4. `ATSP4 + ATAT1 + ATST64` (KWP2000 5-Baud Yedek)
     5. `ATSP5 + ATAT1 + ATST64` (KWP2000 Fast Init Yedek)
     6. `ATSP0 + ATAT0 + ATSTFF` (Otomatik Son Çare)
   - Canlı sürüş sırasında art arda kopma/zaman aşımı algılarsa otomatik olarak bir alt güvenli zamanlama kademesine düşer.
2. **Yalnızca K24A3 İçin Doğrulanmış PID ve Türetilmiş Analizler (`K24A3PidEngine.kt`)**
   - `01 00` / `01 20` / `01 40` bitmask keşfi + doğrulanmış tüm K24A3 sensörleri (`ECT`, `MAP`, `RPM`, `VSS`, `STFT`, `LTFT`, `Wideband A/F 01 34`, `BARO`, `EGR`, `EVAP`, `APP D/E`, `TPS`, `PCM Voltaj`).
   - Türetilmiş K24A3 analizleri: **Toplam Yakıt Düzeltme (`STFT + LTFT`)**, **Gerçek AFR (`Lambda × 14.7`)**, **ECT − IAT Sıcaklık Farkı**.
   - 10.4 kbaud K-Line veri yolu bütçesi uyarısı (4–6 PID ideal 2–3 Hz).
3. **Motor + MCTA 5AT Arıza Kodları & Gelişmiş Modlar**
   - Mode 03 (Kayıtlı) & Mode 07 (Bekleyen) Türkçe K24A3 Motor + MCTA Otomatik Şanzıman (`P07xx`/`P08xx`/`P17xx`) kodları.
   - İki aşamalı güvenli Mode 04 arıza silme, Mode 02 Freeze Frame, Mode 01 `01 01` Hazırlık Monitörleri, Mode 06 test eşikleri, Mode 09 (VIN, CalID, CVN), `ATRV` akü voltajı ve CSV veri kaydı.
   - **Dürüstlük İlkesi:** 2004 CL9'da ELM327 ile okunamayan sistemler (ABS/VSA, SRS, EPS, ATF sıcaklığı, VTEC açısı) sahte değerlerle gösterilmez; bunun yerine Klima Self-Test rehberi sunulur.

---

## APK Nasıl Derlenir? (2 Kolay Yöntem)

### Yöntem 1: Bilgisayara Hiçbir Şey Kurmadan GitHub Actions ile (~2 Dakika)
1. GitHub'da ücretsiz yeni bir depo (repository) oluşturun.
2. Bu ZIP içindeki tüm dosya ve klasörleri (`.github` klasörü dahil) depoya yükleyin.
3. GitHub'da **Actions** sekmesine tıklayın; **CL9 K24A3 OBD2 APK Derleyici** otomatik çalışacaktır.
4. İşlem bitince (yaklaşık 2 dakika) **Artifacts** bölümünden `CL9-K24A3-OBD2-Debug-APK` dosyasını indirip Android telefonunuza kurun.

### Yöntem 2: Android Studio ile Yerel Derleme
1. **Android Studio** (Koala veya üzeri) ile bu klasörü **Open** diyerek açın.
2. Gradle senkronizasyonu tamamlandıktan sonra üst menüden **Build > Build Bundle(s) / APK(s) > Build APK(s)** seçeneğine tıklayın.
3. Oluşan APK `app/build/outputs/apk/debug/app-debug.apk` yolunda hazır olacaktır.
