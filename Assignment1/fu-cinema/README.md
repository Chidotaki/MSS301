# FUCinema Booking System

Assignment 1 for **MSS301** – Cinema Ticket Booking System using a microservices architecture and an API Gateway.

## Architecture

| Service | Port | Database | Main responsibility |
|---|---:|---|---|
| Customer Service | 8081 | SQL Server | Authentication, customer profile, admin customer management |
| Movie Service | 8082 | MongoDB | Genres, cinema rooms, movies, showtimes |
| Booking Service | 8083 | MySQL | Booking, seat map, history, cancellation, report |
| API Gateway | 9000 | — | Routing, JWT authentication, role-based authorization |

All client requests should be sent through:

```text
http://localhost:9000
```

## Main Technologies

- Java
- Spring Boot
- Spring Data JPA
- Spring Data MongoDB
- Spring Cloud OpenFeign
- Spring Security / JWT
- Spring Cloud Gateway MVC
- Flyway
- SQL Server 2022
- MongoDB 7
- MySQL 8
- Docker Compose
- Postman

## Run Guide

### 1. Start databases

From the repository root:

```bash
docker compose up -d
```

Expected containers:

```text
cinema-sqlserver
cinema-sqlserver-init
cinema-mongo
cinema-mysql
```

Wait until SQL Server is healthy and the init container has completed successfully.

### 2. Start Customer Service

```bash
cd customer-service
mvn spring-boot:run
```

Runs at `http://localhost:8081`.

### 3. Start Movie Service

Open another terminal:

```bash
cd movie-service
mvn spring-boot:run
```

Runs at `http://localhost:8082`.

MongoDB seed data is created automatically when the database is empty.

### 4. Start Booking Service

Open another terminal:

```bash
cd booking-service
mvn spring-boot:run
```

Runs at `http://localhost:8083`.

Booking Service communicates with Movie Service through OpenFeign.

### 5. Start API Gateway

Open another terminal:

```bash
cd api-gateway
mvn spring-boot:run
```

Runs at `http://localhost:9000`.

### 6. Quick verification

Gateway health:

```bash
curl http://localhost:9000/actuator/health
```

Expected:

```json
{"status":"UP"}
```

Public movie API:

```bash
curl http://localhost:9000/api/movies
```

Protected API without token:

```bash
curl -i http://localhost:9000/api/customers/me
```

Expected status:

```text
401 Unauthorized
```

## Test Accounts

### Admin

```text
Email:    admin@fucinema.com
Password: @@abc123@@
Role:     ADMIN
```

### Seed Customer 1

```text
Name:     Nguyễn Văn An
Email:    an@gmail.com
Password: 123456
Status:   ACTIVE
Role:     CUSTOMER
```

### Seed Customer 2

```text
Name:     Trần Thị Bình
Email:    binh@gmail.com
Password: 123456
Status:   ACTIVE
Role:     CUSTOMER
```

### Inactive Customer

```text
Name:     Lê Minh Chi
Email:    chi@gmail.com
Password: 123456
Status:   INACTIVE
```

The inactive account is used to verify the `403 Forbidden` login case.

> These credentials and secrets are for the local MSS301 assignment environment only and should not be reused in a production system.

## Postman Testing

Postman files:

```text
postman/FUCinemaBookingSystem.postman_collection.json
postman/FUCinema-Local.postman_environment.json
```

Import both files into Postman and select:

```text
FUCinema-Local
```

Run the collection in this order:

```text
01-Auth
02-Customer
03-Genre-Room
04-Movie
05-Showtime
06-Booking
07-History-Cancel
08-Report
```

Important environment variables include:

```text
adminToken
customerToken
customer2Token
genreId
roomId
movieId
showtimeId
bookingId
booking2Id
booking3Id
```

Expected Collection Runner result:

```text
All tests Passed
0 Failed
```

## Main API Groups

```text
/api/auth/**
/api/customers/**
/api/genres/**
/api/rooms/**
/api/movies/**
/api/showtimes/**
/api/bookings/**
```

Public requests, CUSTOMER-only requests and ADMIN-only requests are authorized at the API Gateway according to JWT role information.

## Database Notes

- Customer Service uses SQL Server database `cinema_customer`.
- Movie Service uses MongoDB database `cinema_movie`.
- Booking Service uses MySQL database `cinema_booking`.
- Flyway manages relational database schema creation.
- Movie Service automatically seeds genres, rooms, movies and showtimes when MongoDB is empty.

## Submission Notes

Before submission, each service should build successfully:

```bash
mvn clean package -DskipTests
```

The Postman Collection Runner should finish with:

```text
0 Failed
```

Export the Postman collection and environment and include them with the repository.
