# BookVerse — Execution + RBL/Paper Plan

## 1. Muc tieu thuc te

Du an co hai san pham can lam song song:

1. **San pham ky thuat**: website nha sach online co backend Spring Boot, PostgreSQL, RAG service Python, Qdrant, MongoDB, OpenAI, Swagger, test va demo chay duoc.
2. **San pham hoc thuat/RBL**: bai bao hoac report nghien cuu ve mot van de lien quan den du an, co cau hoi nghien cuu, co phuong phap, co thuc nghiem nho, co ket qua va co lien he truc tiep voi BookVerse.

Nguyen tac quan trong: khong viet bai bao tach roi du an. Nen de bai RBL dung chinh RAG chatbot cua BookVerse lam doi tuong nghien cuu. Nhu vay cong code va cong nghien cuu bo tro nhau.

## 2. Huong RBL nen chon

### De tai khuyen nghi

**Improving Online Bookstore Chatbot Reliability Using Retrieval-Augmented Generation**

Ten tieng Viet:

**Nang cao do tin cay cua chatbot nha sach truc tuyen bang Retrieval-Augmented Generation**

### Ly do chon de tai nay

De tai nay khop voi du an nhat vi:

- Du an da co RAG service trong `/rag`.
- Co the do luong bang cac metric don gian: answer correctness, source citation accuracy, hallucination rate, response latency.
- Khong can nghien cuu qua rong ve thuong mai dien tu, thanh toan, hay hanh vi nguoi dung.
- Co the demo truc tiep: hoi chatbot ve sach/chinh sach cua cua hang, so sanh cau tra loi co RAG va khong RAG.

### Cau hoi nghien cuu

RQ1: RAG co lam giam ti le chatbot tra loi sai hoac bia thong tin trong mien du lieu nha sach hay khong?

RQ2: Viec them retrieved sources vao prompt co cai thien do dung cua cau tra loi va kha nang truy vet nguon hay khong?

RQ3: Doi lai, RAG anh huong the nao den latency va do phuc tap he thong?

## 3. Pham vi RBL vua suc

Khong nen co gang lam mot bai ve "AI in E-commerce" qua rong. Pham vi nen dong lai nhu sau:

- Domain: online bookstore.
- Task: chatbot hoi dap va goi y sach.
- Method: Retrieval-Augmented Generation voi embedding + vector search.
- Baseline: LLM khong retrieval hoac fake/simple keyword search.
- Dataset: mo ta sach, FAQ, chinh sach cua cua hang, mot so noi dung PDF/EPUB neu co.
- Evaluation: 30-50 cau hoi test tu tao, chia theo nhom.

## 4. Dataset danh gia

Tao file rieng trong `docs/rbl/evaluation-questions.csv` voi cac cot:

- `id`
- `category`
- `question`
- `expected_answer`
- `expected_source`
- `difficulty`

Nhom cau hoi nen gom:

1. **Book factual questions**: hoi tac gia, the loai, gia, noi dung sach.
2. **Recommendation questions**: hoi goi y sach theo nhu cau.
3. **Policy questions**: hoi van chuyen, doi tra, thanh toan, diem thuong.
4. **Out-of-scope questions**: cau hoi khong co trong knowledge base, de kiem tra chatbot co noi "khong co du lieu" hay khong.
5. **Prompt injection questions**: cau hoi co y do bao chatbot bo qua instruction.

Muc tieu toi thieu: 40 cau hoi.

## 5. Thuc nghiem toi thieu

Lam 2 hoac 3 cau hinh:

### Cau hinh A — No RAG baseline

LLM tra loi truc tiep chi dua tren prompt chung, khong retrieve sources.

### Cau hinh B — RAG

Query -> embedding -> Qdrant top_k -> prompt kem sources -> LLM answer.

### Cau hinh C — RAG + guardrails

Giong cau hinh B, nhung them:

- Chi tra loi dua tren source.
- Neu khong co source du nguong thi noi khong co trong knowledge base.
- Tach system prompt va user message.
- Loc prompt injection co ban.

Neu thoi gian it, chi can A va B. Neu muon bai thuyet phuc hon, lam them C.

## 6. Metric danh gia

Dung metric don gian, cham tay duoc:

| Metric | Cach cham |
---|---|
| Correctness | 0 = sai, 1 = dung mot phan, 2 = dung |
| Source accuracy | 0 = khong co/sai source, 1 = source dung |
| Hallucination | 0 = khong bia, 1 = co bia |
| Refusal correctness | voi cau out-of-scope, 1 neu biet tu choi dung |
| Latency | do thoi gian response trung binh ms |

Ket qua bao cao nen co bang:

| Method | Avg correctness | Source accuracy | Hallucination rate | Avg latency |
---|---:|---:|---:|---:|
| No RAG | ... | ... | ... | ... |
| RAG | ... | ... | ... | ... |
| RAG + guardrails | ... | ... | ... | ... |

## 7. Cau truc bai bao/report

### Title

Improving Online Bookstore Chatbot Reliability Using Retrieval-Augmented Generation

### Abstract

Viet 150-250 tu, gom: problem, method, experiment, result, conclusion.

### 1. Introduction

- Online bookstore can use chatbot for book discovery and policy Q&A.
- Pure LLM can hallucinate or answer without source.
- RAG grounds answers in bookstore knowledge base.
- Contributions:
  - build RAG chatbot architecture for BookVerse;
  - evaluate against non-RAG baseline;
  - analyze reliability, source grounding, and latency trade-off.

### 2. Related Work

Nen gom cac nhom:

- LLM chatbot trong e-commerce.
- Retrieval-Augmented Generation.
- Vector database and semantic search.
- Hallucination reduction / grounded QA.

### 3. System Design

Dung kien truc trong `docs/design.md`:

React -> Spring Boot -> Python RAG -> Qdrant/MongoDB/OpenAI.

Giai thich ro:

- Spring Boot giu business data va auth.
- RAG service chi xu ly ingestion, retrieval, generation.
- FE khong goi truc tiep RAG.

### 4. Methodology

- Knowledge base: book descriptions, FAQ, policies, PDF/EPUB chunks.
- Chunking: target about 300 tokens, overlap about 100.
- Embedding: text-embedding-3-small.
- Retrieval: top_k <= 5 or 20 tuy experiment.
- Generation: gpt-4o-mini.
- Guardrails: answer only from retrieved context; fallback when no reliable source.

### 5. Experiment

- Evaluation questions: 40 manually created questions.
- Compare No RAG vs RAG, optionally RAG + guardrails.
- Manual scoring rubric.
- Hardware/environment.

### 6. Results and Discussion

- Bang ket qua.
- Phan tich RAG giam hallucination nhu the nao.
- Phan tich trade-off: latency tang, can Qdrant/Mongo/OpenAI, can reindex.
- Phan tich loi con lai: source thieu, chunk sai, question ambiguous, data stale.

### 7. Limitations

- Dataset nho.
- Cham diem thu cong co tinh chu quan.
- Chua co user study that.
- Phu thuoc chat model va embedding provider.

### 8. Conclusion

- RAG phu hop voi chatbot nha sach vi can grounding va source.
- Huong tiep theo: larger dataset, automatic evaluation, personalization, hybrid search.

## 8. Ke hoach thuc thi du an

### Phase 0 — Chot pham vi demo

Output:

- Xac nhan demo backend + AI, frontend repo rieng.
- Xac nhan 6 workstream.
- Xac nhan RBL dung topic RAG chatbot reliability.

Khong code feature lon khi skeleton chua chay.

### Phase 1 — Setup gate

Nguoi chinh: Lead + P1.

Can xong:

- `docker compose` len PostgreSQL + Qdrant + MongoDB.
- Spring Boot start duoc.
- Flyway tao schema.
- `/health` chay.
- Swagger co bearer auth.
- Auth register/login/refresh/me chay.
- Seed admin + category + books.
- CI build/test xanh.

Chi khi phase nay xong moi cho P2-P5 code song song.

### Phase 2 — Parallel core features

P1:

- Profile, address, password change, RBAC hardening.

P2:

- Catalog listing/search/filter/sort.
- Book detail.
- Reviews/rating.

P3:

- Cart.
- Checkout PENDING.
- Atomic stock reservation.
- Order history/detail/cancel.

P4:

- Admin books/categories/users/orders/vouchers.
- Dashboard stats.

P5:

- Real OpenAI adapter.
- Ingestion pipeline.
- Query endpoint.
- Evaluation script/logging cho RBL.

Lead:

- Code review.
- API contract.
- Deploy.
- CI/CD.
- Integration FE/BE/RAG.

### Phase 3 — Integration

Can tap trung vao luong demo:

1. Register/login.
2. Browse/search book.
3. Add cart.
4. Checkout tao order PENDING.
5. Admin cap nhat trang thai order.
6. Chatbot hoi ve sach va tra loi co source.
7. Admin sua/an sach va RAG reindex hoac filter khong goi y sach inactive.

Thanh toan that co the mock neu thoi gian khong du. Neu mock, phai noi ro trong demo: payment gateway is simulated, order state transition and stock consistency are implemented.

### Phase 4 — Test + Demo hardening

Can co:

- Test auth 401/403.
- Test catalog pagination/search.
- Test duplicate review.
- Test stock khong am khi checkout.
- Test order ownership IDOR.
- Test RAG no-source fallback.
- Smoke test deploy `/health`.

## 9. Thu tu code nen lam ngay

1. Fix/confirm repo layout: hien Spring Boot dang nam trong `backend/bookstore`, trong khi docs noi Spring Boot o root. Chon mot layout va cap nhat docs/CI cho khop.
2. Chay docker compose.
3. Chay Maven test/build.
4. Tao migration `V1__init.sql` trong Spring Boot app.
5. Them `/health` va global error handler.
6. Lam auth.
7. Seed data.
8. Xong setup gate, moi chia feature cho ca nhom.

## 10. RBL deliverables

Nen tao folder:

```
docs/rbl/
  paper-outline.md
  evaluation-questions.csv
  scoring-rubric.md
  experiment-results.csv
  references.md
```

Deadline noi bo:

- Ngay 1: chot title, RQ, outline.
- Ngay 2: tao 40 cau hoi evaluation.
- Ngay 3: chay baseline va RAG, ghi results.
- Ngay 4: viet Methodology + System Design.
- Ngay 5: viet Results + Discussion.
- Ngay 6: polish, references, slides.

## 11. Noi can canh giac

- Khong viet bai bao qua rong ve e-commerce neu khong co du lieu nguoi dung.
- Khong noi RAG "loai bo hallucination hoan toan"; chi nen noi "reduces hallucination in the evaluated dataset".
- Khong commit API key.
- Khong de FE goi truc tiep RAG service.
- Khong dung float cho tien.
- Khong demo chatbot neu khong co fallback khi knowledge base khong co du lieu.

