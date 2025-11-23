# ✈️ 여행.zip (Travel.zip) | 백엔드 시스템 아키텍처

AI 기반 일정 최적화, 실시간 데이터 동기화, 민주적 의사결정 시스템을 위한 견고하고 확장 가능한 백엔드 플랫폼

---

## 🌐 프로젝트 개요

여행.zip은 그룹 여행의 비효율적인 계획 과정을 혁신하는 협업형 여행 플래너입니다. 백엔드는 AI 기반 비즈니스 로직 처리, 실시간 데이터 동기화, 그리고 안정적인 데이터 관리를 책임집니다.

---

## 1. 프로젝트 목표 및 차별화 전략

| 항목 | 목표 | 백엔드 역할 |
|-----|------|-----------|
| **복잡성 해소** | 그룹 여행 준비 시간 60% 단축 및 갈등 최소화 | AI 알고리즘(일정/동선 최적화) 구현 및 민주적 의사결정(투표) 로직 처리 |
| **실시간 협업** | 모든 사용자에게 변경 사항 즉각 동기화 | WebSocket (또는 유사 기술) 기반의 실시간 데이터 처리 및 동기화 API 구축 |
| **지역 특화** | 한국 사용자만을 위한 세밀한 데이터 설계 | 한국 관광 데이터(공공 API 등) 연동 및 로컬 추천 시스템 로직 개발 |
| **상생 경제** | 지역경제 활성화 기여 | 지역 상권 연계 및 관광 수입 환원 메커니즘을 위한 데이터 구조 설계 |

---

## 2. 기술 스택 (Backend Stack)

확장성, 안정성, 그리고 높은 성능을 보장하기 위해 엔터프라이즈급 백엔드 스택을 채택했습니다.

| 분류 | 기술 스택 | 설명 |
|-----|---------|------|
| **프레임워크** | Spring Boot (Java) | 빠르고 안정적인 API 개발 및 비즈니스 로직 구현을 위한 핵심 프레임워크 |
| **데이터베이스** | MySQL | 여행 일정, 사용자, 예산, 투표 결과 등 핵심 관계형 데이터의 안정적 저장 및 관리 |
| **API 통신** | RESTful API | 프론트엔드와의 표준적인 통신 프로토콜. 보안 및 인증 처리 담당 |
| **실시간 처리** | WebSocket (STOMP 등) | 실시간 협업 환경(일정 수정, 채팅, 투표)을 위한 양방향 통신 채널 구축 |
| **AI 통합** | Gemini API 연동 | AI 기반 일정 자동 생성 및 동선 최적화 모듈 연동 및 관리 |

---

## 3. 백엔드 아키텍처 (Architecture)

Spring Boot 기반의 모놀리식 구조(초기)로 시작하며, 추후 마이크로서비스(MSA)로의 확장을 고려합니다.

- **API Layer**: 사용자 요청을 수신하고 응답을 반환하는 계층 (Controller). 인증/인가 및 요청 유효성 검사 담당.
- **Service Layer**: 핵심 비즈니스 로직을 구현하는 계층. AI 모듈 호출, 투표 결과 집계, 예산 정산 로직 등 처리.
- **Persistence Layer**: 데이터베이스(MySQL)와의 통신을 담당하는 계층 (Repository). 데이터의 CRUD 작업 수행.
- **Real-time Module**: 실시간 데이터 동기화를 위한 별도의 모듈 (WebSocket Server).

---

## 4. 데이터 모델 (Key Data Entities)

| 엔티티 (Entity) | 주요 필드 | 설명 |
|---------------|---------|------|
| **Trip (여행)** | id, name, startDate, endDate, style, ownerId | 여행의 기본 정보 및 참여 그룹 관리 |
| **Itinerary (일정)** | id, tripId, day, time, placeId, description | 일자별/시간별 상세 일정 및 장소 정보 |
| **Vote (투표)** | id, tripId, targetType, targetId, voterId, choice | 여행지, 숙소, 식당 등에 대한 사용자 투표 정보 저장 |
| **Budget (예산)** | id, tripId, category, amount, payerId, participants | 항목별 예산 계획 및 비용 분담/정산 데이터 |
| **User (사용자)** | id, nickname, profileImage, authProvider | 사용자 인증 및 권한 관리 정보 |

---

## 5. Git 협업 워크플로우 (Git Workflow)

우리 프로젝트는 안정적인 개발 및 배포 관리를 위해 **Git Flow** 전략을 사용합니다. 모든 작업은 **Pull Request(PR)**를 통해 병합되어야 하며, 코드 리뷰를 필수로 거칩니다.

### 5.1. 주요 브랜치

| 브랜치 | 역할 | 보호 설정 |
|-------|------|----------|
| `main` | 제품 출시(Production) 가능한 안정적인 코드. 절대 직접 커밋 금지. | PR을 통해서만 병합 허용 |
| `develop` | 다음 출시 버전을 위한 통합 개발 브랜치. 모든 기능 병합의 중심지. | PR을 통해서만 병합 허용 |

### 5.2. 보조 브랜치

| 브랜치 접두사 | 목적 | 생성 기준 | 병합 대상 |
|------------|------|----------|----------|
| `feature/` | 새로운 기능 개발 (API 구현, 로직 개발 등) | `develop` 브랜치에서 분기 | `develop` 브랜치로 PR |
| `release/` | 배포 준비 및 안정화 작업 | `develop` 브랜치에서 분기 | `main` 및 `develop` 브랜치로 PR |
| `hotfix/` | `main` 브랜치의 치명적인 버그 긴급 수정 | `main` 브랜치에서 분기 | `main` 및 `develop` 브랜치로 PR |

### 5.3. 개발 프로세스 (Feature Development)

#### 1. 브랜치 생성
`develop` 브랜치에서 작업할 기능 브랜치를 생성합니다.

```bash
git checkout develop
git pull origin develop
git checkout -b feature/TRIP-005_budget-settlement-api
```

#### 2. 커밋
작업 단위별로 명확한 커밋 메시지를 사용합니다.

```bash
# 예: feat(budget): 예산 정산 로직 구현 및 API 연동
git commit -m "feat(budget): 예산 정산 로직 구현 및 API 연동"
```

#### 3. PR 요청
기능 개발 완료 후, `feature/` 브랜치를 `develop` 브랜치로 병합하기 위한 PR을 생성합니다. (PR 템플릿 사용 권장)

#### 4. 코드 리뷰
리뷰어 승인 후 병합을 진행합니다.

---

## 💪 견고하고 효율적인 백엔드 시스템으로 그룹 여행의 미래를 구축합시다!

---

© 2025 여행.zip (Travel.zip) - All rights reserved.
