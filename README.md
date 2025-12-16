# JOINCO Scanner - Native Android App

## Opis
Natywna aplikacja Android do skanowania kodów kreskowych dla systemu JOINCO.

## Funkcje
- ✅ Natywne skanowanie kodów kreskowych (ML Kit)
- ✅ Automatyczne przekierowanie do JOINCO z parametrem
- ✅ Wsparcie dla EAN, UPC, Code128, Code39, QR i innych formatów
- ✅ Minimalna wersja Android: 8.0 (API 26)

## Jak zbudować APK

### Opcja 1: Android Studio (Zalecana)
1. Zainstaluj Android Studio: https://developer.android.com/studio
2. Otwórz projekt (folder `joinco-android`)
3. Poczekaj aż Gradle zsynchronizuje dependencies
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. APK znajdziesz w: `app/build/outputs/apk/debug/app-debug.apk`

### Opcja 2: Wiersz poleceń (Linux/Mac/Windows z WSL)
```bash
cd joinco-android
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

### Opcja 3: GitHub Actions (CI/CD)
Możesz dodać ten projekt do GitHub i użyć Actions do automatycznego budowania.

## Jak zainstalować na telefonie

### Metoda 1: Bezpośrednia instalacja
1. Prześlij `app-debug.apk` na telefon (email/Dropbox/dysk/USB)
2. Na telefonie: Ustawienia → Bezpieczeństwo → Włącz "Nieznane źródła" dla przeglądarki/menedżera plików
3. Otwórz APK i zainstaluj

### Metoda 2: ADB (przez USB)
```bash
adb install app-debug.apk
```

### Metoda 3: Serwer firmowy
1. Wrzuć APK na serwer: `https://twoja-firma.com/joinco-scanner.apk`
2. Pracownicy otwierają link na telefonie
3. Pobierają i instalują

## Konfiguracja URL

URL jest ustawiony w `MainActivity.kt` linia 23:
```kotlin
private const val JOINCO_URL = "https://joinco.qalcwise.com/AppInstance/Create/0-App-COURIER_DELIVERY?BARCODE="
```

Jeśli trzeba zmienić, edytuj ten plik i przebuduj.

## Uprawnienia

Aplikacja wymaga tylko uprawnień do kamery:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
```

## Testowanie

1. Zainstaluj APK
2. Uruchom aplikację
3. Zezwól na dostęp do kamery
4. Skieruj na kod kreskowy
5. Kod zostanie automatycznie wykryty
6. Aplikacja otworzy przeglądarkę z URL JOINCO

## Rozwiązywanie problemów

### "Gradle sync failed"
- Sprawdź połączenie internetowe
- File → Invalidate Caches → Restart

### "SDK not found"
- Tools → SDK Manager → Zainstaluj Android SDK 34

### APK nie instaluje się
- Sprawdź czy "Nieznane źródła" włączone
- Odinstaluj starą wersję jeśli istnieje

## Wymagania systemowe do budowania

- Java JDK 17+
- Android SDK 34
- Gradle 8.2+
- 4GB RAM minimum

## Wsparcie

W razie problemów sprawdź:
- Android Studio Logcat (dla błędów w aplikacji)
- Build Output (dla problemów z kompilacją)
