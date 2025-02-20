# Open Video Conference (OVC) Backend Documentation
Java Backend API for DVB Booking Portal

Backend application created using:
* Spring Boot
* Spring Web MVC Rest Controllers + Swagger
* Spring JPA + Hibernate
* Postgresql DB

## REST API Documentation (OpenAPI 3.0)
The OVC backend API has an auto-generated Swagger (OpenAPI 3.0) documentation.
One can see the REST endpoints documentation as a web page in a web-browser.

In order to see the documentation, the backend application must be running.
The url for that page follows the pattern:
`https://{{ backend-app-domain }}/swagger-ui/index.html`

For instance, for the integration environment, the url is:
https://ovc-backend.nordeck.io/swagger-ui/index.html.

## Running the docker containers for DB & App
```sh
docker compose build
docker compose up
```

## Application health
We use the Spring Actuator endpoint in order to check if the application is running:
``` 
http://{{ backend-app-domain }}/actuator/health
```
The response should be:
```json
{
  "status": "UP"
}
```
