12 RENK ÇARKI — Fabric 1.21.1

Özellikler
- 12 renkli rastgele çark mantığı; seçimden önce yaklaşık 3 saniyelik actionbar çark animasyonu
- Her 15 dakikada bir seçim
- Seçilen renge ait dünyadaki renkli bloklar silinir
- Envanterdeki eşyalara dokunulmaz
- Yeni yüklenen chunk'larda yasak renk temizlenmeye devam eder
- Olay tüm dünyalara değil, oyuncuların o anda yüklediği/çevresindeki chunk'lara uygulanır; bu performans için bilerek böyle tasarlandı.

Kurulum
1. Minecraft Java 1.21.1 + Fabric Loader kur.
2. Fabric API'yi mods klasörüne koy.
3. Bu projenin derlenmiş JAR dosyasını mods klasörüne koy.
4. Dünyayı aç.

Derleme
- Java 21 gerekir.
- Gradle ile `./gradlew build` çalıştır.
- Çıktı: build/libs/renk-carki-1.0.0.jar

Not
Bu paket kaynak proje olarak verildi. Bu çalışma ortamında Maven/Fabric sunucularına ağ erişimi olmadığı için burada doğrudan derlenmiş JAR üretemiyorum.
