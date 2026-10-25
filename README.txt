Shared Waypoints (Fabric 1.21.4)

[I] - dodaj waypoint (domyślnie na Twojej pozycji)
[G] - ustawienia + lista waypointów (ukryj / edytuj / usuń)

Waypointy są trzymane na serwerze (plik sharedwaypoints.json w folderze świata)
i wysyłane do KAŻDEGO gracza, który ma moda. Mod musi być na serwerze i u graczy.
Edytować / usuwać może właściciel waypointu lub OP.

Budowanie:
  1. Zainstaluj JDK 21 i Gradle 8.12+
  2. W folderze projektu: gradle wrapper   (jednorazowo)
  3. ./gradlew build   (Windows: gradlew.bat build)
  4. Gotowy plik: build/libs/sharedwaypoints-1.0.0.jar
  5. Wrzuć do mods/ razem z Fabric API 0.119.2+1.21.4
