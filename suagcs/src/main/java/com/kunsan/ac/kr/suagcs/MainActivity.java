package com.kunsan.ac.kr.suagcs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener {
  private static final String TAG = MainActivity.class.getSimpleName();

  private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
  private FusedLocationSource locationSource;

  MapFragment mNaverMapFragment = null;

  private Drone drone;

   //네이버 맵
    NaverMap myMap;

    List<Marker> markers = new ArrayList<>();
    List<LatLng> coords = new ArrayList<>();                // 폴리라인
    ArrayList<String> recycler_list = new ArrayList<>();    // 리사이클러뷰
    List<LocalTime> recycler_time = new ArrayList<>();      // 리사이클러뷰 시간
    List<Marker> Auto_Marker = new ArrayList<>();           // 간격감시 마커
    List<LatLng> PolygonLatLng = new ArrayList<>();         // 간격감시 폴리곤
    List<LatLng> Auto_Polyline = new ArrayList<>();         // 간격감시 폴리라인


    Marker marker_goal = new Marker(); // Guided 모드 마커

    PolylineOverlay polyline = new PolylineOverlay();           // 마커 지나간 길
    PolygonOverlay polygon = new PolygonOverlay();              // 간격 감시 시 뒤 사각형 (하늘)
    PolylineOverlay polylinePath = new PolylineOverlay();       // 간격 감시 시 Path (하양)
    PolygonOverlay Area_polygon = new PolygonOverlay();         // 면적 감시 시 뒤 다각형 (하늘)

    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;

    private Spinner modeSelector;

    private int Marker_Count = 0;
    private int Recycler_Count = 0;
    private int takeOffAltitude = 3;
    private int Auto_Marker_Count = 0;
    public int Auto_Distance = 50;
    public int Gap_Distance = 5;
    private int Gap_Top = 0;
    private int Guided_Count = 0;

    protected double mRecentAltitude = 0;

    public int Reached_Count = 0;

    private final Handler handler = new Handler();

    //json 리턴값 저장할 변수
    private String result="";

    //스피너 클릭했는지 체크하는 변수
    private boolean lockCheck = true;
    private boolean cadastralCheck = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {//맵 실행 되기 전
        Log.i(TAG, "Start mainActivity");
        super.onCreate(savedInstanceState);
        // 소프트바 없애기
        deleteStatusBar();
        // 상태바 없애기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);


        //지도띄우기
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        // 모드 변경 스피너
        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> prent) {
                // Do nothing
            }
        });

        // 내 위치
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        mapFragment.getMapAsync(this);


    }

    //***********************************************************
    //onCreate밖

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    //지도 생성시 초기화
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.myMap = naverMap;

        // 켜지자마자 드론 연결
        ConnectionParameter params = ConnectionParameter.newUdpConnection(null);
        this.drone.connect(params);

        // 네이버 로고 위치 변경
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLogoMargin(2080, 0, 0, 925);

        // 나침반 제거
        uiSettings.setCompassEnabled(false);

        // 축척 바 제거
        uiSettings.setScaleBarEnabled(false);

        // 줌 버튼 제거
        uiSettings.setZoomControlEnabled(false);

        // 이륙고도 표시
        ShowTakeOffAltitude();

        // 초기 상태를 맵 잠금으로 설정
        uiSettings.setScrollGesturesEnabled(false);

        // UI상 버튼 제어
        ControlButton();

        // 내 위치
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.NoFollow);

        // 롱 클릭 시 경고창
        naverMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull PointF pointF, @NonNull LatLng coord) {
                LongClickWarning(pointF, coord);
            }
        });


        // 클릭 시
        naverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                final Button BtnFlightMode = (Button) findViewById(R.id.flightModeBtn);
                if (BtnFlightMode.getText().equals("일반\n모드")) {
                    // TODO : 일반모드 클릭
                } else if (BtnFlightMode.getText().equals("경로\n비행")) {
                    // TODO : 경로비행 클릭
                } else if (BtnFlightMode.getText().equals("간격\n감시")) {
                    MakeGapPolygon(latLng);
                } else if (BtnFlightMode.getText().equals("면적\n감시")) {
                    MakeAreaPolygon(latLng);
                }
            }
        });



      /*  myMap.setMapType(NaverMap.MapType.Basic);
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


        //마커 클릭시 정보창 뜨게하기
        marker1.setOnClickListener(listener);
        marker2.setOnClickListener(listener);
        marker3.setOnClickListener(listener);

*/

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

    private void deleteStatusBar() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        deleteStatusBar();
        return super.onTouchEvent(event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            //updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {

    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {

    }

    @Override
    public void onTowerConnected() {

    }

    @Override
    public void onTowerDisconnected() {

    }

    //################################# UI ########################################

    private void ShowSatelliteCount(){
        // 잡히는 GPS 개수
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        int Satellite = droneGps.getSatellitesCount();
        TextView textView_gps = (TextView) findViewById(R.id.satelliteValue);
        textView_gps.setText(Satellite);
    }

    private void ShowTakeOffAltitude(){
        final Button BtnTakeOffAltitude = (Button) findViewById(R.id.takeOffAltitudeBtn);
        BtnTakeOffAltitude.setText(getTakeOffAltitude()+"m\n이륙고도");
    }

    private void UpdateYaw(){
        //attitude 받아오기
        Attitude attitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        double yaw = attitude.getYaw();

        //yaw값을 양수로
        if((int) yaw < 0){
            yaw += 360;
        }

        // yaw 보여주기
        TextView textView_yaw = (TextView) findViewById(R.id.yaw);
        textView_yaw.setText((int) yaw + "deg");
    }

    private void BatteryUpdate(){
        TextView textView_Vol = (TextView) findViewById(R.id.voltage);
        Battery battery = this.drone.getAttribute(AttributeType.BATTERY);
        double batteryVoltage = Math.round(battery.getBatteryVoltage() * 10) / 10.0;
        textView_Vol.setText(batteryVoltage + "V");
        Log.d("Position8", "Battery : "+ batteryVoltage);
    }

    public void SetDronePosition(){
        //드론 위치 받아오기
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong dronePosition = droneGps.getPosition();

        Log.d("Position1", "droneGps : " + droneGps);
        Log.d("Position1", " dronePosition : "+dronePosition);

        //이동했던 위치 맵에서 지워주기
        if(Marker_Count -1 >= 0)
        {
            markers.get(Marker_Count - 1).setMap(null);
        }

        //마커 리스트에 추가
        markers.add(new Marker(new LatLng(dronePosition.getLatitude(), dronePosition.getLongitude())));

        //yaw에 따라 네비게이션 마커 회전
        Attitude attitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        double yaw = attitude.getYaw();
        Log.d("Position4", "yaw : "+ yaw);
        if((int) yaw < 0){
            yaw += 360;
        }
        markers.get(Marker_Count).setAngle((float) yaw);

        //마커 크기 지정
        markers.get(Marker_Count).setHeight(400);
        markers.get(Marker_Count).setWidth(80);

        //마커 아이콘 지정
        markers.get(Marker_Count).setIcon(OverlayImage.fromResource(R.drawable.marker_icon));

        //마커 위치를 중심점으로 지정
        markers.get(Marker_Count).setAnchor(new PointF(0.5F, 0.9F));

        //마커 띄우기
        markers.get(Marker_Count).setMap(myMap);

        //카메라 위치 설정
        Button mapMoveLockBtn = (Button) findViewById(R.id.lock);
        String text = (String) mapMoveLockBtn.getText();

        if(text.equals("맵 잠금")){
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(dronePosition.getLatitude(), dronePosition.getLongitude()));
            myMap.moveCamera(cameraUpdate);
        }

        //지나간 길 Polyline
        Collections.addAll(coords, markers.get(Marker_Count).getPosition());
        polyline.setCoords(coords);

        //선 예쁘게 설정
        polyline.setWidth(15);
        polyline.setCapType(PolylineOverlay.LineCap.Round);
        polyline.setJoinType(PolylineOverlay.LineJoin.Round);
        polyline.setColor(Color.GREEN);

        polyline.setMap(myMap);

        Log.d("Position3", "coords.size() : " + coords.size());
        Log.d("Position3", "markers.size() : " + markers.size());


        //가이드 모드일 때 지정된 좌표와 드론 사이의 거리 측정
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        if(vehicleMode == VehicleMode.COPTER_GUIDED){
            LatLng droneLatLng = new LatLng(markers.get(Marker_Count).getPosition().latitude, markers.get(Marker_Count).getPosition().longitude);
            LatLng goalLatLng = new LatLng(marker_goal.getPosition().latitude, marker_goal.getPosition().longitude);

            double distance = droneLatLng.distanceTo(goalLatLng);

            Log.d("Position9", "distance : " + distance);

            if(distance < 1.0){
                if(Guided_Count == 0){
                    if(Guided_Count == 0){
                        alertUser("목적지에 도착하였습니다");
                        marker_goal.setMap(myMap);
                        Guided_Count = 1;
                    }
                }
            }

            ShowSatelliteCount();
            Marker_Count++;
        }
    }

    private void AltitudeUpdate(){
        Altitude currentAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        mRecentAltitude = currentAltitude.getRelativeAltitude();
        double DoubleAltitude = (double) Math.round(mRecentAltitude = 10) / 10.0;

        TextView textView = (TextView) findViewById(R.id.altitude);
        Altitude altitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        int intAltitude = (int) Math.round(altitude.getAltitude());

        textView.setText(DoubleAltitude + "m");
        Log.d("Position7", "Altitude : "+ DoubleAltitude);
    }

    private void SpeedUpdate(){
        TextView textView = (TextView) findViewById(R.id.speed);
        Speed speed = this.drone.getAttribute(AttributeType.SPEED);
        int doubleSpeed = (int) Math.round(speed.getGroundSpeed());
        textView.setText(doubleSpeed + "m/s");
        Log.d("Position6", "Speed : "+ this.drone.getAttribute(AttributeType.SPEED));
    }

    public void onFlightModeSelected(View view){
        final VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("비행 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onError(int executionError) {
                alertUser("비행 모드 변경 실패 : " + exectionError);
            }

            @Override
            public void onTimeout() {
                alertUser("비행 모드 변경 시간 초과.");
            }
        });
    }

    //###################### 이륙고도 클릭 #######################################

    public int getTakeOffAltitude(){
        return this.takeOffAltitude;
    }

    public void setTakeOffAltitude(int Altitude){
        this.takeOffAltitude = Altitude;
    }

    //########################### 롱 클릭 시 Guidede 모드로 변경 ####################

    private void LongClickWarning(@NonNull PointF pointF , @NonNull final LatLng coord){
        Button flightModeBtn = (Button) findViewById(R.id.flightModeBtn);
        if(flightModeBtn.getText().equals("일반\n모드")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("가이드 모드");
            builder.setMessage("클릭한 지점으로 이도하게 됩니다. 이동하시겠습니까?");
            builder.setPositiveButton("예", new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which){
                    //도착지 마커 생성
                    marker_goal.setMap(null);
                    marker_goal.setPosition(new LatLng(coord.latitude,coord.longitude));
                    marker_goal.setIcon(OverlayImage.fromResource(R.drawable.final_flag));
                    marker_goal.setWidth(70);
                    marker_goal.setHeight(70);
                    marker_goal.setMap(myMap);

                    Guided_Count = 0;

                    //Guided 모드로 변환
                    ChangeToGuideMode();

                    GotoTartget();
                }
            });
            builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        }
    }

    private void GotoTartget() {
        ControlApi.getApi(this.drone).goTo(
                new LatLong(marker_goal.getPosition().latitude, marker_goal.getPosition().longitude),
                true, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("목적지로 향합니다.");
                    }

                    @Override
                    public void onError(int executionError) {
                        alertUser("이동 할 수 없습니다 : " + executionError);
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("이동 할 수 없습니다.");
                    }
                });
    }


    //############################# 버튼 컨트롤 ##############################
    public void ControlButton(){

        //기본 ui 4개 버튼
        final Button armBtn = (Button)findViewById(R.id.arm);
        final Button lockBtn = (Button)findViewById(R.id.lock);
        final Button mapTypeBtn = (Button)findViewById(R.id.mapType);
        final Button cadastralOffBtn = (Button) findViewById(R.id.cadastralOff);
        final Button clearBtn = (Button) findViewById(R.id.clear);

        //지도타입 버튼 변수
        final Button basicMap = (Button) findViewById(R.id.basicMap);
        final Button terrainMap = (Button) findViewById(R.id.topoMap);
        final Button satelliteMap = (Button) findViewById(R.id.satelliteMap);

        //이륙고도 버튼
        final Button takeOffAltitudeBtn = (Button) findViewById(R.id.takeOffAltitudeBtn);
        // up/down버튼
        final Button altitudeUpBtn = (Button) findViewById(R.id.altitudeUp);
        final Button altitudeDownBtn = (Button) findViewById(R.id.altitudeDown);

        //비행모드 버튼
        final Button flightModeBtn = (Button) findViewById(R.id.flightModeBtn);

        final Button flightMode_Basic = (Button)findViewById(R.id.FlyMode_Basic);
        final Button flightMode_Path = (Button) findViewById(R.id.FlyMode_Path);
        final Button flightMode_Gap = (Button) findViewById(R.id.FlyMode_Gap);
        final Button flightMode_Area = (Button) findViewById(R.id.FlyMode_Area);

        final UiSettings uiSettings = myMap.getUiSettings();

        //*************************** 기본 ui 버튼 제어

        //LinearLayout 변수
        TableLayout layout = (TableLayout) findViewById(R.id.mapTypeLayout);

        armBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        /*앱 잠금 버튼 클릭 시 이벤트
        앱 잠금 상태 :맵을 드래그하면 맵이 자동으로 기체의 위치가 가운데인 생태로 조정된다. -> 드론 gps 값으로 카메라 고정
        앱 잠금 해제 상태 : 맵을 드래그하면 맵이 이동된 상태로 그대로 유지가 됩니다.*/

        lockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(lockCheck){
                    Toast.makeText(getApplicationContext(), "앱 잠금", Toast.LENGTH_SHORT).show();
                    lockCheck = false;
                }else{
                    Toast.makeText(getApplicationContext(), "앱 잠금 해제", Toast.LENGTH_SHORT).show();
                    lockCheck = true;
                }
            }
        });

        //지도타입버튼 클릭시 지도유형 보이도록 이벤트
        mapTypeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(layout.getVisibility() == View.INVISIBLE){
                    layout.setVisibility(View.VISIBLE);
                }else{
                    layout.setVisibility(View.INVISIBLE);
                }
            }
        });

        //지적도버튼 클릭 시 이벤트
        cadastralOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cadastralCheck){
                    myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                    Toast.makeText(getApplicationContext(), "지적도 on", Toast.LENGTH_SHORT).show();
                    cadastralCheck = false;
                }else{
                    myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                    Toast.makeText(getApplicationContext(), "지적도 off", Toast.LENGTH_SHORT).show();
                    cadastralCheck = true;
                }
            }
        });

        //초기화버튼 클릭 시 이벤트
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //지도 초기화
                Toast.makeText(getApplicationContext(), "초기화", Toast.LENGTH_SHORT).show();
            }
        });


        //지도유형 버튼들
        basicMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Basic);
                Toast.makeText(getApplicationContext(), "기본지도띄우기", Toast.LENGTH_SHORT).show();
            }
        });

        terrainMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Terrain);
                Toast.makeText(getApplicationContext(), "지형도띄우기", Toast.LENGTH_SHORT).show();
            }
        });

        satelliteMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Satellite);
                Toast.makeText(getApplicationContext(), "위성지도 띄우기", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ************************ 비행 모드 변경 ******************************

    private void ChangeToLoiterMode() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Loiter 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Loiter 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Loiter 모드 변경 실패");
            }
        });
    }

    private void ChangeToAutoMode() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_AUTO, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Auto 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Auto 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Auto 모드 변경 실패.");
            }
        });
    }

    private void ChangeToGuidedMode() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("가이드 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("가이드 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("가이드 모드 변경 실패.");
            }
        });
    }


    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }



    //*********************************************************************************
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

   //***************************알림창*********************************
    private void alertUser(String message){
        //5개 이상 삭제
        if (recycler_list.size() > 3){
            recycler_list.remove(Recycler_Count);
        }
        LocalTime localTime = LocalTime.now();
        recycler_list.add(String.format("  ☆ " + message));
        recycler_time.add(localTime);

        //리사이클러뷰에 LinearLayoutManager 객체 지정.
        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //리사이클러뷰에 SimpleAdapter 객체 지정.
        SimpleTextAdapter adapter = new SimpleTextAdapter(recycler_list);
        recyclerView.setAdapter(adapter);
    }
}
