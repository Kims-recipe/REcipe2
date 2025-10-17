# REcipe2 데모 유저 생성 스크립트

Firebase에 데모 유저를 생성하고 실제 영양소 및 식단 데이터를 자동으로 채웁니다.

## 스크립트 버전

- **create_demo_user.py**: 기본 버전 (3일간 데이터)
- **create_demo_user_v2.py**: 고급 버전 (6개월간 실제 사용자 패턴)

## 설치 방법

1. Python 3.7 이상이 설치되어 있는지 확인합니다.

2. 필요한 패키지를 설치합니다:
```bash
cd scripts
pip install -r requirements.txt
```

3. Firebase 서비스 계정 키 다운로드:
   - [Firebase Console](https://console.firebase.google.com/project/re-cipe/settings/serviceaccounts/adminsdk)에 접속
   - "새 비공개 키 생성" 버튼 클릭
   - 다운로드한 JSON 파일을 `REcipe2/firebase-admin-key.json`으로 저장

## 사용 방법

### 기본 버전 (3일간 데이터)
```bash
python3 create_demo_user.py
```

### 고급 버전 (6개월간 실제 사용자 패턴) - 추천!
```bash
python3 create_demo_user_v2.py
```

서비스 계정 키 파일 경로를 지정하려면:
```bash
FIREBASE_SERVICE_ACCOUNT=/path/to/your/key.json python3 create_demo_user_v2.py
```

## 생성되는 데이터

### 공통 데이터

#### 1. 데모 유저 계정
- 이메일: `demo@recipe2.com`
- 비밀번호: `demo123456`

#### 2. 유저 프로필
- 키: 175cm
- 몸무게: 70kg
- 성별: 남성
- 목표 칼로리: 2200 kcal
- 목표 탄수화물: 275g
- 목표 단백질: 165g
- 목표 지방: 73g

#### 3. 냉장고 재료 (10종)
- 계란, 우유, 돼지고기, 닭고기
- 양파, 마늘, 대파, 김치
- 두부, 된장
- 유통기한 정보 포함

---

### v2 (고급 버전) 추가 데이터

#### 1. 6개월간 일일 영양소 데이터 (180일)
- 칼로리, 탄수화물, 단백질, 지방
- 칼슘, 철분, 나트륨, 비타민 A, 비타민 C
- 목표의 70-110% 사이 자연스러운 변화

#### 2. 540개의 식단 기록 (6개월 × 3끼)
**집밥 (65%)**:
- 냉장고 재료를 선택하여 요리
- 레시피: 김치찌개, 된장찌개, 계란볶음밥, 제육볶음, 김치볶음밥, 두부조림, 계란말이
- 재료 정보 포함 (예: "김치찌개 (김치, 돼지고기, 두부)")
- 밥과 반찬 자동 조합

**외식 (35%)**:
- 데이터베이스에서 검색하여 추가
- 22개 외식 메뉴: 한식, 중식, 일식, 양식
- 예: 짜장면, 돈까스, 스테이크, 피자 등

#### 3. 나만의 레시피 (3개)
자주 만드는 집밥을 레시피로 저장:
- 나만의 김치찌개
- 나만의 계란볶음밥
- 나만의 제육볶음

#### 4. 외식 메뉴 데이터베이스 (22개)
실제 음식점 메뉴와 영양 정보 포함

## 주의사항

- 이미 동일한 이메일로 생성된 유저가 있으면 자동으로 삭제 후 재생성됩니다.
- Firebase Admin SDK를 사용하므로 서비스 계정 키 파일이 필요합니다.
- 실행 전 Firebase 프로젝트 설정이 올바른지 확인하세요.

## 문제 해결

### 서비스 계정 키 오류
```
ValueError: Invalid service account certificate
```
해결 방법:
1. [Firebase Console](https://console.firebase.google.com/project/re-cipe/settings/serviceaccounts/adminsdk)에서 서비스 계정 키 다운로드
2. 다운로드한 JSON 파일을 프로젝트 루트에 `firebase-admin-key.json`으로 저장
3. 또는 환경변수로 경로 지정: `export FIREBASE_SERVICE_ACCOUNT=/path/to/key.json`

### Python 버전 오류
```
pip._vendor.pytoml.core.TomlError
```
- Python 3.8은 지원이 종료되어 최신 패키지와 호환성 문제가 있습니다.
- Python 3.9 이상으로 업그레이드하는 것을 권장합니다.
- 또는 pip을 업그레이드: `pip install --upgrade pip`

### 권한 오류
```
Error: Insufficient permissions
```
- Firebase 콘솔에서 Firestore 및 Authentication 권한을 확인하세요.
- 서비스 계정에 필요한 역할이 부여되어 있는지 확인하세요.
