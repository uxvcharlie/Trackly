# Trackly

Tablero personal de escritorio: finanzas, recordatorios con pagos y horario en un solo lugar. Programación de Escritorio, UAM.

**Tecnología:** JavaFX 21, Hibernate 7.4, PostgreSQL y jBCrypt (Java 21, Maven).

## Cómo correrlo

1. Crear la base y las tablas (una vez, cada quien en su PostgreSQL):

   ```bash
   psql -U postgres -h localhost -c "CREATE DATABASE bdtrackly"
   psql -U postgres -h localhost -d bdtrackly -f tablero.sql
   ```

2. La clave de PostgreSQL va en una variable de entorno, nunca en el código (ver `.env.example`):

   ```bash
   export DB_PASSWORD=tu_clave
   ```

   En IntelliJ: Run → Edit Configurations → Environment variables → `DB_PASSWORD=tu_clave`.

3. Ejecutar:

   ```bash
   mvn javafx:run
   ```

## Cómo se organiza

```text
src/main/java/com/example/trackly/
├── database/    JPAUtil, Sesion
├── model/       entidades
├── dao/         un DAO por tabla
├── servicio/    cuentas del Inicio, avisos y reporte PDF
└── controller/  un controlador por pantalla
src/main/resources/com/example/trackly/   FXML y CSS
src/main/resources/com/example/trackly/reportes/   reportes de JasperReports (.jrxml)
tablero.sql                               script de la base
```

## Reporte mensual (JasperReports)

El botón **Reporte PDF** de la barra superior guarda el reporte del mes en PDF:

- Resumen: presupuesto, gastado, ingresos, pagos pendientes, disponible y proyección a fin de mes (las mismas cuentas del Inicio, salen de `ResumenMes`).
- Movimientos del mes agrupados por categoría, con subtotales y balance.
- Gráficas de gastos por categoría y por materia.
- Recordatorios del mes (pagados, pendientes y atrasados) y el horario con lo gastado en cada clase.

| Archivo | Qué es |
|---|---|
| `reportes/trackly_reporte_mensual.jrxml` | reporte principal |
| `reportes/trackly_recordatorios.jrxml` | subreporte de recordatorios |
| `reportes/trackly_horario.jrxml` | subreporte del horario |
| `servicio/ReporteMensual.java` | compila los `.jrxml`, los llena con la conexión de Hibernate y exporta el PDF |

Para editarlos en Jaspersoft Studio 7.0.8:

1. Crear un Data Adapter llamado `Trackly` (PostgreSQL, `jdbc:postgresql://localhost:5432/bdtrackly`, usuario `postgres`).
2. Abrir los tres `.jrxml` y compilar primero los dos subreportes (crean los `.jasper`, que no se suben).
3. Previsualizar el principal con el `ID_USUARIO` propio. En Studio las tarjetas del resumen salen con "—" porque esos valores los manda la app.

## Cómo trabajamos

- Una rama por módulo: `finanzas`, `recordatorios`, `horario`, `inicio`.
- Antes de empezar: `git pull` en `main` y `git merge main` en la rama propia.
- Entidades, DAO y `tablero.sql` los cambia quien lleva la base. Si hace falta algo nuevo, se pide.
- Mensajes de commit con formato convencional: `feat(finanzas): guardar gasto con clase`.
- No se sube nada de claves ni configuración local (`.gitignore`).
