<!---
  HS Resolver design doc for feature-chatbot branch
-->
# HS Resolver Service – Architecture & Data Flow

## Goal

Design the backend service that powers the HS Code AI chatbot. The resolver receives product descriptions and returns one or more harmonised system (HS) code suggestions together with confidence scores, disambiguation prompts, and rationales.

---

## High-Level Architecture

```
Chat Widget (React)
      │
      ▼
Backend REST endpoint  /api/hs/resolve
      │
      ├── Input normalisation & validation
      │
      ├── Resolver Engine (LLM + Rules hybrid)
      │     ├─ Rule-based keyword/entity matcher (fast path)
      │     ├─ Embedding similarity search over HS descriptions
      │     └─ LLM summariser / reasoner (OpenAI GPT-4o mini)
      │
      ├── Confidence aggregator & thresholding
      │     ├─ Weighted average of rule score, similarity score, LLM confidence
      │     └─ Normalises to [0,1], enforces format ####.##.##
      │
      ├── Disambiguation engine
      │     └─ Computes distinguishing attributes and follow-up questions
      │
      ├── Persistence (optional based on consent flag)
      │     ├─ chat_turn table (conversation transcript)
      │     └─ hs_resolution table (final HS candidates, confidence, rationale)
      │
      └── Response DTO
            {
              queryId,
              candidates: [
                { hsCode, confidence, rationale, source ("RULE" | "LLM" | "HYBRID") }
              ],
              disambiguationQuestions: [ ... ],
              fallback: { lastUsedCodes, manualSearchUrl }
            }
```

---

## Hybrid Resolution Strategy

| Stage | Purpose | Implementation Notes |
|-------|---------|----------------------|
| 1. **Rule-based Matcher** | Quick wins using keyword, NAICS/HS mappings | - HS reference data stored in `hs_code_reference` table (seeded weekly)<br>- Keyword dictionary derived from official descriptions (materials, industry, usage)<br>- Uses trigram similarity + token presence<br>- Produces initial confidence (0.4 weight) |
| 2. **Embedding Similarity** | Semantic search against HS descriptions | - Use OpenAI text-embedding-3-small (or local alternative)<br>- Pre-compute embeddings for HS reference table<br>- Query embedding compared via cosine similarity<br>- Confidence derived from similarity score (0.3 weight) |
| 3. **LLM Reasoner** | Natural language reasoning, disambiguation hints | - Prompt instructs model to return structured JSON with up to 3 HS suggestions<br>- Few-shot examples for common industries<br>- Extracts relevant attributes (material, use, composition)<br>- Confidence from model response (0.3 weight after calibration) |

Final confidence = `(ruleScore * 0.4) + (similarityScore * 0.3) + (llmScore * 0.3)`  
Threshold: default 0.7 (configurable via environment variable `HS_RESOLVER_CONFIDENCE_THRESHOLD`)

---

## Disambiguation Flow

When the top two or three candidates are within ±10% confidence:
1. Identify distinguishing attributes via diff of HS description keywords.
2. Generate 1–3 targeted follow-up questions (e.g., “Is the casing made of plastic or metal?”).
3. Return questions to frontend in `disambiguationQuestions` array.
4. Frontend sends answers via `/api/hs/resolve` with `context.previousAnswers`.
5. Resolver recalculates scores incorporating new attributes.

---

## Persistence Model (Optional – when user consents)

**Tables:**

```
hs_chat_session (
  id UUID PK,
  user_id FK nullable (for anonymous users store session key),
  consent_logging boolean,
  created_at timestamptz
)

hs_chat_turn (
  id UUID PK,
  session_id FK -> hs_chat_session.id,
  role text CHECK ('user','assistant'),
  content text,
  resolved boolean default false,
  created_at timestamptz
)

hs_resolution (
  id UUID PK,
  session_id FK,
  hs_code varchar(10),
  confidence numeric(4,3),
  rationale text,
  source text,
  confirmed boolean default false,
  created_at timestamptz
)
```

Consent flag stored per session; if false, chat_turn and hs_resolution rows are not inserted.

---

## API Contract Proposal

### Request

`POST /api/hs/resolve`

```json
{
  "queryId": "optional-uuid-to-correlate",
  "productName": "Example product title",
  "description": "Longer free-text description from user",
  "attributes": ["plastic", "consumer electronics", "portable"],
  "previousAnswers": [
    {
      "questionId": "material-clarification",
      "answer": "Aluminum casing"
    }
  ],
  "sessionId": "chat-session-uuid (generated client-side or returned from server)",
  "consentLogging": true
}
```

Validation:
- productName or description required (min length 10 characters)
- attributes optional array of strings
- Session ID optional; generated server-side when absent
- Rate limiting via IP + session (e.g., 5 req/minute)

### Response

```json
{
  "queryId": "echoed or generated",
  "sessionId": "uuid",
  "candidates": [
    {
      "hsCode": "8471.30.0100",
      "confidence": 0.81,
      "rationale": "Portable automatic data processing machine with keyboard and display.",
      "source": "HYBRID",
      "attributesUsed": ["portable", "data processing", "electronic"]
    },
    {
      "hsCode": "8471.41.0000",
      "confidence": 0.73,
      "rationale": "Automatic data processing units; more suited for servers without integrated display.",
      "source": "HYBRID"
    }
  ],
  "disambiguationQuestions": [
    {
      "id": "power-source",
      "question": "Is the device designed to operate without an external power supply?",
      "options": ["Yes", "No", "Not sure"]
    }
  ],
  "fallback": {
    "lastUsedCodes": [
      {
        "hsCode": "8471.30.0100",
        "confidence": 0.78,
        "timestamp": "2025-11-10T10:30:00Z"
      }
    ],
    "manualSearchUrl": "https://hts.usitc.gov/"
  },
  "notice": {
    "message": "We may store chat history to improve results.",
    "privacyPolicyUrl": "/privacy",
    "consentGranted": true
  }
}
```

---

## Error Handling

| Scenario | Response |
|----------|----------|
| Invalid input | `400 Bad Request` with validation details |
| Rate limit exceeded | `429 Too Many Requests` + retry-after header |
| LLM timeout/error | Return best available rule/similarity results, set `fallback.error = "LLM_UNAVAILABLE"`, surface toast on frontend |
| No HS code resolved | Confidence < threshold → `candidates` empty, fallback populated with last stored HS codes or manual link |
| Unexpected failure | `500 Internal Server Error`, log with requestId, inform user with generic message “We hit a snag, please try again” |

Logging (structured):
```json
{
  "event": "hs_resolve",
  "sessionId": "...",
  "requestId": "...",
  "durationMs": 1234,
  "confidenceTop": 0.81,
  "numCandidates": 2,
  "usedLLM": true,
  "status": "SUCCESS"
}
```

Metrics (to emit via Micrometer/Prometheus):
- `hs_resolver_requests_total` (labels: outcome, source)
- `hs_resolver_latency_ms` (histogram)
- `hs_resolver_confidence` (gauge of top candidate)
- `hs_resolver_disambiguation_needed_total`

---

## Data Flow Summary

1. Frontend sends user message → POST `/api/hs/resolve`.
2. Backend validates input, loads session metadata.
3. Executes rule-based matcher → collects top candidates + scores.
4. Generates embeddings → similarity search → additional scores.
5. Formulates LLM prompt (few-shot). If LLM available, merges output.
6. Aggregates and normalises confidence scores.
7. Runs disambiguation logic if required.
8. Persists chat turn and results (if consentLogging true).
9. Responds with candidates, questions, fallback data.
10. Frontend displays typing indicator until response arrives, then renders suggestions.

---

## Open Questions / Next Steps

1. Confirm LLM provider & pricing: use OpenAI GPT-4o mini / Azure / local? (Feature flag `HS_RESOLVER_USE_LLM`).
2. Determine seeding process for HS reference table. Start with US HTS or WCO HS chapters?
3. Finalise rate limiting strategy (per IP, per user, per session).
4. Decide retention policy for chat logs (e.g., 30 days) and anonymisation fields.
5. Evaluate fallback manual search URL per user’s country (e.g., US, EU, Singapore HS lookup portals).

Once these decisions are confirmed, implementation can begin following the component flow above.

