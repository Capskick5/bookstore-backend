# BookVerse - Huong dan setup de bat dau code

Tai lieu nay danh cho thanh vien moi pull repo ve may va muon chay duoc moi truong backend + database truoc khi nhan task.

## 1. Can cai truoc

Kiem tra may da co cac cong cu sau:

```bash
java -version
docker --version
docker compose version
git --version
```

Yeu cau:

- Java 21.
- Docker Desktop dang chay.
- Git.
- Python/RAG chi can neu ban lam phan AI. Neu khong lam RAG thi co the bo qua buoc RAG.

## 2. Lay code moi nhat

```bash
git pull --rebase origin main
```

Neu clone lan dau:

```bash
git clone <repo-url>
cd backend
```

## 3. Tao file moi truong

Tu thu muc `backend/`:

```bash
cp .env.example .env
```

Dung mac dinh local la du de chay:

```env
POSTGRES_DB=bookverse
POSTGRES_USER=bookverse
POSTGRES_PASSWORD=changeme
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bookverse
SPRING_DATASOURCE_USERNAME=bookverse
SPRING_DATASOURCE_PASSWORD=changeme
JWT_SECRET=please-change-this-to-a-long-random-secret-min-32-chars
RAG_SERVICE_URL=http://localhost:8000
QDRANT_URL=http://localhost:6333
MONGO_URL=mongodb://localhost:27017
```

Khong commit `.env`.

## 4. Bat database local

Tu thu muc `backend/`:

```bash
docker compose -f infra/docker-compose.yml up -d postgres qdrant mongo
```

Kiem tra container:

```bash
docker compose -f infra/docker-compose.yml ps
```

Muon tat:

```bash
docker compose -f infra/docker-compose.yml down
```

Muon xoa sach data local va tao lai tu dau:

```bash
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d postgres qdrant mongo
```

## 5. Chay backend Spring Boot

Tu thu muc `backend/`:

```bash
cd bookstore
./mvnw spring-boot:run
```

Khi app start, Flyway se tu tao schema va seed data mau.

Backend chay tai:

```text
http://localhost:8080
```

Health check:

```text
http://localhost:8080/api/health
```

Swagger:

```text
http://localhost:8080/swagger-ui
```

## 6. Tai khoan mau

Sau khi backend chay lan dau, he thong co seed san:

```text
Admin:
email: admin@example.com
password: adminpassword123

Customer:
email: test@example.com
password: password123
```

Ngoai ra DB co san:

- 4 categories.
- 12 books mau.

## 7. Cach test login tren Swagger

Mo:

```text
http://localhost:8080/swagger-ui
```

Goi endpoint:

```text
POST /api/auth/login
```

Body mau:

```json
{
  "email": "admin@example.com",
  "password": "adminpassword123"
}
```

Copy `accessToken` trong response.

Bam nut `Authorize` tren Swagger va nhap:

```text
Bearer <accessToken>
```

Sau do co the goi cac endpoint can dang nhap, vi du:

```text
GET /api/auth/me
```

## 8. Chay test truoc khi code va truoc khi push

Tu thu muc `backend/bookstore/`:

```bash
./mvnw test
```

Neu test bi loi ket noi DB, kiem tra lai:

```bash
docker compose -f ../infra/docker-compose.yml ps
```

Phai thay PostgreSQL dang `running`.

## 9. Chay RAG service neu lam AI

Tu thu muc `backend/rag/`:

```bash
uv sync
uv run main.py
```

RAG service chay tai:

```text
http://localhost:8000
```

Health:

```text
http://localhost:8000/health
```

Neu may chua co `uv`, cai theo huong dan cua uv hoac bao lead de setup. Nguoi khong lam AI/RAG co the bo qua buoc nay.

## 10. Quy trinh truoc khi nhan task

Moi thanh vien can lam du cac buoc sau truoc khi code feature:

1. Pull code moi nhat.
2. Tao `.env`.
3. Bat Docker database.
4. Chay backend thanh cong.
5. Mo duoc `/api/health`.
6. Mo duoc Swagger.
7. Login duoc tai khoan admin hoac customer.
8. Chay `./mvnw test` xanh.

Neu mot trong cac buoc tren loi, bao vao Discord kem:

```text
May dang dung:
Lenh da chay:
Log loi:
Da thu cach nao:
```

## 11. Quy uoc khi bat dau code

Truoc khi code:

```bash
git pull --rebase origin main
```

Trong luc code:

- Chi sua file thuoc task cua minh.
- Neu can sua file dung chung nhu `pom.xml`, `SecurityConfig`, migration, CI, thong bao truoc tren Discord.
- Commit nho, message ro nghia.

Vi du:

```bash
git add .
git commit -m "feat(catalog): add book listing endpoint"
git pull --rebase origin main
git push origin main
```

Khong commit:

- `.env`
- API key
- file sach PDF/EPUB
- data local
- file trong `_local/`

## 12. Loi thuong gap

### Docker chua chay

Loi thuong thay:

```text
Connection refused localhost:5432
```

Xu ly:

```bash
docker compose -f infra/docker-compose.yml up -d postgres qdrant mongo
```

### Port 5432 da bi dung

Kiem tra app nao dang dung port PostgreSQL. Neu may da co PostgreSQL local, tat PostgreSQL local hoac doi port trong `infra/docker-compose.yml`.

### Swagger mo duoc nhung endpoint protected bi 401

Ban chua bam `Authorize` hoac nhap sai token. Can nhap theo format:

```text
Bearer <accessToken>
```

### Test fail vi DB cu

Neu schema local bi lech, reset volume:

```bash
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d postgres qdrant mongo
cd bookstore
./mvnw test
```

