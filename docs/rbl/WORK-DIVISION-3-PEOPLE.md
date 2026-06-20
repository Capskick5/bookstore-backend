# Phân Công Viết Báo — 3 Người

> **Timeline**: 7–10 ngày  
> **Output**: Bài báo 20–25 trang format IEEE/ACM  
> **Đề tài**: Improving Online Bookstore Chatbot Reliability Using RAG

---

## Vai Trò & Nhiệm Vụ

### Person 1 — Leader / System Architect (Bạn)

**Strengths**: Hiểu toàn bộ dự án, kiến trúc hệ thống, tích hợp frontend-backend-AI

**Trách nhiệm chính**:
- Tổng hợp + review toàn bộ bài báo
- Viết các phần architecture + business context
- Đảm bảo consistency giữa các sections
- Polish language + format cuối cùng

**Sections phụ trách**:

| Section | Công việc | Deadline | Ước tính giờ |
|---------|-----------|----------|--------------|
| **1. Introduction** | Viết problem statement, motivation, contributions | Ngày 3 | 4h |
| **3. System Design** | Vẽ architecture diagram, mô tả Spring Boot ↔ RAG ↔ Qdrant/Mongo/OpenAI | Ngày 4 | 6h |
| **3.4 Integration** | Viết reindexing strategy, rate limiting, timeout handling | Ngày 4 | 2h |
| **6. Discussion** | Interpretation, practical implications, comparison | Ngày 7 | 3h |
| **9. Conclusion** | Tóm tắt toàn bài, key takeaways | Ngày 7 | 1h |
| **Final Polish** | Review toàn bài, sửa grammar, format references, layout | Ngày 8–9 | 6h |
| **Total** | | | **22h** |

---

### Person 2 — AI Expert / RAG Specialist (Ông AI giỏi)

**Strengths**: Hiểu sâu RAG, LLM, embedding, retrieval, prompt engineering

**Trách nhiệm chính**:
- Technical depth cho RAG service
- Design + run experiments
- Phân tích kết quả + error analysis
- Tìm và tóm tắt related work papers

**Sections phụ trách**:

| Section | Công việc | Deadline | Ước tính giờ |
|---------|-----------|----------|--------------|
| **Abstract** | Viết 200-word summary of paper | Ngày 8 (sau khi có Results) | 1h |
| **2. Related Work** | Tìm + đọc + tóm tắt 20+ papers về RAG, LLM chatbot, vector DB, hallucination | Ngày 2–3 | 8h |
| **3.2 RAG Service** | Viết chi tiết ingestion pipeline, retrieval engine, generation module | Ngày 4 | 4h |
| **4. Methodology** | Viết RQ, configurations (No RAG, RAG, RAG+Guardrails), metrics | Ngày 5 | 3h |
| **5. Results** | Chạy 120 queries (40 câu x 3 configs), ghi kết quả, vẽ charts, error analysis | Ngày 6–7 | 10h |
| **7. Limitations** | Viết dataset size, manual scoring, LLM dependency, cost | Ngày 7 | 1h |
| **8. Future Work** | Đề xuất larger dataset, hybrid retrieval, personalization, cost optimization | Ngày 7 | 1h |
| **References** | Format 20+ citations theo IEEE/ACM style | Ngày 8 | 2h |
| **Total** | | | **30h** |

---

### Person 3 — Data / QA Specialist (Người thứ 3)

**Strengths**: Tỉ mỉ, tạo dataset, chấm điểm, documentation

**Trách nhiệm chính**:
- Tạo evaluation dataset (40 câu hỏi)
- Chấm điểm 120 responses (manual scoring)
- Tạo appendix (prompts, screenshots, examples)
- Support Person 1 và 2 khi cần

**Sections phụ trách**:

| Section | Công việc | Deadline | Ước tính giờ |
|---------|-----------|----------|--------------|
| **4.2 Evaluation Dataset** | Tạo 40 câu hỏi (Book Facts, Recommendations, Policy, Out-of-scope, Adversarial) + expected answers | Ngày 2 | 6h |
| **4.4 Scoring Rubric** | Viết rubric cho Correctness, Source Accuracy, Hallucination, Refusal | Ngày 2 | 2h |
| **5. Results — Scoring** | Chấm điểm 120 responses theo rubric (cùng Person 2 để tính inter-rater agreement) | Ngày 6 | 8h |
| **Appendix A** | Paste 10 example questions from dataset | Ngày 7 | 0.5h |
| **Appendix B** | Show exact prompt templates for 3 configs | Ngày 7 | 1h |
| **Appendix C** | Take screenshots of chatbot UI, admin reindex, conversation with sources | Ngày 5 | 1h |
| **Support** | Proofread Introduction (P1), check References format (P2) | Ngày 8 | 2h |
| **Total** | | | **20.5h** |

---

## Timeline Chi Tiết (10 Ngày)

### Ngày 1 (Hôm nay) — Kickoff Meeting

**All**:
- Họp với AI Expert (Person 2) theo `QUESTIONS-FOR-AI-EXPERT.md`
- Chốt: architecture, experiment design, work division
- Output: Meeting notes + confirmed timeline

---

### Ngày 2 — Dataset + Related Work

**Person 3**:
- Tạo `evaluation-questions.csv`: 40 câu hỏi + expected answers
- Viết `scoring-rubric.md`
- **Deliverable**: `evaluation-questions.csv`, `scoring-rubric.md`

**Person 2**:
- Tìm 20+ papers về RAG, LLM chatbot, hallucination, vector DB
- Tạo `references.md` với bullet points tóm tắt mỗi paper
- **Deliverable**: `references.md` (annotated bibliography)

---

### Ngày 3 — Introduction + Related Work Draft

**Person 1**:
- Viết **§1 Introduction** (problem, motivation, contributions)
- **Deliverable**: `01-introduction.md`

**Person 2**:
- Viết **§2 Related Work** (5 subsections: LLM chatbot, RAG, vector DB, hallucination, positioning)
- **Deliverable**: `02-related-work.md`

---

### Ngày 4 — System Design + Methodology

**Person 1**:
- Viết **§3 System Design** (architecture diagram, Spring Boot, RAG service, reindexing)
- **Deliverable**: `03-system-design.md` + architecture diagram (PNG/SVG)

**Person 2**:
- Viết **§4 Methodology** (RQ, dataset description, configurations, metrics, environment)
- **Deliverable**: `04-methodology.md`

**Person 3**:
- Take screenshots của chatbot UI, admin panel, reindex button
- **Deliverable**: `appendix-c-screenshots/` folder

---

### Ngày 5 — Setup Experiment

**Person 2**:
- Viết script chạy experiment: `run_experiment.py`
- Test với 5 câu pilot để verify setup
- **Deliverable**: `run_experiment.py`, pilot results

**Person 3**:
- Review 40 câu hỏi với Person 2, adjust nếu cần
- Chuẩn bị scoring spreadsheet

---

### Ngày 6 — Run Experiment

**Person 2**:
- Chạy 120 queries (40 câu x 3 configs)
- Log latency, token usage
- Export results ra `experiment-results.csv`
- **Deliverable**: `experiment-results.csv`, `latency-log.csv`

**Person 3**:
- Chấm điểm 120 responses theo rubric (Correctness, Source Accuracy, Hallucination, Refusal)
- **Deliverable**: `manual-scores.csv`

---

### Ngày 7 — Results + Discussion + Conclusion

**Person 2**:
- Tính inter-rater agreement (Cohen's kappa) với Person 3
- Viết **§5 Results** (tables, charts, error analysis)
- Viết **§7 Limitations** và **§8 Future Work**
- **Deliverable**: `05-results.md`, `07-limitations.md`, `08-future-work.md`

**Person 1**:
- Viết **§6 Discussion** (interpretation, implications, comparison)
- Viết **§9 Conclusion**
- **Deliverable**: `06-discussion.md`, `09-conclusion.md`

**Person 3**:
- Tạo **Appendix A** (example questions) và **Appendix B** (prompt templates)
- **Deliverable**: `appendix-a-questions.md`, `appendix-b-prompts.md`

---

### Ngày 8 — Abstract + Polish

**Person 2**:
- Viết **Abstract** (dựa trên Results đã có)
- Format **References** theo IEEE/ACM style
- **Deliverable**: `00-abstract.md`, `references.bib`

**Person 1**:
- Review toàn bộ sections (1–9)
- Check consistency: terminology, notation, citations
- Sửa grammar, flow
- **Deliverable**: Combined draft v1

**Person 3**:
- Proofread Introduction + System Design
- Check figures, tables, appendix format

---

### Ngày 9 — Final Review + Format

**All**:
- Merge all sections vào main document
- Format theo template IEEE/ACM (double-column, references style)
- Final proofread
- **Deliverable**: `BookVerse-RAG-Paper-Final.pdf`

---

### Ngày 10 — Buffer / Slides Prep (nếu cần thuyết trình)

**All**:
- Address last-minute issues
- Chuẩn bị slides nếu cần present (15–20 slides)

---

## Công Cụ Cộng Tác

### 1. Google Docs hoặc Overleaf

**Lý do**: Real-time collaboration, comment/suggest mode, version history

**Setup**:
- Tạo 1 Google Doc hoặc Overleaf project
- Share cho cả 3 người với edit access
- Mỗi người viết section của mình trong doc riêng, sau đó merge

---

### 2. Shared Folder (Google Drive / Dropbox)

**Cấu trúc**:
```
BookVerse-RBL-Paper/
├── 00-outline-and-plan/
│   ├── PAPER-OUTLINE-BOOKVERSE.md
│   ├── QUESTIONS-FOR-AI-EXPERT.md
│   └── WORK-DIVISION-3-PEOPLE.md
├── 01-dataset/
│   ├── evaluation-questions.csv
│   ├── scoring-rubric.md
│   └── expected-answers.csv
├── 02-experiment/
│   ├── run_experiment.py
│   ├── experiment-results.csv
│   ├── latency-log.csv
│   └── manual-scores.csv
├── 03-drafts/
│   ├── 00-abstract.md
│   ├── 01-introduction.md
│   ├── 02-related-work.md
│   ├── 03-system-design.md
│   ├── 04-methodology.md
│   ├── 05-results.md
│   ├── 06-discussion.md
│   ├── 07-limitations.md
│   ├── 08-future-work.md
│   └── 09-conclusion.md
├── 04-references/
│   ├── references.md (annotated)
│   └── references.bib
├── 05-figures/
│   ├── architecture-diagram.png
│   ├── results-chart-1.png
│   └── ...
├── 06-appendix/
│   ├── appendix-a-questions.md
│   ├── appendix-b-prompts.md
│   └── appendix-c-screenshots/
└── 07-final/
    ├── BookVerse-RAG-Paper-Final.pdf
    └── BookVerse-RAG-Slides.pdf (nếu có)
```

---

### 3. Communication (Telegram / Discord / Slack)

**Daily standup** (mỗi tối 9pm):
- Person 1: "Hôm nay làm xong gì, ngày mai làm gì, có block gì không?"
- Person 2: ...
- Person 3: ...

**Ping khi cần review**:
- "Person 2, em đã xong Introduction draft, anh review giúp em nhé"

---

## Checklist Trước Khi Submit

- [ ] **Completeness**: Đủ 9 sections + Abstract + References + Appendix
- [ ] **Consistency**: Terminology thống nhất (RAG, LLM, Qdrant, không viết sai thành QRANT)
- [ ] **Figures**: Tất cả figures có caption + reference trong text
- [ ] **Tables**: Tất cả tables có caption + đủ data
- [ ] **Citations**: Mọi claim đều có citation, không có [?] hay [TODO]
- [ ] **Grammar**: Proofread bằng Grammarly hoặc LanguageTool
- [ ] **Format**: Đúng IEEE/ACM template (font, margin, column, reference style)
- [ ] **Reproducibility**: Appendix B có đủ prompt templates, scripts có trong repo
- [ ] **Page count**: 20–25 trang (không quá ngắn, không quá dài)

---

## Rủi Ro & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|------------|------------|
| Person 2 bận đột xuất, không chạy được experiment | High | Medium | Person 1 học cách chạy script từ ngày 5, backup |
| OpenAI API rate limit | Medium | Low | Spread queries over time, hoặc dùng multiple keys |
| Dataset 40 câu quá ít, reviewer complain | Medium | Medium | Acknowledge trong Limitations, đề xuất larger dataset ở Future Work |
| Results không improve nhiều (RAG chỉ tốt hơn 5%) | High | Low | Analyze why, viết Discussion section giải thích trade-offs |
| Không kịp deadline 10 ngày | Medium | Medium | Cut Appendix C (screenshots), giảm Related Work từ 2.5 trang xuống 2 trang |

---

## Summary — Who Does What

| Person | Main Focus | Workload | Critical Sections |
|--------|------------|----------|-------------------|
| **Person 1 (Leader)** | Architecture + Business Context | 22h | Introduction, System Design, Discussion, Conclusion |
| **Person 2 (AI Expert)** | Technical Depth + Experiments | 30h | Related Work, Methodology, Results, Limitations |
| **Person 3 (QA)** | Dataset + Scoring + Appendix | 20.5h | Evaluation Questions, Manual Scoring, Appendix |

**Total**: 72.5 person-hours ≈ 9 person-days → Feasible trong 10 ngày với 3 người.

