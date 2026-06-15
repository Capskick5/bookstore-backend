# Câu hỏi cho buổi họp với AI Expert (tối nay)

> **Mục tiêu buổi họp**: Chốt phạm vi RBL, phân công việc, và xác nhận technical details cho bài báo

---

## A. Về Phạm Vi Nghiên Cứu (Research Scope)

### A1. RAG Architecture

**Q**: Anh confirm giúp em kiến trúc RAG service hiện tại:
- Đang dùng embedding model nào? (text-embedding-3-small dim 1536?)
- LLM nào cho generation? (gpt-4o-mini?)
- Qdrant collection config như thế nào? (cosine similarity?)
- MongoDB dùng để lưu gì? (full chunk text + images?)

**Tại sao hỏi**: Phần System Design (§3) cần ghi chính xác technical stack.

---

### A2. Ingestion Pipeline

**Q**: Pipeline ingest sách vào Qdrant + MongoDB hoạt động như thế nào?
- Chunking strategy: target bao nhiêu tokens? overlap bao nhiêu?
- Có xử lý PDF/EPUB thế nào? (PyPDF2, pypdf, pdfplumber, ebooklib?)
- Một cuốn sách tạo ra bao nhiêu chunks trung bình?
- Reindex khi admin sửa sách: có tự động không? hay trigger manual?

**Tại sao hỏi**: §3.2.1 Ingestion Pipeline cần flow chi tiết + §5 Results cần số liệu thực tế.

---

### A3. Retrieval Mechanism

**Q**: Khi user hỏi chatbot, retrieval hoạt động như thế nào?
- `top_k` mặc định là bao nhiêu? (5? 10? 20?)
- Có similarity threshold không? (ví dụ: chỉ lấy chunk có score > 0.7?)
- Nếu không có chunk nào đạt threshold thì trả lời gì? ("Tôi không có thông tin này"?)
- Có filter theo metadata không? (ví dụ: chỉ search trong sách active?)

**Tại sao hỏi**: §4.3 Configurations cần mô tả chính xác cơ chế retrieval.

---

### A4. Prompt Engineering

**Q**: Prompt hiện tại trông như thế nào?
- System prompt có instruction nào? (ví dụ: "answer ONLY from sources"?)
- Retrieved chunks được format vào prompt ra sao?
- Có xử lý multi-turn conversation không? (lưu lịch sử chat?)
- Có mechanism nào chống prompt injection không?

**Tại sao hỏi**: §4.3 Configurations + Appendix B cần show exact prompts.

---

## B. Về Thực Nghiệm (Experiment Design)

### B1. Evaluation Dataset

**Q**: Em dự định tạo 40 câu hỏi evaluation, anh thấy phân loại này OK không?
- Book Facts (10 câu): hỏi tác giả, giá, thể loại, nội dung sách
- Recommendations (10 câu): "gợi ý sách về X", "sách giống Y"
- Policy (8 câu): hỏi chính sách vận chuyển, đổi trả, thanh toán, điểm thưởng
- Out-of-scope (8 câu): hỏi ngoài phạm vi (thời tiết, tin tức, toán học)
- Adversarial (4 câu): prompt injection, jailbreak

**Anh có gợi ý câu hỏi cụ thể nào không?** (ví dụ câu hỏi khó, edge case)

**Tại sao hỏi**: §4.2 Evaluation Dataset — cần confirm categories trước khi tạo dataset.

---

### B2. Baseline Configurations

**Q**: Em định so sánh 3 config:
1. **No RAG**: chỉ LLM + generic prompt (không retrieve)
2. **RAG**: retrieve top_k → inject vào prompt
3. **RAG + Guardrails**: thêm instruction "chỉ trả lời từ sources" + fallback khi không có source

**Anh có đề xuất config nào khác không?** (ví dụ: RAG với top_k khác nhau, hoặc embedding model khác?)

**Tại sao hỏi**: §4.3 Configurations — cần chốt số lượng experiments.

---

### B3. Metrics và Scoring

**Q**: Em dự định đo:
- **Correctness** (0–2): 0=sai, 1=đúng một phần, 2=hoàn toàn đúng
- **Source Accuracy** (0–1): có cite đúng source không?
- **Hallucination** (0–1): có bịa thông tin không?
- **Refusal Correctness** (0–1): với câu out-of-scope, có từ chối đúng không?
- **Latency** (ms): thời gian response

**Anh thấy còn metric nào cần thêm không?** (ví dụ: token usage, cost, retrieval recall?)

**Cách chấm điểm**: 2 người chấm độc lập, tính Cohen's kappa. Anh OK không?

**Tại sao hỏi**: §4.4 Evaluation Metrics — metrics phải đo được và có ý nghĩa.

---

### B4. Knowledge Base Size

**Q**: Em nên chuẩn bị knowledge base bao lớn?
- Bao nhiêu cuốn sách? (12? 20? 50?)
- Bao nhiêu FAQ entries? (5? 10?)
- Bao nhiêu policy documents? (shipping, return, payment, loyalty?)

**Anh có book nào gợi ý ingest không?** (sách nào có nội dung rõ, dễ test?)

**Tại sao hỏi**: §4.5 Environment — cần specify knowledge base size.

---

## C. Về Kết Quả Dự Kiến (Expected Results)

### C1. Hypothesis

**Q**: Anh nghĩ kết quả sẽ ra sao?
- RAG có giảm hallucination bao nhiêu % so với No RAG?
- Source accuracy sẽ cải thiện bao nhiêu?
- Latency tăng bao nhiêu là acceptable? (<500ms? <1s?)

**Tại sao hỏi**: §5 Results — cần có baseline expectation để analyze.

---

### C2. Failure Modes

**Q**: Anh dự đoán failure modes nào sẽ xảy ra?
- Chunking sai → retrieve sai chunk?
- Query ambiguous → không biết retrieve gì?
- Source stale → admin sửa sách nhưng chưa reindex?
- Embedding mismatch → query không match được chunk liên quan?

**Tại sao hỏi**: §5.3 Error Analysis — cần categorize failure modes.

---

## D. Về Thực Thi (Execution Plan)

### D1. Timeline

**Q**: Anh nghĩ timeline này realistic không?
- **Ngày 1–2**: tạo 40 câu hỏi evaluation + scoring rubric
- **Ngày 3–4**: chạy experiment (No RAG, RAG, RAG+Guardrails) + ghi kết quả
- **Ngày 5–6**: phân tích lỗi + viết Results section
- **Ngày 7**: polish Abstract + Introduction + Conclusion

**Có giai đoạn nào cần thêm thời gian không?**

**Tại sao hỏi**: Cần chốt deadline nội bộ.

---

### D2. Tools và Scripts

**Q**: Anh có script/tool nào sẵn để:
- Generate embeddings hàng loạt?
- Batch query Qdrant?
- Log latency + token usage?
- Export results ra CSV/JSON?

**Nếu chưa có, em sẽ viết script. Anh confirm format output nhé.**

**Tại sao hỏi**: §5 Results cần automation để chạy 40 câu x 3 configs = 120 queries.

---

### D3. Replication

**Q**: Sau khi experiment xong, anh có thể chạy lại để verify results không?
- Environment giống nhau không? (Python version, dependencies)
- Random seed có cố định không? (OpenAI API có deterministic mode?)

**Tại sao hỏi**: Reproducibility — để tránh reviewer challenge.

---

## E. Về Phân Công (Work Division)

### E1. Ai làm gì?

**Q**: Em đề xuất phân công như sau, anh thấy sao?

**Leader (bạn)**:
- Introduction (problem statement, motivation)
- System Design (architecture overview, integration)
- Discussion (interpretation, implications)
- Conclusion
- Tổng hợp + polish toàn bộ bài

**AI Expert (anh)**:
- Abstract (technical summary)
- Related Work (tìm + tóm tắt papers)
- Methodology (experiment design, metrics)
- Results (chạy experiment + phân tích)
- Limitations + Future Work

**Person 3** (nếu có):
- Evaluation Dataset (tạo 40 câu hỏi + expected answers)
- Scoring (chấm điểm 120 responses)
- Appendix (prompt templates, screenshots)
- References (format citations)

**Anh có đề xuất khác không?**

**Tại sao hỏi**: Phân công rõ ràng → tránh overlap/miss.

---

### E2. Deliverables

**Q**: Sau buổi họp này, anh sẽ deliver những gì trước?
- [ ] Confirm kiến trúc RAG (A1–A4)
- [ ] Review 40 câu hỏi evaluation (B1)
- [ ] Gợi ý papers cho Related Work (B — ít nhất 10 papers)
- [ ] Chạy pilot experiment với 5 câu để test setup (D2)

**Deadline**: [chốt ngày cụ thể]

**Tại sao hỏi**: Cần action items rõ ràng sau meeting.

---

## F. Về Rủi Ro (Risks)

### F1. Technical Risks

**Q**: Anh thấy có rủi ro kỹ thuật nào không?
- OpenAI API rate limit → experiment bị ngắt?
- Qdrant data bị mất → mất knowledge base?
- Reindex lâu → experiment chạy quá lâu?

**Có backup plan không?**

---

### F2. Academic Risks

**Q**: Anh thấy bài này có điểm yếu nào reviewer có thể chỉ ra không?
- Dataset quá nhỏ (40 câu)?
- Chấm điểm manual → subjective?
- Không có user study thật?
- Chỉ test với OpenAI → không generalize?

**Em nên address như thế nào trong Limitations?**

---

## G. Tóm Tắt — Checklist Trước Khi Kết Thúc Họp

- [ ] **Architecture confirmed**: embedding model, LLM, Qdrant config, MongoDB schema
- [ ] **Ingestion pipeline documented**: chunking strategy, PDF/EPUB handling, reindex flow
- [ ] **Retrieval mechanism clarified**: top_k, threshold, fallback behavior
- [ ] **Prompt templates shared**: system prompt, user prompt, guardrails
- [ ] **Evaluation dataset reviewed**: 40 questions OK, có thêm gợi ý nào
- [ ] **Baseline configs finalized**: No RAG, RAG, RAG+Guardrails
- [ ] **Metrics confirmed**: correctness, source accuracy, hallucination, refusal, latency
- [ ] **Timeline agreed**: ngày nào deliver gì
- [ ] **Work division clear**: Leader, AI Expert, Person 3 làm gì
- [ ] **Papers suggested**: ít nhất 10 papers cho Related Work
- [ ] **Risks identified**: technical + academic risks + mitigation
- [ ] **Next steps defined**: action items + deadline

---

# Câu Hỏi Quan Trọng Nhất (Đừng Quên Hỏi!)

**Q**: Anh có tin vào kết quả experiment này không? Hay anh nghĩ sẽ có surprise nào?

**Tại sao hỏi**: Nếu anh thấy có surprise, em cần chuẩn bị sẵn plan B (ví dụ: RAG không improve nhiều → cần explain why).

