# Karanlık Mod (Ekran Filtresi)

OPPO AX7 (CPH1903), Android 8.1.0 için hazırlanmış, root ve ADB gerektirmeyen
bir ekran filtresi uygulaması.

## Ne yapar?

- **Renk Tersleme butonu:** Ekranın tamamına, altındaki her rengi tersine
  çeviren bir katman ekler (beyaz zeminler siyaha döner). Sistem geneli
  "renk tersleme" hissi verir; gerçek Erişilebilirlik > Renk Tersleme
  özelliği kadar kusursuz olmayabilir ama pratikte iyi çalışır.
- **Ekstra Karartma butonu:** Ekranın üzerine yarı saydam siyah bir katman
  ekler, en düşük parlaklığın da altına inmiş gibi bir etki sağlar.

İkisi de bağımsız olarak açılıp kapatılabilir, birlikte de kullanılabilir.

## GitHub Actions ile APK almak

1. Bu ZIP'in içindekileri yeni bir GitHub reposuna yükleyin (repo kök
   dizininde `app/`, `build.gradle`, `settings.gradle`,
   `.github/workflows/build.yml` görünmeli).
2. Repoya push ettiğinizde (veya Actions sekmesinden "Run workflow" ile elle
   tetiklediğinizde) `Build APK` iş akışı otomatik çalışır.
3. İş akışı bitince Actions sekmesinde ilgili çalışmanın altında
   **Artifacts** kısmında `karanlik-mod-apk` adlı bir dosya belirir. Bunu
   indirip içinden `app-debug.apk` dosyasını telefonunuza aktarın.
4. Telefonda "Bilinmeyen kaynaklardan yükleme" iznini açıp APK'yı kurun.

## Kurulum sonrası kullanım

1. Uygulamayı açın, butonlardan birine ilk kez bastığınızda Android sizi
   "Diğer uygulamaların üzerinde göster" izin ekranına yönlendirecek.
2. İzni açın, geri tuşuyla uygulamaya dönün, butona tekrar basın.
3. Bildirim çubuğunda "Ekran Filtresi" bildirimini gördüğünüzde filtre
   aktiftir. Butonu tekrar kapatarak filtreyi kaldırabilirsiniz.

## Notlar

- Renk tersleme katmanı GPU üzerinde bir karışım modu (blend mode) ile
  çalışır; bazı cihaz/GPU sürücülerinde görsel sonuç ufak farklılıklar
  gösterebilir. Test edip gerekirse `OverlayService.kt` içindeki
  `DIM_ALPHA` değerini (karartma yoğunluğu) değiştirebilirsiniz.
- Uygulama minSdk 27 / targetSdk 27 olarak ayarlandı, doğrudan
  CPH1903'ün Android 8.1.0 sürümüne göre yapılandırıldı.
