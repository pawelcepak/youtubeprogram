# Quiz Video Studio — Android

Pierwsza natywna wersja Android programu do tworzenia quizów flagowych.

## Android v0.1
- quiz flagowy PL/EN,
- katalog 257 państw/terytoriów PL+EN,
- flagi, tła i muzyka z telefonu lub Google Drive przez systemowy selektor Androida,
- opcjonalne tła: cały film / poziom / kraj,
- blur i przyciemnienie,
- muzyka 0–100% + zachowane generowane SFX,
- podgląd pytania i render całego quizu,
- zapis/odczyt projektu JSON,
- eksport MP4 bezpośrednio do wybranego miejsca, w tym Google Drive.

Kod źródłowy Android Studio znajduje się w `android-source.zip`. GitHub Actions buduje z niego APK.

## Drive
Aplikacja używa Android Storage Access Framework zamiast własnego formularza logowania. Konto Google pozostaje obsługiwane przez Android/Google Drive; aplikacja otrzymuje dostęp wyłącznie do plików wskazanych przez użytkownika.
