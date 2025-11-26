# Conda Env Setting

## 1. Environment Setup
conda 환경 생성:
```
conda env create -f environment.yml
```

환경 활성화:
```
conda activate weather-env
```

## 2. Run
```
cd Python
python tiff_maker.py
```

## 3. Files
- `environment.yml` : 프로젝트에 필요한 conda 환경 설정 파일
- `Python/` : Python 스크립트들이 들어 있는 폴더
    - `tiff_maker.py` : 실행 파일