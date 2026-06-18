# Gateway Comparison PoC

APISIX AI Gateway PoC를 확장한 게이트웨이 비교용 프로젝트입니다.

`app-cs-service`와 `app-mock-llm`은 공통 비교 대상 애플리케이션으로 고정하고, 화면의 `Gateway 선택` 콤보박스에서 실제 호출할 게이트웨이를 바꿔가며 비교합니다.

## 구조

```text
gateway-comparison-poc/
  app-cs-service/             # CS 업무 서비스, 선택된 gateway만 호출
  app-mock-llm/               # OpenAI-compatible Mock LLM
  gateway/                    # 현재 기본 APISIX 설정
  gateways/
    apisix/                   # APISIX 설정 템플릿
    envoy/                    # Envoy 설정
    litellm/                  # LiteLLM Proxy 설정
  docker-compose.apps.yml     # 공통 앱 compose
  scripts/
    compose.ps1               # 공통 앱 + gateway compose 래퍼
    use-gateway.ps1           # gateway/ 폴더 교체용
    test-cs-summary.ps1       # CS 서비스 호출 테스트
```

## 실행

```powershell
cd "C:\Users\yhjeong\Documents\New project\gateway-comparison-poc"
.\scripts\compose.ps1 up -d --build
```

기본 포트가 이미 사용 중이면 외부 포트만 바꿔 실행할 수 있습니다.

```powershell
$env:CS_SERVICE_PORT="18081"
$env:GATEWAY_PORT="19080"
$env:ENVOY_GATEWAY_PORT="19081"
$env:LITELLM_GATEWAY_PORT="14000"
$env:MOCK_LLM_PORT="18082"
.\scripts\compose.ps1 up -d --build
```

PowerShell 실행 정책으로 `.ps1` 직접 실행이 막히면 다음처럼 실행합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\compose.ps1 up -d --build
```

브라우저 데모 화면:

```text
http://localhost:8081/
```

대체 포트로 실행했다면:

```text
http://localhost:18081/
```

화면에서 `연결 Gateway 전체 비교` 버튼을 누르면 같은 요청을 현재 연결된 APISIX, Envoy, LiteLLM에 순차 전송하고 아래 항목을 표로 비교합니다.

- HTTP status
- gateway decision
- Mock LLM 도달 여부
- 처리 시간
- 실제 target gateway URL
- route path
- raw gateway response preview

## 연결 여부 기준

이 PoC에서 `현재 연결됨`은 단순히 콤보박스에 이름만 있는 상태가 아닙니다. 다음 조건을 만족해야 합니다.

- Docker compose에 해당 게이트웨이 컨테이너가 정의되어 있음
- `app-cs-service`가 컨테이너 네트워크 내부 URL로 호출할 수 있음
- 선택된 게이트웨이가 `app-mock-llm:8082`까지 요청을 전달함
- 화면에서 `Gateway로 전송`을 눌렀을 때 실제 200/401/429 같은 결과를 받을 수 있음

`후보` 또는 `표시만`은 아직 위 조건을 만족하지 않는 상태입니다.

## Service 전단 개인정보 가드

`app-cs-service`는 게이트웨이에 요청을 보내기 전에 상담 본문에서 11자리 휴대폰 번호를 검사합니다.

감지 패턴:

```text
01012345678
010-1234-5678
010 1234 5678
```

화면의 `개인정보 처리` 콤보박스에서 다음 정책을 선택할 수 있습니다.

| 정책 | 동작 | Gateway 호출 여부 |
| --- | --- | --- |
| 전화번호 마스킹 | `01012345678`을 `010-****-5678`로 바꾼 뒤 전송 | 호출함 |
| 전화번호 포함 시 호출 차단 | 전화번호가 있으면 Service 단계에서 요청 차단 | 호출하지 않음 |
| 검사 끄기 | 원문 그대로 전송 | 호출함 |

차단 정책이 동작하면 데모 응답의 gateway decision은 `BLOCKED_BY_PRIVACY_GUARD`가 됩니다. 이 경우 APISIX, Envoy, LiteLLM 어느 게이트웨이도 호출되지 않고 Mock LLM에도 도달하지 않습니다.

`/counsels/summary` 기본 API는 안전한 기본값으로 전화번호 마스킹 정책을 사용합니다.

## 연결된 게이트웨이

| Gateway | 내부 호출 URL | 외부 데모 포트 | 주요 경로 | 인증 | 연결 방식 |
| --- | --- | --- | --- | --- | --- |
| Apache APISIX | `http://gateway:9080` | `${GATEWAY_PORT:-9080}` | `/ai/v1/chat/completions` | `X-API-KEY: test-api-key` | APISIX standalone YAML route와 plugin으로 Mock LLM에 proxy |
| Envoy | `http://gateway-envoy:9080` | `${ENVOY_GATEWAY_PORT:-9081}` | `/ai/v1/chat/completions` | `X-API-KEY: test-api-key` | Envoy route + prefix rewrite + Lua filter로 PoC용 인증/rate limit 처리 |
| LiteLLM Proxy | `http://gateway-litellm:4000` | `${LITELLM_GATEWAY_PORT:-4000}` | `/v1/chat/completions` | `Authorization: Bearer sk-test-api-key` | Mock LLM을 OpenAI-compatible provider로 등록해 LiteLLM Proxy 경유 |

### APISIX

APISIX는 gateway plugin 비교에 가장 직접적인 기준점입니다.

- `key-auth` plugin으로 API key를 검증합니다.
- `limit-count` plugin으로 rate limit 케이스를 검증합니다.
- `proxy-rewrite`로 `/ai/v1/chat/completions`를 `/v1/chat/completions`로 바꿔 upstream에 전달합니다.
- 운영형 API Gateway 기능, 플러그인 생태계, route/consumer 정책 비교에 적합합니다.

### Envoy

Envoy는 범용 L7 proxy 기준점입니다.

- route와 `prefix_rewrite`로 Mock LLM에 전달합니다.
- PoC에서는 Lua filter로 API key 검증과 rate limit 케이스를 흉내냅니다.
- APISIX처럼 AI Gateway 제품 기능이 있는 것은 아니지만, 고성능 프록시/서비스 메시/필터 기반 확장 관점에서 비교할 수 있습니다.
- 현재 rate limit은 PoC용 Lua 카운터라 실제 운영용 rate limit 구현과는 다릅니다.

### LiteLLM Proxy

LiteLLM은 AI Gateway 관점에서 비교 가치가 큽니다.

- OpenAI-compatible `/v1/chat/completions` API를 제공합니다.
- `model_list`에 `internal-counsel-mock-llm`을 등록하고 upstream을 `app-mock-llm:8082/v1`로 지정했습니다.
- LiteLLM master key를 `sk-test-api-key`로 설정해 proxy 인증을 검증합니다.
- 모델 라우팅, provider 추상화, LLM 사용량/비용 추적, fallback 같은 AI Gateway 기능 비교에 적합합니다.
- APISIX/Envoy처럼 `/ai/v1/...`를 rewrite하는 구조가 아니라, OpenAI-compatible LLM endpoint 자체를 제공하는 구조입니다.

## 연결되지 않은 게이트웨이

| Gateway | 화면 표시 | 왜 아직 연결하지 않았나 | 연결하려면 필요한 것 |
| --- | --- | --- | --- |
| Portkey AI Gateway | 무료 self-host 후보 | 로컬 self-host gateway 실행 방식과 Mock LLM을 provider로 붙이는 설정을 아직 PoC에 넣지 않음 | Portkey gateway 컨테이너, provider/virtual key 설정, Mock LLM upstream 매핑 |
| Cloudflare AI Gateway | 무료 플랜, 외부 계정 필요 | Cloudflare 계정과 외부 AI Gateway endpoint가 필요해 순수 로컬 PoC에 기본 연결하지 않음 | Cloudflare 계정, AI Gateway URL, provider/API key 설정 |
| OpenRouter | 무료 모델 가능, API 키 필요 | 외부 SaaS API라 API key와 모델별 제한이 필요함 | OpenRouter API key, 무료 모델 선택, outbound network 호출 허용 |
| Kong Gateway | 유료/라이선스 주의 | 최신 Kong Gateway Enterprise free mode deprecation 이슈가 있어 무료 PoC 기본 연결 대상에서 제외 | 사용 가능한 라이선스/배포 방식 확정, route/auth/rate limit 설정 |

### Portkey

Portkey는 AI Gateway 성격이 있어 비교 후보로는 좋습니다. 다만 APISIX/Envoy처럼 단순 reverse proxy 설정만으로 끝나는지, LiteLLM처럼 provider/virtual key 설정이 필요한지 확인이 필요합니다. 다음 단계로 붙인다면 Portkey self-host 컨테이너를 올리고 Mock LLM을 OpenAI-compatible provider로 등록하는 방식이 될 가능성이 큽니다.

### Cloudflare AI Gateway

Cloudflare AI Gateway는 Cloudflare 계정과 Cloudflare가 제공하는 gateway endpoint를 전제로 합니다. 무료 플랜으로 시작할 수는 있지만 로컬 Docker 네트워크 안에서만 닫힌 PoC를 구성하는 목적과는 다릅니다. 그래서 화면에는 후보로 표시하되 기본 연결은 하지 않았습니다.

### OpenRouter

OpenRouter는 OpenAI-compatible 외부 API입니다. 무료 모델이 있을 수 있지만 API key가 필요하고, 모델별 제공 여부와 제한이 바뀔 수 있습니다. 이 PoC에서는 외부 비용/계정 의존성을 줄이기 위해 표시만 합니다.

### Kong Gateway

Kong은 API Gateway 비교 후보지만, 무료/라이선스 조건이 APISIX/Envoy/LiteLLM만큼 단순하지 않습니다. 유료 또는 라이선스 확인이 필요한 후보로 표시하고, 무료 조건이 확정되기 전에는 기본 연결하지 않습니다.

## 게이트웨이별 차이 요약

| 비교 항목 | APISIX | Envoy | LiteLLM Proxy | Portkey | Cloudflare AI Gateway | OpenRouter | Kong |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 기본 성격 | API Gateway | L7 Proxy | LLM Gateway | AI Gateway | Managed AI Gateway | LLM API Router | API Gateway |
| 현재 연결 | 연결됨 | 연결됨 | 연결됨 | 미연결 | 미연결 | 미연결 | 미연결 |
| 로컬 self-host | 가능 | 가능 | 가능 | 가능 후보 | 아님 | 아님 | 조건 확인 필요 |
| 외부 계정 필요 | 없음 | 없음 | 없음 | 없음으로 구성 가능 후보 | 필요 | 필요 | 배포 방식에 따라 다름 |
| Mock LLM 연결 방식 | route + rewrite | route + rewrite | provider/model mapping | provider 설정 필요 | 외부 endpoint 설정 필요 | 외부 API 호출 | route/plugin 설정 필요 |
| 인증 비교 | `key-auth` plugin | Lua filter PoC | master key | virtual key 예상 | Cloudflare 설정 | API key | plugin 설정 |
| Rate limit 비교 | `limit-count` plugin | Lua filter PoC | 별도 설정 필요 | 별도 설정 필요 | Cloudflare 정책 | provider 제한 | plugin 설정 |
| AI 특화 기능 | plugin 기반 확장 | 직접 구현 필요 | 강함 | 강함 | managed 기능 | 모델 라우팅 중심 | plugin/enterprise 기능 |

## 운영 관점 예상 사양, 비용, 개발 공수

아래 표는 초기 운영 산정용 기준입니다. 실제 사양은 TPS, 요청/응답 크기, 로그 보관량, TLS 종료 위치, HA/DR 요구사항, Kubernetes 사용 여부에 따라 달라집니다.

| Gateway | 라이선스/제품 비용 | 최소 운영 사양 예시 | 운영 비용 항목 | 개발 공수 | 운영 난이도 | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| APISIX | Apache 2.0 기반 OSS 사용 시 라이선스 비용 없음 | 2 vCPU / 2-4 GB RAM x 2대 이상 권장 | VM/Pod, 로그/모니터링, 인증/route 설정 관리 | 중간 | 중간 | API Gateway 정책을 plugin으로 관리하기 좋아 운영형 정책 비교에 적합 |
| Envoy | Apache 2.0 OSS, 라이선스 비용 없음 | 1-2 vCPU / 512 MB-2 GB RAM x 2대 이상 권장 | VM/Pod, config 배포, 로그/메트릭, xDS 사용 시 control plane | 높음 | 높음 | 고성능 proxy지만 인증/rate limit/AI 정책은 직접 구성하거나 외부 컴포넌트 필요 |
| LiteLLM Proxy | OSS self-host 가능, Enterprise 기능은 별도 상용 가능 | 2 vCPU / 2-4 GB RAM x 2대 이상, 운영 DB 권장 | VM/Pod, Postgres/Redis 선택, LLM 사용량 비용, key/예산 관리 | 중간-높음 | 중간-높음 | LLM provider/model 관리, budget, usage tracking, fallback 같은 AI Gateway 기능에 적합 |

### 비용 산정 포인트

APISIX와 Envoy는 자체 소프트웨어 라이선스 비용보다 인프라 비용이 중심입니다. 실제 운영 비용은 gateway pod/VM 수, 로그량, 모니터링, 트래픽 egress, HA 구성이 좌우합니다.

LiteLLM은 self-host proxy 자체는 무료로 시작할 수 있지만, 실제 운영에서는 연결된 LLM provider 비용이 핵심입니다. 또한 team/key/budget 관리, audit, SSO, SLA 같은 엔터프라이즈 기능이 필요하면 상용 옵션 검토가 필요합니다.

### 개발 공수 기준

| 작업 | APISIX | Envoy | LiteLLM Proxy |
| --- | --- | --- | --- |
| 단순 라우팅 | 낮음 | 낮음 | 낮음 |
| 인증 정책 | 낮음, plugin 사용 | 중간-높음, filter/ext_authz 필요 | 낮음-중간, master key/key 관리 |
| Rate limit | 낮음, plugin 사용 | 중간-높음, local/global rate limit 구성 필요 | 중간, key/model별 정책 설계 필요 |
| LLM provider 라우팅 | 중간, plugin/route 설계 필요 | 높음, 직접 구현 필요 | 낮음, model_list/provider mapping |
| 관측/비용 추적 | 중간, 로그/외부 시스템 연동 | 높음, 직접 연동 필요 | 낮음-중간, LLM usage 중심 기능 활용 |
| 운영 자동화 | 중간 | 높음 | 중간 |

### 운영 선택 가이드

- API Gateway 표준 기능이 중심이면 APISIX가 가장 균형적입니다.
- 서비스 메시, 고성능 L7 proxy, 세밀한 네트워크 제어가 중심이면 Envoy가 적합합니다.
- 여러 LLM provider, model routing, budget, usage tracking이 중심이면 LiteLLM Proxy가 가장 직접적입니다.
- 개인정보 마스킹/차단 같은 업무 정책은 이 PoC처럼 gateway 전 Service 단계에 두면 모든 gateway에 동일하게 적용할 수 있습니다.

## 테스트

CS 서비스를 통해 현재 선택 로직과 Mock LLM 응답을 확인합니다.

```powershell
.\scripts\test-cs-summary.ps1
```

연결된 게이트웨이를 한 번에 비교하려면:

```powershell
$body = @{
  gatewayId = "apisix"
  privacyPolicy = "MASK"
  scenario = "ROUTING"
  counselText = "고객 연락처는 01012345678입니다."
  apiKey = "test-api-key"
  gatewayPath = "/ai/v1/chat/completions"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:18081/demo/gateway-compare" `
  -ContentType "application/json; charset=utf-8" `
  -Body $body
```

게이트웨이별 직접 호출 예시:

```powershell
# APISIX
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:19080/ai/v1/chat/completions" `
  -ContentType "application/json" `
  -Headers @{ "X-API-KEY" = "test-api-key" } `
  -Body '{"model":"internal-counsel-mock-llm","messages":[{"role":"user","content":"배송 지연 문의"}],"temperature":0}'

# Envoy
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:19081/ai/v1/chat/completions" `
  -ContentType "application/json" `
  -Headers @{ "X-API-KEY" = "test-api-key" } `
  -Body '{"model":"internal-counsel-mock-llm","messages":[{"role":"user","content":"배송 지연 문의"}],"temperature":0}'

# LiteLLM
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:14000/v1/chat/completions" `
  -ContentType "application/json" `
  -Headers @{ "Authorization" = "Bearer sk-test-api-key" } `
  -Body '{"model":"internal-counsel-mock-llm","messages":[{"role":"user","content":"배송 지연 문의"}],"temperature":0}'
```

## 새 게이트웨이 추가 규칙

새 게이트웨이를 실제 연결 상태로 만들려면 다음 작업이 필요합니다.

1. `gateways/{gateway-name}/` 아래에 설정 파일과 `gateway.yml`을 추가합니다.
2. `gateway/docker-compose.gateway.yml`에 컨테이너 서비스를 추가합니다.
3. `GatewayCatalogService`에 `runnable=true`, 내부 `baseUrl`, `routePath`를 등록합니다.
4. 필요하면 `GatewayDemoService`에 인증 헤더나 경로 차이를 반영합니다.
5. `.\scripts\compose.ps1 up -d --build`로 재기동합니다.

이 조건을 만족하면 화면 콤보박스에서 선택한 게이트웨이로 실제 요청이 전송됩니다.
