package com.kunsan.ac.kr.mygcs;

import android.graphics.PointF;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    //네이버 맵
    NaverMap myMap;

    //지오코드 객체 선언
    String defaultURL = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc";

    //json 리턴값 저장할 변수
    private String result="";


    //버튼변수생성
    private Button armBtn;
    private Button takeOffBtn;
    private Button landBtn;
    private Button lockBtn;
    private Button mapTypeBtn;
    private Button cadastralOffBtn;
    private Button clearBtn;

    //지도타입 버튼 변수
    private Button basicMap;
    private Button topoMap;
    private Button satelliteMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //버튼변수생성
        armBtn = (Button)findViewById(R.id.arm);
        takeOffBtn = (Button) findViewById(R.id.takeOff);
        landBtn = (Button)findViewById(R.id.land);
        lockBtn = (Button)findViewById(R.id.lock);
        mapTypeBtn = (Button)findViewById(R.id.mapType);
        cadastralOffBtn = (Button) findViewById(R.id.cadastralOff);
        clearBtn = (Button) findViewById(R.id.clear);

        //지도타입 버튼 변수
        basicMap = (Button) findViewById(R.id.basicMap);
        topoMap = (Button) findViewById(R.id.topoMap);
        satelliteMap = (Button) findViewById(R.id.satelliteMap);

        //지도
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

        armBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        takeOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        landBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        lockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mapTypeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        cadastralOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.myMap = naverMap;
        myMap.setMapType(NaverMap.MapType.Navi);
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(35.9452863, 126.6799643));
        naverMap.moveCamera(cameraUpdate);
        Toast.makeText(this.getApplicationContext(), "지도띄우기", Toast.LENGTH_SHORT).show();

        //마커띄우기
        Marker marker1 = new Marker();
        marker1.setPosition(new LatLng(35.9452863, 126.6799643));
        marker1.setMap(naverMap);
        Marker marker2 = new Marker();
        marker2.setPosition(new LatLng(35.9408996, 126.6864375));
        marker2.setMap(naverMap);
        Marker marker3 = new Marker();
        marker3.setPosition(new LatLng(35.9417, 126.6895613));
        marker3.setMap(naverMap);


        //지도 클릭시 이벤트
        myMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                AsyncTaskThread async = new AsyncTaskThread();
                try{
                    result = async.execute(latLng).get();
                    System.out.println(result);
                    System.out.println(latLng);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //정보창
        InfoWindow infoWindow = new InfoWindow();
        infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return "정보 창";
            }
        });


        //마커클릭시 이벤트
        Overlay.OnClickListener listener = overlay -> {
            Marker mark = (Marker) overlay;

            if (mark.getInfoWindow() == null) {
                // 현재 마커에 정보 창이 열려있지 않을 경우 엶
                infoWindow.open(mark);
            } else {
                // 이미 현재 마커에 정보 창이 열려있을 경우 닫음
                infoWindow.close();
            }

            return true;
        };


        marker1.setOnClickListener(listener);
        marker2.setOnClickListener(listener);
        marker3.setOnClickListener(listener);



        //구글 geocoder
        /*Geocoder geocoder = new Geocoder(this);
        myMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {//지도 클릭시
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                //지도를 클릭시 클릭한 부분의 위도와 경도를 Toast로 출력
                String text = "latitude = " + latLng.latitude + " longitude = " + latLng.longitude;
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();


                //구글 geocoder 사용
                List<Address> list = null;
                try {
                    list = geocoder.getFromLocation(
                            latLng.latitude,
                            latLng.longitude,
                            10);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("test", "입출력 오류 - 서버에서 주소변환시 에러 발생");
                }
                if (list != null) {
                    if (list.size() == 0) {
                        Toast.makeText(getApplicationContext(), "해당되는 주소정보는 없습니다", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), list.get(0).getPostalCode(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });*/


    }

    //asyncTask 스레드 사용해서 json받아오기기
    private class AsyncTaskThread extends AsyncTask<LatLng,Void, String> {


        @Override
        protected String doInBackground(LatLng...latLngs) {

            String strCoord = String.valueOf(latLngs[0].longitude) + "," + String.valueOf(latLngs[0].latitude);
            StringBuilder sb = new StringBuilder();

            StringBuilder urlBuilder = new StringBuilder("https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&coords=" +strCoord+ "&sourcecrs=epsg:4326&output=json&orders=addr");

            try {
                URL url = new URL(urlBuilder.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-type", "application/json");
                conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", "g7am17sezv");
                conn.setRequestProperty("X-NCP-APIGW-API-KEY", "Poxz4ESNyh5zFZa4bnDJTczoP1FRmhVALjShWYvc");
                InputStream contentStream = conn.getInputStream();

                BufferedReader rd;
                if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300){
                    rd = new BufferedReader((new InputStreamReader(conn.getInputStream())));
                }else{
                    rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }
                String line;
                while((line = rd.readLine()) != null){
                    sb.append(line);
                }
                rd.close();
                conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }
    }

   /* @Override
    protected void onPostExecute(String jsonStr) {
        onPostExecute(jsonStr);

        String pnu = getPnu(jsonStr);
        if(m_callback != null) {
            m_callback.callbackMethod(pnu);
        }
    }


    private String getPnu(String jsonStr) {
        JsonParser jsonParser = new JsonParser();

        JsonObject jsonObj = (JsonObject) jsonParser.parse(jsonStr);
        JsonArray jsonArray = (JsonArray) jsonObj.get("results");
        jsonObj = (JsonObject) jsonArray.get(0);
        jsonObj = (JsonObject) jsonObj.get("code");
        String pnu = jsonObj.get("id").getAsString();

        jsonObj = (JsonObject) jsonParser.parse(jsonStr);
        jsonArray = (JsonArray) jsonObj.get("results");
        jsonObj = (JsonObject) jsonArray.get(0);
        jsonObj = (JsonObject) jsonObj.get("land");
        pnu = pnu + jsonObj.get("type").getAsString();
        String number1 = jsonObj.get("number1").getAsString();
        String number2 = jsonObj.get("number2").getAsString();
        pnu = pnu + makeStringNum(number1) + makeStringNum(number2);
        return pnu;
    }

    private String makeStringNum(String number) {
        String strNum="";
        for (int i=0; i<4-number.length(); i++) {
            strNum = strNum + "0";
        }
        strNum=strNum+number;
        return strNum;
    }*/

}
