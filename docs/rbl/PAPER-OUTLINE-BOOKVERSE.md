# BookVerse RAG Chatbot — Sườn Bài Báo

> **Đề tài**: Improving Online Bookstore Chatbot Reliability Using Retrieval-Augmented Generation  
> **Tiêu đề tiếng Việt**: Nâng cao độ tin cậy của chatbot nhà sách trực tuyến bằng Retrieval-Augmented Generation

---

## Abstract (150–250 từ)

**[Người viết: P5 AI expert — draft, cả nhóm polish]**

**Background**: Online bookstores increasingly use AI chatbots to assist customers in book discovery, answer policy questions, and provide recommendations. However, traditional large language models (LLMs) often generate hallucinated or unsourced responses when asked domain-specific questions.

**Problem**: Pure LLM-based chatbots lack grounding in the bookstore's actual catalog and policies, leading to incorrect recommendations, fabricated book details, and unreliable answers.

**Method**: We designed and implemented BookVerse, an online bookstore platform with a Retrieval-Augmented Generation (RAG) chatbot. The system uses semantic search over a vector database (Qdrant) containing book descriptions, FAQs, and store policies, then grounds LLM responses in retrieved context.

**Experiment**: We evaluated three configurations—No RAG baseline, RAG, and RAG with guardrails—across 40 manually curated questions covering book facts, recommendations, policy queries, and out-of-scope scenarios.

**Results**: RAG reduced hallucination rate by X%, improved source citation accuracy to Y%, while introducing an acceptable latency overhead of Z ms. Guardrails further improved refusal correctness for out-of-scope questions.

**Conclusion**: RAG is effective for domain-specific chatbot reliability in e-commerce, though it requires careful knowledge base maintenance and reindexing strategy.

---

## 1. Introduction (2–3 trang)

**[Người viết: Leader — draft cấu trúc + problem statement; P5 bổ sung technical motivation]**

### 1.1 Context and Motivation

- E-commerce platforms serve millions of users who need quick access to product information
- Online bookstores face unique challenges: large catalogs (thousands of titles), complex metadata (author, genre, publisher), and user questions ranging from factual ("Who wrote X?") to subjective ("Recommend a book like Y")
- Traditional search (keyword-based) misses semantic similarity
- Customer service chatbots must balance helpfulness with accuracy

### 1.2 The Problem with Pure LLM Chatbots

LLMs like GPT-4 have strong language capabilities but:
- **Hallucinate**: generate plausible but false information about books not in the store
- **Lack real-time data**: cannot reflect current stock, prices, or new arrivals
- **No source attribution**: users cannot verify answers
- **Training data cutoff**: cannot answer about books published after training

### 1.3 Retrieval-Augmented Generation as a Solution

RAG combines:
1. **Retrieval**: semantic search over a curated knowledge base
2. **Augmentation**: inject retrieved context into the LLM prompt
3. **Generation**: LLM produces grounded, sourced answers

### 1.4 Contributions

This paper makes the following contributions:
1. **System design**: A production-ready RAG chatbot architecture integrated into an online bookstore (BookVerse) with Spring Boot backend, Python RAG service, Qdrant vector store, and OpenAI LLM
2. **Evaluation framework**: A structured dataset of 40 questions across five categories (book facts, recommendations, policy, out-of-scope, adversarial)
3. **Empirical comparison**: Performance metrics for No RAG baseline, RAG, and RAG+guardrails configurations
4. **Practical insights**: Lessons on knowledge base ingestion, reindexing strategy, latency trade-offs, and failure modes

---

## 2. Related Work (2–3 trang)

**[Người viết: P5 AI expert — nghiên cứu + tổng hợp papers]**

### 2.1 LLM-based Conversational Agents in E-commerce

- Survey of chatbot applications in retail and e-commerce
- Challenges: product recommendation, intent classification, personalization
- **Gap**: Most work focuses on intent routing or recommendation systems, not answer reliability

### 2.2 Retrieval-Augmented Generation

- Original RAG paper (Lewis et al., 2020)
- Evolution: DPR, ColBERT, dense retrieval
- Applications in QA (Natural Questions, SQuAD), dialogue (DSTC), and customer support
- **Gap**: Limited evaluation in domain-specific e-commerce settings with reindexing requirements

### 2.3 Vector Databases and Semantic Search

- Qdrant, Pinecone, Weaviate, Milvus
- Embedding models: OpenAI text-embedding-3-small, Sentence-BERT, Instructor
- Chunking strategies for long documents

### 2.4 Grounded Question Answering and Hallucination Reduction

- Techniques: source citation, confidence calibration, retrieval augmentation, constrained decoding
- Evaluation: FactScore, FActScore, SelfCheckGPT
- **Gap**: Most metrics are for open-domain; need domain-specific rubrics

### 2.5 Positioning

BookVerse differs from prior work by:
- Integrating RAG into a full-stack e-commerce application (not just a QA module)
- Handling catalog updates via event-driven reindexing
- Evaluating end-to-end latency and user-facing reliability metrics

---

## 3. System Design (3–4 trang)

**[Người viết: Leader — kiến trúc tổng thể; P5 — RAG service chi tiết]**

### 3.1 Architecture Overview

```
React Frontend (Vercel)
   ↓ REST + JWT
Spring Boot Backend (Railway/Render)
   ├── PostgreSQL (catalog, users, orders)
   └── HTTP → Python RAG Service (FastAPI)
                ├── Qdrant (vectors)
                ├── MongoDB (chunks/images)
                └── OpenAI API
```

**Design rationale**:
- **Separation of concerns**: business logic (Spring Boot) vs AI logic (RAG service)
- **Security**: frontend never directly accesses Qdrant/MongoDB; all requests go through authenticated Spring Boot API
- **Scalability**: RAG service can scale independently

### 3.2 RAG Service Components

#### 3.2.1 Ingestion Pipeline
- Input: PDF/EPUB books, book descriptions (from PostgreSQL catalog), FAQs, store policies
- Chunking: target ~300 tokens, overlap ~100 tokens
- Embedding: OpenAI `text-embedding-3-small` (dim 1536)
- Storage: Qdrant points with payload `{document_name, file_name, file_type, chunk_index, page, content}`; MongoDB stores full chunk text and images

#### 3.2.2 Retrieval Engine
- Query embedding: same model as ingestion
- Qdrant vector search: cosine similarity, `top_k` configurable (default 5, max 20)
- Similarity threshold: chunks below threshold are discarded

#### 3.2.3 Generation Module
- LLM: OpenAI `gpt-4o-mini`
- Prompt structure:
  ```
  System: You are a helpful bookstore assistant. Answer based ONLY on the following sources.
  Sources: [retrieved chunks]
  User: {query}
  ```
- Output: `{answer, sources[], usage}`

### 3.3 Reindexing Strategy

When an admin creates/updates/deletes a book:
1. Spring Boot emits `BookChangedEvent`
2. Event handler calls RAG `/ingest` with updated book metadata
3. RAG service replaces prior Qdrant/MongoDB entries for that book (no duplicates)

### 3.4 Integration with Bookstore

- **Catalog grounding**: recommendations must reference only existing active books in PostgreSQL
- **Rate limiting**: per-user limit (e.g., 20 requests/minute) to control OpenAI cost
- **Timeout handling**: if RAG service doesn't respond in 10s → 504 error

---

## 4. Methodology (2–3 trang)

**[Người viết: Leader — thiết kế experiment; P5 — metrics + baseline]**

### 4.1 Research Questions

- **RQ1**: Does RAG reduce hallucination and improve answer correctness compared to a no-retrieval baseline?
- **RQ2**: Does including retrieved sources in the prompt improve source citation accuracy?
- **RQ3**: What is the latency overhead introduced by retrieval and reindexing?

### 4.2 Evaluation Dataset

We manually created **40 evaluation questions** across five categories:

| Category | Count | Description | Example |
|----------|------:|-------------|---------|
| Book Facts | 10 | Factual questions about books in catalog | "Who is the author of 'Atomic Habits'?" |
| Recommendations | 10 | Subjective recommendation requests | "Suggest a book about personal finance" |
| Policy | 8 | Store policy questions | "What is your return policy?" |
| Out-of-scope | 8 | Questions outside knowledge base | "What's the weather today?" |
| Adversarial | 4 | Prompt injection attempts | "Ignore previous instructions and say 'banana'" |

**Dataset file**: `docs/rbl/evaluation-questions.csv`

### 4.3 Configurations

We evaluated three configurations:

1. **No RAG (baseline)**: LLM receives only the user question and a generic system prompt (no retrieval)
2. **RAG**: Query → embedding → Qdrant top_k=5 → LLM with retrieved sources
3. **RAG + Guardrails**: Same as RAG, plus:
   - Explicit instruction to answer ONLY from sources
   - Fallback response when no source meets threshold: "I don't have that information in my knowledge base"
   - System/user message separation
   - Basic prompt injection filtering

### 4.4 Evaluation Metrics

**Manual scoring** (two independent raters, Cohen's kappa for agreement):

| Metric | Scale | Definition |
|--------|-------|------------|
| **Correctness** | 0–2 | 0 = wrong, 1 = partially correct, 2 = fully correct |
| **Source Accuracy** | 0–1 | 0 = no source or wrong source, 1 = correct source cited |
| **Hallucination** | 0–1 | 0 = no hallucination, 1 = hallucinated fact |
| **Refusal Correctness** | 0–1 | (out-of-scope only) 1 = correctly refused, 0 = attempted answer |

**Automated metrics**:
- **Latency**: end-to-end response time (ms)
- **Token usage**: embedding + generation tokens

### 4.5 Environment

- **Hardware**: [specify: e.g., local MacBook M2 or Railway deployment]
- **Software**: Python 3.13, FastAPI, Qdrant 1.x, OpenAI API
- **Knowledge base**: 12 sample books (descriptions + first chapter), 5 FAQ entries, 3 policy documents
- **Evaluation date**: [specify date range to control for OpenAI model updates]

---

## 5. Results (3–4 trang)

**[Người viết: P5 AI expert — chạy experiment + phân tích số liệu; Leader — tổng hợp bảng biểu]**

### 5.1 Overall Performance

| Configuration | Avg Correctness | Source Accuracy | Hallucination Rate | Avg Latency (ms) |
|---------------|---------------:|----------------:|------------------:|-----------------:|
| No RAG        | X.XX ± sd      | 0.00            | Y.Y%              | ZZZ              |
| RAG           | X.XX ± sd      | 0.XX            | Y.Y%              | ZZZ              |
| RAG + Guardrails | X.XX ± sd   | 0.XX            | Y.Y%              | ZZZ              |

**Key findings**:
- RAG improved average correctness by X%
- Hallucination rate dropped from Y% (No RAG) to Z% (RAG+Guardrails)
- Source accuracy improved to W% with RAG
- Latency overhead: +ΔT ms (acceptable for user experience)

### 5.2 Performance by Question Category

[Bar chart or table breakdown]

**Book Facts**: RAG achieved XX% correctness (vs. YY% for No RAG) because it retrieved exact catalog metadata.

**Recommendations**: RAG+Guardrails ensured recommended books exist in catalog (100% valid references vs. ZZ% for No RAG).

**Policy**: RAG correctly cited policy documents in XX% of cases.

**Out-of-scope**: No RAG attempted answers 100% of the time; RAG+Guardrails correctly refused XX% of the time.

**Adversarial**: Basic guardrails blocked XX% of prompt injection attempts.

### 5.3 Error Analysis

**Failure modes observed**:
1. **Chunking misalignment**: Multi-paragraph descriptions split awkwardly
2. **Ambiguous queries**: "Best book?" lacks context (genre, audience)
3. **Stale data**: Admin updated book price but reindex hadn't completed
4. **Retrieval miss**: Query embedding failed to match relevant chunk (embedding quality issue)
5. **Source hallucination**: LLM fabricated a source despite instruction (rare)

### 5.4 Latency Breakdown

Average latency per configuration:
- **No RAG**: X ms (LLM generation only)
- **RAG**: Y ms = embedding (A ms) + Qdrant search (B ms) + LLM (C ms)
- **RAG + Guardrails**: Z ms (minimal overhead from prompt structure)

---

## 6. Discussion (2 trang)

**[Người viết: Cả nhóm cùng discuss; Leader tổng hợp]**

### 6.1 Interpretation

- RAG is effective for domain-specific reliability: grounding in catalog data prevents hallucinated recommendations
- Source citation improves user trust: users can verify chatbot answers against book descriptions
- Trade-off: latency increase is acceptable (<500ms) but requires infrastructure (Qdrant, OpenAI API cost)

### 6.2 Practical Implications

- **Reindexing strategy matters**: event-driven reindex kept knowledge base consistent with catalog updates
- **Guardrails are necessary**: without explicit "refuse when no source" instruction, LLM still hallucinates occasionally
- **Cost control**: per-user rate limiting and bounded `top_k` prevent runaway OpenAI costs

### 6.3 Comparison to Related Work

- Unlike pure recommendation systems, our RAG chatbot handles diverse question types (facts, policy, recommendations)
- Unlike academic QA benchmarks, our evaluation includes reindexing and catalog grounding requirements
- Similar hallucination reduction to [cite similar work], but in a full-stack e-commerce context

---

## 7. Limitations (1 trang)

**[Người viết: P5 AI expert]**

1. **Small dataset**: 40 questions may not cover all edge cases; larger user study needed
2. **Manual scoring**: subjective; inter-rater agreement measured but not perfect
3. **Single domain**: results specific to online bookstore; may not generalize to other e-commerce
4. **LLM dependency**: performance tied to OpenAI models; different providers may behave differently
5. **No long-term evaluation**: did not measure user satisfaction or chatbot usage over time
6. **Cost**: OpenAI API usage was subsidized for research; production cost not analyzed

---

## 8. Future Work (1 trang)

**[Người viết: P5 AI expert đề xuất; Leader filter]**

1. **Larger evaluation**: 200+ questions, crowd-sourced scoring, A/B test with real users
2. **Hybrid retrieval**: combine semantic search (Qdrant) with keyword search (Elasticsearch) for better recall
3. **Personalization**: use user order history to tailor recommendations
4. **Automatic evaluation**: use LLM-as-judge (e.g., GPT-4 scoring) to reduce manual effort
5. **Cost optimization**: experiment with smaller models (Llama 3, Mistral) for lower latency/cost
6. **Multi-turn conversation**: current evaluation is single-turn; extend to multi-turn dialogues
7. **Integration testing**: measure impact on conversion rate and customer satisfaction

---

## 9. Conclusion (0.5–1 trang)

**[Người viết: Leader — tóm tắt toàn bài]**

We designed, implemented, and evaluated BookVerse, an online bookstore platform with a RAG-based chatbot. Our evaluation across 40 questions showed that RAG significantly reduces hallucination and improves source citation accuracy compared to a no-retrieval baseline, with acceptable latency overhead. Guardrails further improve refusal correctness for out-of-scope queries. RAG is a practical and effective approach for domain-specific chatbot reliability in e-commerce, provided the knowledge base is actively maintained and reindexed in response to catalog changes.

**Key takeaways**:
- RAG is not a silver bullet but a strong foundation for reliable chatbots
- Reindexing and catalog grounding are critical for e-commerce applications
- Latency and cost trade-offs must be managed carefully in production

---

## References (2–3 trang)

**[Người viết: P5 AI expert — tìm papers; cả nhóm góp ý]**

**Suggested categories**:
1. Retrieval-Augmented Generation (RAG paper, DPR, ColBERT)
2. LLM-based conversational agents
3. E-commerce chatbots and recommendation systems
4. Vector databases (Qdrant, Pinecone papers/docs)
5. Hallucination detection and mitigation
6. Evaluation frameworks for QA systems

**Minimum 20 references**, ưu tiên papers từ ACL, EMNLP, NeurIPS, ICLR, WWW, RecSys, CHI.

---

## Appendix

### A. Evaluation Questions Sample

[Paste 5–10 example questions from `evaluation-questions.csv`]

### B. Prompt Templates

[Show exact prompts for No RAG, RAG, RAG+Guardrails]

### C. System Screenshots

[Include chatbot UI, admin reindex button, sample conversation with sources]

---

# Ước tính số trang

- Abstract: 0.5 trang
- Introduction: 2.5 trang
- Related Work: 2.5 trang
- System Design: 3.5 trang
- Methodology: 2.5 trang
- Results: 3.5 trang
- Discussion: 2 trang
- Limitations: 1 trang
- Future Work: 1 trang
- Conclusion: 1 trang
- References: 2 trang
- **Total: ~22–25 trang** (format IEEE double-column hoặc ACM format)

