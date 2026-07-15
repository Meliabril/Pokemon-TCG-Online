# Instalacion y ejecucion

## Requisitos

Backend:

- Java 21.
- Maven Wrapper incluido en `BE/mvnw` y `BE/mvnw.cmd`.
- Puerto por defecto: `8080`.

Frontend:

- Node.js compatible con Angular 21.
- npm.
- Puerto por defecto: `4200`.

No se encontro archivo `.nvmrc` ni `.node-version`, por lo que la version exacta de Node no esta fijada en el repositorio.

## Backend

Desde la carpeta `BE/`:

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Configuracion principal:

- Archivo: `BE/src/main/resources/application.properties`.
- Base activa actual: H2 en memoria.
- URL configurada: `jdbc:h2:mem:pokemon_tcg;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE`.
- Flyway: presente en dependencias y migraciones, pero desactivado con `spring.flyway.enabled=false`.
- Hibernate: `spring.jpa.hibernate.ddl-auto=create`.

Endpoints utiles:

- Health: `GET http://localhost:8080/api/health`.
- Ping: `GET http://localhost:8080/ping`.
- Swagger UI: `http://localhost:8080/swagger-ui.html`.
- H2 console: `http://localhost:8080/h2-console`.

## Frontend

Desde la carpeta `FE/`:

```bash
npm install
npm start
```

El frontend queda disponible en:

```text
http://localhost:4200
```

Comandos disponibles:

```bash
npm start
npm run build
npm test
```

## Variables y propiedades relevantes

Backend:

- `APP_VERSION`: version mostrada por la app.
- `APP_URL`: URL base del backend.
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: documentadas como override, aunque la configuracion activa usa H2 en memoria.
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`: configuracion de correo.
- `app.jwt.secret`: secreto JWT. Si no se configura, el servicio usa un fallback inseguro para desarrollo.
- `app.websocket.allowed-origins`: origen permitido para WebSocket, actualmente `http://localhost:4200`.

Frontend:

- `FE/src/environments/environment.ts`.
- `FE/src/environments/environment.production.ts`.

## Flujo minimo para probar localmente

1. Levantar backend.
2. Levantar frontend.
3. Registrar usuario desde la pantalla de registro o por API.
4. Verificar cuenta si el flujo lo requiere.
5. Iniciar sesion.
6. Importar cartas XY1 con un usuario admin.
7. Crear o randomizar mazo.
8. Entrar a configuracion de partida o cola.

## Advertencias

- La configuracion actual no representa una base PostgreSQL productiva.
- La base H2 en memoria se recrea al levantar la aplicacion.
- Flyway esta desactivado en runtime, aunque existen migraciones en el repositorio.
- No se detectaron Dockerfile, docker-compose ni pipeline CI/CD.
