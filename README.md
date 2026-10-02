# LEADEN1 Volume Diagnostic V5.1

Repositorio independiente para investigar los botones laterales de volumen de las gafas LEADEN1.

Application ID: `com.leaden1.volumediagnostic`

No modifica V4 ni la PWA.

## Construcción

GitHub Actions -> `Build LEADEN1 Volume Diagnostic V5.1`

Artifact -> `LEADEN1-Volume-Diagnostic-V5.1`

## Prueba

1. Instalar el APK.
2. Abrir V5.1.
3. Pulsar `ABRIR ACCESO A NOTIFICACIONES`.
4. Activar `LEADEN1 Media Monitor V5.1`.
5. Regresar a V5.1.
6. Pulsar `ACTUALIZAR SESIONES`.
7. Reproducir música.
8. Probar botón central.
9. Probar volumen +.
10. Probar volumen -.
11. Guardar/capturar el historial.

Objetivo: determinar si los botones laterales generan KEY_DOWN, cambios de STREAM_MUSIC, broadcasts observables, o son procesados por Android/HyperOS sin entregar el evento a la aplicación.
