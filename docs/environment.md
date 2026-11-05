# Backend · variables de entorno

| Variable | Default | Descripcion |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | _(vacio)_ | `postgres` para usar PostgreSQL en vez de H2 |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/mallalab` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `mallalab` | usuario |
| `SPRING_DATASOURCE_PASSWORD` | `mallalab` | password |
| `MALLALAB_SEED_ENABLED` | `true` | carga la malla semilla al arrancar |

# Frontend

| Variable | Default | Descripcion |
|---|---|---|
| `VITE_API_URL` | _(vacio)_ | URL publica de la API; vacio usa el proxy de Vite |
