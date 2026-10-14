# Deudom 💸

> ⏸️ **Proyecto en desarrollo, en pausa hasta nuevo aviso.**
> El código refleja el estado actual del trabajo. Algunas funciones pueden estar incompletas.

**App Android para llevar el control de deudas, préstamos y gastos personales entre amigos y grupos.**

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Koin](https://img.shields.io/badge/DI-Koin-F88909)
![Room](https://img.shields.io/badge/Room-offline--first-3DDC84)
![Gemini](https://img.shields.io/badge/Google-Gemini%20AI-8E75B2?logo=googlegemini&logoColor=white)
![Estado](https://img.shields.io/badge/estado-en%20pausa-lightgrey)

## Qué es

Deudom responde a una pregunta muy común: *¿quién me debe y cuánto?* Permite registrar préstamos a amigos, dividir gastos en grupo, adjuntar fotos de recibos como prueba de pago y ver analíticas de tus finanzas personales.

## Funciones implementadas

- Préstamos y deudas por persona y por grupo, con historial de pagos.
- Registro y análisis de gastos personales (panel con analíticas).
- **Lectura de recibos con IA** (Google Gemini) y OCR con ML Kit + CameraX.
- Importación de amigos desde los contactos.
- Bloqueo de la app con biometría.
- Recordatorios en segundo plano con WorkManager.
- Sistema de recompensas y tutorial inicial.

## Aspectos técnicos

- **Offline-first** con **Room** como fuente de verdad local y sincronización con **Firebase**.
- **MVVM** con inyección de dependencias con **Koin**.
- **UI en Jetpack Compose** con Navigation, Coil y Lottie.

## Compilar

1. Coloca tu `google-services.json` en `app/`.
2. Añade a `local.properties`:
   ```properties
   GEMINI_API_KEY=tu_clave
   ```
3. Ejecuta `./gradlew assembleDebug`.

---
Desarrollado por **[Cukiti](https://github.com/Cukiti)**.
