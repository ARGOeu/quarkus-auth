# Quarkus Auth Extension Example

This project is a simple REST API that demonstrates the integration and usage of the **quarkus-auth** extension.

## Prerequisites

Before running the application, make sure you have the following installed:

* Java 17
* Maven 3.9.9+
* Docker 20.10.6+

## Running Keycloak

The example uses Keycloak for authentication and authorization.

To start Keycloak, navigate to the `example/keycloak` directory and run:

```bash
docker-compose up
```

Wait until all Keycloak services are fully started before launching the application.

## Running the Example Application

Navigate to the `example` directory and start the application in development mode:

```bash
mvn clean quarkus:dev
```

Once the application is running, open:

```text
http://localhost:8080
```

## Welcome Page

The application provides a welcome page containing two actions:

### Visit API Documentation

Redirects you to the OpenAPI/Swagger documentation where you can explore:

* The REST endpoints provided by the example application.
* The endpoints exposed by the `quarkus-auth` extension.

### Obtain an Access Token

Redirects you to a page that helps you obtain an access token from Keycloak for testing secured endpoints.

## Available Users

The following test users are pre-configured:

| Username | Password | Role                |
| -------- | -------- | ------------------- |
| admin    | admin    | Super Administrator |
| user1    | user1    | Standard User       |
| user2    | user2    | Standard User       |
| user3    | user3    | Standard User       |

Use these credentials to authenticate through Keycloak and test the available APIs.

## Keycloak Account / Group Management (RCIAM)

To access the RCIAM group management / account console as an admin, use the following URL:

```text
http://localhost:58080/realms/rciam/account
```

Login with:

* Username: `user1`
* Password: `user1`

## Purpose

This example demonstrates how a REST API can integrate the **quarkus-auth** extension to provide authentication and authorization capabilities using Keycloak.
