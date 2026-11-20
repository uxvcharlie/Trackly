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
├── servicio/    cuentas del Inicio y avisos
└── controller/  un controlador por pantalla
src/main/resources/com/example/trackly/   FXML y CSS
tablero.sql                               script de la base
```

## Cómo trabajamos

- Una rama por módulo: `finanzas`, `recordatorios`, `horario`, `inicio`.
- Antes de empezar: `git pull` en `main` y `git merge main` en la rama propia.
- Entidades, DAO y `tablero.sql` los cambia quien lleva la base. Si hace falta algo nuevo, se pide.
- Mensajes de commit con formato convencional: `feat(finanzas): guardar gasto con clase`.
- No se sube nada de claves ni configuración local (`.gitignore`).
