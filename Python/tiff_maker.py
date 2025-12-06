import os
import schedule
import time
import requests
import numpy as np
import geopandas as gpd
from shapely.geometry import Point
import rasterio
from rasterio.transform import from_origin
from rasterio.features import rasterize
from dateutil import parser
from dotenv import load_dotenv

# ==========================
# 1. Supabase REST API URL 및 GEOSERVER URL 연결 데이터 불러오기
# ==========================

load_dotenv()

supabase_url = os.getenv('SUPABASE_URL')
supabase_key = os.getenv('SUPABASE_KEY')
table_name = os.getenv('TABLE_NAME')

geoserver_url = os.getenv('GEOSERVER_URL')
geoserver_user = os.getenv('GEOSERVER_USER')
geoserver_pass = os.getenv('GEOSERVER_PASS')
geoserver_root = os.getenv('GEOSERVER_ROOT')

workspace = os.getenv('WORKSPACE')
coverage_store = os.getenv('COVERAGE_STORE')

output_dir = os.getenv('OUTPUT_DIR')
ec2_raster_dir = os.getenv('EC2_RASTER_DIR')

# ==========================
# 2. 서울 영역 & 해상도
# ==========================
MIN_LON = 126.76
MAX_LON = 127.19
MIN_LAT = 37.41
MAX_LAT = 37.70

RESOLUTION = 0.001          # 0.005 : 더 고해상도 이지만, 경계가 뚜렷함

NODATA_VALUE = -9999.0      # 서울 밖에 넣을 값

headers = { 
    "apikey": supabase_key, 
    "Authorization": f"Bearer {supabase_key}", 
}

# ==========================
# 3. weather_raw 테이블에서 모든 ts 가져오기
# ==========================
def fetch_all_timestamps():
    url = f"{supabase_url}/rest/v1/{table_name}"
    all_ts = set()
    page_size = 1000
    offset = 0

    while True:
        params = {
            "select": "ts",
            "order": "ts.asc",
            "limit": page_size,
            "offset": offset,
        }

        r = requests.get(url, params=params, headers=headers)
        r.raise_for_status()
        rows = r.json()

        if not rows:
            break

        for row in rows:
            ts_val = row.get("ts")
            if ts_val is not None:
                all_ts.add(ts_val)

        if len(rows) < page_size:
            # 마지막 페이지
            break

        offset += page_size

    ts_list = sorted(all_ts)
    print("ts 개수:", len(ts_list))
    if ts_list:
        print("ts 범위:", ts_list[0], "~", ts_list[-1])
    return ts_list

# ==========================
# 4. GeoDataFrame 설정
# ==========================
def fetch_gdf_for_timestamp(ts_str: str) -> gpd.GeoDataFrame:
    url = f"{supabase_url}/rest/v1/{table_name}"

    params = {
        "select": "lon,lat,temp",
        "ts": f"eq.{ts_str}",
        "temp": "not.is.null",
    }

    r = requests.get(url, params=params, headers=headers)
    r.raise_for_status()
    rows = r.json()

    if not rows:
        raise ValueError(f"데이터 없음: {ts_str}")

    lons = [row["lon"] for row in rows]
    lats = [row["lat"] for row in rows]
    temps = [row["temp"] for row in rows]

    gdf = gpd.GeoDataFrame(
        {"temp": temps},
        geometry=[Point(x, y) for x, y in zip(lons, lats)],
        crs="EPSG:4326",
    )
    return gdf

# ==========================
# 5. IDW 보간
# ==========================
def idw_interpolation(grid_x, grid_y, xs, ys, values, power=2, eps=1e-12):
    weighted_sum = np.zeros_like(grid_x, dtype=float)
    weight_total = np.zeros_like(grid_x, dtype=float)

    for x, y, v in zip(xs, ys, values):
        d2 = (grid_x - x) ** 2 + (grid_y - y) ** 2
        d2[d2 == 0] = eps
        w = 1.0 / (d2 ** (power / 2.0))
        weighted_sum += w * v
        weight_total += w

    return weighted_sum / weight_total

# ==========================
# 6. GeoTIFF 생성 + 서울 shp로 마스킹
# ==========================
def create_tiff_for_timestamp(ts_str: str, out_tiff_path: str):
    # 1) 포인트 데이터
    gdf = fetch_gdf_for_timestamp(ts_str)
    xs = gdf.geometry.x.values
    ys = gdf.geometry.y.values
    temps = gdf["temp"].values

    # 2) 그리드
    width = int(np.ceil((MAX_LON - MIN_LON) / RESOLUTION))
    height = int(np.ceil((MAX_LAT - MIN_LAT) / RESOLUTION))

    lon_coords = np.linspace(MIN_LON + RESOLUTION/2, MAX_LON - RESOLUTION/2, width)
    lat_coords = np.linspace(MAX_LAT - RESOLUTION/2, MIN_LAT + RESOLUTION/2, height)

    grid_lon, grid_lat = np.meshgrid(lon_coords, lat_coords)

    # 3) IDW 보간
    temp_grid = idw_interpolation(grid_lon, grid_lat, xs, ys, temps, power=2)

    # 4) transform (서울 bbox 기준)
    transform = from_origin(MIN_LON, MAX_LAT, RESOLUTION, RESOLUTION)

    # 5) 서울시 경계 shp 읽어서 mask 생성
    seoul = gpd.read_file("seoul_boundary.shp")

    if seoul.crs is None or seoul.crs.to_string() != "EPSG:4326":
        seoul = seoul.to_crs("EPSG:4326")

    shapes = [(geom, 1) for geom in seoul.geometry]

    mask = rasterize(
        shapes=shapes,
        out_shape=temp_grid.shape,
        transform=transform,
        fill=0,              # 서울 밖 = 0
        all_touched=True,
        dtype="uint8",
    )

    # 서울 안(1)은 유지, 밖(0)은 NODATA로
    temp_grid_masked = np.where(mask == 1, temp_grid, NODATA_VALUE)

    # 6) GeoTIFF 저장
    os.makedirs(os.path.dirname(out_tiff_path), exist_ok=True)

    print(f"[저장] {out_tiff_path}")
    with rasterio.open(
        out_tiff_path,
        "w",
        driver="GTiff",
        height=height,
        width=width,
        count=1,
        dtype=rasterio.float32,
        crs="EPSG:4326",
        transform=transform,
        nodata=NODATA_VALUE,
    ) as dst:
        dst.write(temp_grid_masked.astype(np.float32), 1)

# ==========================
# 7. 자동 일괄 생성 (timestamptz → temp_YYYY-MM-DD_HH.tif 로 변환)
# ==========================

def ts_to_filename(ts_str: str) -> str:
    dt = parser.parse(ts_str)  # 문자열을 datetime으로 변환
    date_part = dt.strftime("%Y-%m-%d")
    hour_part = dt.strftime("%H")       # HH 형태
    return f"temp_{date_part}_{hour_part}.tif"

# 지오서버 인덱스 자동 갱신   
def harvest_to_geoserver(tiff_path):
    file_name = os.path.basename(tiff_path)
    file_url = f"file:{ec2_raster_dir}/{file_name}"

    # print("tiff_path:", tiff_path)
    # print("GeoServer:", file_url)

    url = (
        f"{geoserver_url}/rest/workspaces/"
        f"{workspace}/coveragestores/{coverage_store}/external.imagemosaic"
    )

    headers = {"Content-Type": "text/plain"}

    resp = requests.post(
        url,
        data=file_url,
        headers=headers,
        auth=(geoserver_user, geoserver_pass),
    )

    print("harvest status:", resp.status_code)
    print(resp.text)

# DB 내 새로 추가된 데이터 -> TIFF 생성
def create_all_tiffs(output_dir: str):
    ts_list = fetch_all_timestamps()
    new_files = [] 

    for ts_str in ts_list:
        file_name = ts_to_filename(ts_str)
        out_tiff_path = os.path.join(output_dir, file_name)

        if os.path.exists(out_tiff_path):
            continue
        try:
            create_tiff_for_timestamp(ts_str, out_tiff_path)
            new_files.append(out_tiff_path)
            upload_to_ec2(out_tiff_path)
        except ValueError as e:
            pass
    
    for path in new_files:
        harvest_to_geoserver(path)

    print("완료")

# AWS에 TIFF 파일 업로드
def upload_to_ec2(local_path):
    cmd = f'scp -i C:/Users/admin/geoserver-key.pem "{local_path}" ubuntu@43.203.150.74:{ec2_raster_dir}'
    result = os.system(cmd)
    if result == 0:
        print(f"{local_path} 전송 완료")
    else:
        print(f"{local_path} 전송 실패 (exit code={result})")


# 스케줄러로 매일 한번 실행

#schedule.every().day.at("15:57").do(create_all_tiffs, output_dir)
schedule.every().day.at("04:10").do(create_all_tiffs, output_dir)

while True:
    schedule.run_pending()
    time.sleep(1)