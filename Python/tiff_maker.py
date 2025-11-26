import requests
import numpy as np
import geopandas as gpd
from shapely.geometry import Point
import rasterio
from rasterio.transform import from_origin
from rasterio.features import rasterize

# ==========================
# 1. Supabase REST 설정
# ==========================
SUPABASE_URL = "https://{server_id}.supabase.co"
SUPABASE_KEY = "{server_key}"
TABLE_NAME = "weather_raw"

# ==========================
# 2. 서울 영역 & 해상도
# ==========================
MIN_LON = 126.76
MAX_LON = 127.19
MIN_LAT = 37.41
MAX_LAT = 37.70

RESOLUTION = 0.001          # 0.005 : 더 고해상도 이지만, 경계가 뚜렷함

NODATA_VALUE = -9999.0      # 서울 밖에 넣을 값

# ==========================
# 3. GeoDataFrame 설정
# ==========================
def fetch_gdf_for_timestamp(ts_str: str) -> gpd.GeoDataFrame:
    url = f"{SUPABASE_URL}/rest/v1/{TABLE_NAME}"

    params = {
        "select": "lon,lat,temp",
        "ts": f"eq.{ts_str}",
        "temp": "not.is.null",
    }

    headers = {
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
    }

    r = requests.get(url, params=params, headers=headers)
    r.raise_for_status()
    rows = r.json()

    if not rows:
        raise ValueError(f"해당 시각 데이터 없음: {ts_str}")

    lons = [row["lon"] for row in rows]
    lats = [row["lat"] for row in rows]
    temps = [row["temp"] for row in rows]

    gdf = gpd.GeoDataFrame(
        {"temp": temps},
        geometry=[Point(x, y) for x, y in zip(lons, lats)],
        crs="EPSG:4326",
    )
    print(f"날짜 및 시각 : {ts_str}, 지점 개수: {len(gdf)}개")
    return gdf

# ==========================
# 4. IDW 보간
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
# 5. GeoTIFF 생성 + 서울 shp로 마스킹
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
    print(f"GeoTIFF 저장 → {out_tiff_path}")
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

    print("완료")

# ==========================
# 6. 실행 예시 (자동화 예정)
# ==========================

ts = "2025-11-27 17:00:00+00"
output_path = r"C:\GeoServer\data\rasters\temp_2025-11-27_17.tif"

create_tiff_for_timestamp(ts, output_path)
