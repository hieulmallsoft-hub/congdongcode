# CodeTogether

Du an cong dong lap trinh voi Spring Boot 3 + JWT + PostgreSQL/H2 va React.

## Chuc nang

- Dang ky, dang nhap bang JWT
- Dang cau hoi loi code, bai tap kho hoac chu de thao luan
- Dinh kem doan code, ngon ngu lap trinh va tags cho bai dang
- Tra loi/gop y loi giai cho bai dang cua cong dong
- Hien thi mot bai tap code moi tren trang chinh moi ngay
- Nop loi giai cho bai tap hang ngay
- Seed san 3 daily challenges mau khi database chua co du lieu

## Chay backend

Sua thong tin PostgreSQL trong `backend/src/main/resources/application.properties` neu can:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/codetogether}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:123456}
```

Co the bat PostgreSQL bang Docker:

```bash
docker compose up -d
```

Sau do chay backend:

```bash
cd backend
mvn spring-boot:run
```

Backend chay tai `http://localhost:8080`.

Neu muon chay nhanh bang H2 in-memory:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Du lieu H2 se mat khi tat backend. Console H2 o `http://localhost:8080/h2-console`, JDBC URL la `jdbc:h2:mem:codetogether`.

## Chay frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend chay tai `http://localhost:5173`.

## API chinh

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/community/feed`
- `POST /api/community/posts`
- `POST /api/community/posts/{postId}/answers`
- `GET /api/community/challenges/daily`
- `GET /api/community/challenges`
- `POST /api/community/challenges/{challengeId}/submissions`
