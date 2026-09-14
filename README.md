# ms-rutaexpress-report

Microservicio de KPIs/analytics de RutaExpress. Consume `shipments.events`
de Kafka y expone agregaciones de solo lectura para el rol **Admin**.

## Variables de entorno (`.env`)

```
TENANT_ID=<tenantId de Azure AD>
API_CLIENT_ID=<apiClientId>
DB_HOST=localhost

```


## Correr local

```bash
mvn spring-boot:run
```

## Endpoints

- `GET /api/report/kpis?range=last24h|last7d|last30d` — envíos por estado,
  envíos activos y lead time promedio (minutos). Requiere rol `Admin`.
- `GET /api/report/top-services?range=last7d` — servicios más usados por
  cantidad de envíos creados. Requiere rol `Admin`.
