# Entorno de Base de Datos Local (Docker)

Este directorio contiene la definición de servicios de infraestructura local para desarrollo, aislado de las carpetas `backend/` y `frontend/`.

## Base de Datos PostgreSQL de Desarrollo
- **Imagen:** `postgres:16-alpine`
- **Puerto:** `localhost:5432`
- **Base de Datos:** `restaurant_db`
- **Usuario:** `postgres`
- **Contraseña:** `postgres`
- **Zona Horaria:** `America/La_Paz`
- **Persistencia de Datos:** Almacenada fuera del repositorio de Git en `${HOME}/.postgres-data/gestion-restaurante`.

## Comandos Útiles

### Iniciar la base de datos en segundo plano:
```bash
docker compose -f docker/docker-compose.yml up -d
```

### Detener la base de datos (conservando los datos):
```bash
docker compose -f docker/docker-compose.yml stop
```

### Reiniciar la base de datos a cero (borrar datos y recrear limpia):
```bash
docker compose -f docker/docker-compose.yml down
rm -rf ~/.postgres-data/gestion-restaurante
docker compose -f docker/docker-compose.yml up -d
```

### Ver logs del contenedor:
```bash
docker compose -f docker/docker-compose.yml logs -f postgres
```
