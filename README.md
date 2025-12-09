# WeatherMap 프로젝트

이 프로젝트는 Supabase PostgreSQL → GeoTIFF → GeoServer ImageMosaic 파이프라인을 자동화하여  
최신 기상 데이터를 기반으로 시계열 래스터 레이어를 자동 생성·발행하는 시스템입니다.

---

## 1. 프로젝트 구조 (핵심만)

### Java (Spring Boot)
- controller/ : 날씨 조회 API
- dto/
- entity/
- repository/
- scheduler/ : 매일 04:05 기상데이터 수집 스케줄러
- service/

### Python
- raster/ : 생성된 GeoTIFF, indexer.properties 등
- seoul_boundary.* : 서울시 경계 SHP 파일
- tiff_maker.py : 04:10 TIFF 생성 스크립트
- environment.yml : Conda 환경 정의 파일

---

## 2. 주요 구성 요소

### (1) Supabase PostgreSQL (weather_raw 테이블)

저장되는 필드:
- lat, lon
- ts (timestamp)
- temp, rain
- geom (PostGIS Point 자동 생성)
- created_at

Spring Scheduler가 매일 04:05에 OpenWeather API를 호출하여 데이터를 저장합니다.

---

### (2) Python GeoTIFF 생성 모듈

#### Conda 환경 설정
```
conda env create -f environment.yml
conda activate weather-env
```

#### 실행
```
cd Python
python tiff_maker.py
```

#### 수행 역할
- Supabase weather_raw 데이터 조회
- IDW 보간 기반 온도 격자 생성
- 서울시 경계(shp)로 마스킹
- temp_YYYY-MM-DD_HH.tif 형식 GeoTIFF 생성
- ImageMosaic 인덱스 갱신

---

### (3) GeoServer (ImageMosaic)
```
필요 설정 단계:
0. 17 버전 이상 JDK 설치
1. GeoServer 설치
2. ImageMosaic 저장소 생성 후 raster 폴더 연결
3. SLD 스타일 생성 및 적용
4. TIFF 파일 추가 시:
    - 기존 인덱스 파일(rasters.\*) 삭제
    - GeoServer 재시작 또는 REST Reload
5. Python에서 TIFF 생성 → 자동 반영
```
---

## 3. 자동화 흐름 요약

### ① Spring Boot (04:05)
- OpenWeather API 호출
- weather_raw 테이블에 시간별 temp, rain 저장

### ② Python (04:10)
- 새 데이터 조회
- GeoTIFF 생성
- raster 폴더 저장
- 인덱스 갱신

### 결과
DB → TIFF → GeoServer 레이어로 최신 기상 데이터가 자동 발행됩니다.

---

## 4. 실행 순서

### 1) Conda 환경 구성
```
conda env create -f environment.yml
conda activate weather-env
```

### 2) Spring Boot 실행
```
./gradlew bootRun
```

### 3) Python TIFF 모듈 실행
```
python tiff_maker.py
```

---

## 5. 환경 파일

- environment.yml : Python TIFF 생성 환경 설정
- .env : Spring/Python 공통 환경 변수 (Git에는 포함되지 않음)