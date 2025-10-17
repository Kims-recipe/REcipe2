# REcipe2 - 영양 관리 및 레시피 추천 앱

냉장고 재료 관리, 식단 기록, 영양소 추적 기능을 제공하는 Android 앱입니다.

## 주요 기능

- 📱 **냉장고 관리**: 재료 등록 및 유통기한 관리
- 🍳 **식단 기록**: 집밥/외식 구분하여 식사 기록
- 📊 **영양소 추적**: 일일 영양소 섭취량 실시간 모니터링
- 📅 **캘린더**: 과거 식단 기록 조회
- 👤 **마이페이지**: 목표 달성률 및 통계 확인
- 📖 **나만의 레시피**: 자주 만드는 요리 저장

## 기술 스택

- **언어**: Kotlin
- **아키텍처**: MVVM
- **백엔드**: Firebase (Authentication, Firestore, Storage)
- **UI**:
  - Material Design Components
  - View Binding
  - Navigation Component
- **라이브러리**:
  - Kizitonwose Calendar (캘린더 UI)
  - MPAndroidChart (통계 차트)
  - Coil (이미지 로딩)

## 시작하기

### 1. 사전 요구사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 8 이상
- Android SDK 30 이상

### 2. Firebase 설정

1. [Firebase Console](https://console.firebase.google.com/)에서 새 프로젝트 생성
2. Android 앱 추가:
   - 패키지 이름: `com.kims.recipe2`
   - `google-services.json` 다운로드
3. `google-services.json`을 `app/` 디렉토리에 복사

4. Firebase 콘솔에서 다음 서비스 활성화:
   - **Authentication**: Email/Password 로그인
   - **Firestore Database**: 데이터베이스 생성
   - **Storage**: 스토리지 버킷 생성

### 3. 프로젝트 빌드

```bash
git clone <repository-url>
cd REcipe2
./gradlew build
```

### 4. 앱 실행

Android Studio에서 프로젝트를 열고 Run (Shift+F10)

## 프로젝트 구조

```
app/src/main/java/com/kims/recipe2/
├── model/              # 데이터 모델
├── ui/
│   ├── auth/          # 로그인/회원가입
│   ├── home/          # 홈 (영양소 현황)
│   ├── fridge/        # 냉장고 관리
│   ├── calendar/      # 캘린더 (식단 기록)
│   └── mypage/        # 마이페이지
└── util/              # 유틸리티 함수
```

## Firestore 데이터 구조

```
users/{userId}/
  ├── userInfo/
  │   └── profile              # 유저 프로필 (키, 몸무게, 목표 영양소)
  ├── dailyNutrition/{date}    # 일일 영양소 데이터
  ├── mealRecords/{recordId}   # 식단 기록
  ├── userRecipes/{recipeId}   # 나만의 레시피
  └── ingredients/{itemId}     # 냉장고 재료

foods/                          # 외식 메뉴 데이터베이스
```

## 빌드 설정

### Minimum SDK
- **minSdk**: 30 (Android 11)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 35

### 주요 의존성

```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage-ktx")

// UI
implementation("com.google.android.material:material:1.12.0")
implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
implementation("com.kizitonwose.calendar:view:2.0.3")
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
implementation("io.coil-kt:coil:2.6.0")
```

## 개발 가이드

### 새로운 기능 추가

1. `model/` 디렉토리에 데이터 모델 추가
2. `ui/` 디렉토리에 Fragment 및 ViewModel 추가
3. `res/layout/` 에 레이아웃 XML 작성
4. `res/navigation/` 에서 네비게이션 그래프 업데이트

### Firebase 규칙 설정

Firestore 보안 규칙 예시:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /foods/{document=**} {
      allow read: if request.auth != null;
    }
  }
}
```

## 주의사항

### Firebase 설정 파일
- `google-services.json`은 반드시 본인의 Firebase 프로젝트에서 다운로드해야 합니다
- 기존 파일은 예시용이며, 실제 동작하지 않을 수 있습니다

### 민감한 파일
- `firebase-admin-key.json`: Git에 커밋하지 마세요 (.gitignore에 포함됨)
- Admin SDK 키는 서버 사이드 작업에만 사용됩니다

## 문제 해결

### 빌드 실패
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Firebase 연결 오류
1. `google-services.json` 파일이 `app/` 디렉토리에 있는지 확인
2. 패키지 이름이 `com.kims.recipe2`인지 확인
3. Firebase 콘솔에서 SHA-1 인증서 등록 (필요시)

### Sync 오류
- Android Studio에서 `File > Invalidate Caches / Restart`

## 라이선스

이 프로젝트는 교육 목적으로 제작되었습니다.

## 기여

Pull Request는 언제나 환영합니다!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 문의

프로젝트 관련 문의사항은 Issues 탭에 등록해주세요.
