package com.kunsan.ac.kr.suagcs;
import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.service.controls.Control;
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
import android.widget.LinearLayout;
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
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Home;
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
import java.util.Arrays;
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

    //폴리라인 그리기 위해 내 위치들 저장하는 리스트
    private List<LatLng> guideCoords = new ArrayList<>();
    private List<LatLng> abCoords = new ArrayList<>();
    private List<LatLng> polygonCoords = new ArrayList<>();

    private final Handler handler = new Handler();

    //ui셋팅변수
    private UiSettings uiSettings;

    //내 기체 위치
    private LatLong vehiclePosition;

    //맵 폴리라인
    private PolylineOverlay polyline = new PolylineOverlay();

    //내 위치 마커
    private Marker myLocation = new Marker();

    //가이드 모드에 사용되는 변수
    private Marker mMarkerGuide = new Marker();
    private OverlayImage guideIcon = OverlayImage.fromResource(R.drawable.marker_guide);

    //json 리턴값 저장할 변수
    private String result="";

    //스피너 클릭했는지 체크하는 변수
    private boolean lockCheck = true;
    private boolean cadastralCheck = true;

    //버튼들 정의
    Button armBtn, lockBtn, mapTypeBtn, cadastralOffBtn, clearBtn, connectBtn;
    Button basicMap, terrainMap, satelliteMap;
    Button missionBtn, abBtn, cancelBtn, polygonBtn;

    //이륙고도 버튼
    Button takeOffAltitudeBtn;

    // up/down버튼
    Button altitudeUpBtn;
    Button altitudeDownBtn;

    //layout 변수
    TableLayout layout, missionLayout;
    LinearLayout altitudeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //맵 실행 되기 전
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

        // 네이버 로고 위치 변경
        uiSettings = naverMap.getUiSettings();
        uiSettings.setLogoMargin(2080, 0, 0, 925);

        // 이륙고도 표시
        ShowTakeOffAltitude();


        Log.e("mylog","컨트롤 버튼 들어가기전");
        // UI상 버튼 제어
        initButtons();

        //지도를 롱클릿 했을때 가이드 모드로 변경
        myMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                Log.e("my_log","롱클릭 시 함수 들어왔다");
                    mMarkerGuide.setPosition(latLng);
                    mMarkerGuide.setMap(myMap);
                    mMarkerGuide.setIcon(guideIcon);
                    guideMode(latLng);
            }
        });

    }


    //상태바 제거
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

    //==================================================
    //로이터 모드로 변환
    private void changeToLoiter(){
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER,new SimpleCommandListener(){
            @Override
            public void onSuccess(){
                alertUser("로이터 모드로 변경 성공");
            }

            @Override
            public void onError(int executionError){
                alertUser("로이터 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout(){
                alertUser("로이터 모드 변경 실패");
            }
        });

    }

    //*********************************가이드 모드 함수*********************
    private void guideMode(@NonNull final LatLng point){
        Log.d("my_log","dialogsimple함수 들어왔다");
        LatLong latlong = new LatLong(point.latitude,point.longitude);
        Log.d("my_log","latlong : "+latlong);


        AlertDialog.Builder guideMode_builder = new AlertDialog.Builder(this);
        guideMode_builder.setTitle("가이드 모드")
                .setMessage("가이드 모드로 변경합니다.\n목적지로 이동합니다.")
                .setCancelable(false)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       guide(latlong);
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        guideMode_builder.show();
    }

    private void guide(@NonNull LatLong latlong){
        VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED,
                new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        Log.e("my_log","가이드 모드 실행됐다");
                        ControlApi.getApi(drone).goTo(latlong, true, null);
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
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("드론 연결됨");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("드론 연결해제됨");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.HOME_UPDATED:
                //updateDistanceFromHome();
                break;
            case AttributeEvent.BATTERY_UPDATED:
                BatteryUpdate();
                break;
            case AttributeEvent.GPS_COUNT:
                ShowSatelliteCount();
                break;
            case AttributeEvent.ATTITUDE_UPDATED:
                UpdateYaw();
                break;
            case AttributeEvent.GPS_POSITION:
                updateMyState();
                break;
            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    //UI Events
    //=================================================

    public void onFlightModeSelected(View view) {
        deleteStatusBar();
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser(vehicleMode+" change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }
    //################################# UI updating ###############################

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
    }


    //update 연결버튼
    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.connectBtn);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }

    //update arm버튼
    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.arm);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    //고도 업데이트
    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValue);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    //속도 업데이트
    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValue);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    //내 위치 업데이트
    protected  void updateMyState(){
        Log.d("my_location","내 위치표시 함수 들어왔당");

        //기체의 gps값 받아오기
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        vehiclePosition = droneGps.getPosition();

        Log.d("myLog", "vehiclePosition : " + vehiclePosition);



        if(vehiclePosition == null){
                Log.d("gpsNull", "gps가 아직 안잡혀서 널값들어갔다. 다시");
        } else {
            //기체의 gps값으로 마커위치 지정하고 이미지 입히기
            myLocation.setIcon(OverlayImage.fromResource(R.drawable.marker_icon));
            myLocation.setWidth(20);
            myLocation.setHeight(100);
            myLocation.setPosition(new LatLng(vehiclePosition.getLatitude(),vehiclePosition.getLongitude()));


            //마커가 카메라 중심에 있게 움직이게 하기
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(vehiclePosition.getLatitude(),vehiclePosition.getLongitude()));
            myMap.moveCamera(cameraUpdate);

            //yaw값에 따라 마커도 회전하기
            Attitude attitude = this.drone.getAttribute(AttributeType.ATTITUDE);
            float yaw = (float)attitude.getYaw();
            myLocation.setAngle(yaw);
            myLocation.setMap(myMap);
        }
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        if(vehicleMode == VehicleMode.COPTER_GUIDED){
            guideModeState();
            changeGoal();
        }
        if(checkGoal()==true){
            changeToLoiter();
            mMarkerGuide.setMap(null);
        }
    }

    //가이드 모드일때
    private void guideModeState(){
            if(vehiclePosition == null){
                Log.d("gpsNull", "gps가 아직 안잡혀서 널값들어갔다. 다시");
            } else {
                try {
                    //기체 위치를 gps저장하는 리스트에 저장하기
                    LatLng gps = new LatLng(vehiclePosition.getLatitude(), vehiclePosition.getLongitude());
                    guideCoords.add(gps);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("gps", guideCoords.toString());
                }

                //기체가 지나갔던 길을 폴리라인으로 그려주기
                polyline.setCoords(guideCoords);
                polyline.setColor(Color.BLUE);
                polyline.setMap(myMap);
            }

    }

    //가이드 모드일때 목표지점을 바꾸면 목표지점 바꿔서 다시 시작하기
    private void changeGoal(){
            myMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                    //목적지 바꾸기
                    alertUser("목적지를 변경합니다");
                    mMarkerGuide.setPosition(latLng);
                    mMarkerGuide.setMap(myMap);
                    mMarkerGuide.setIcon(guideIcon);
                    LatLong latlong = new LatLong(latLng.latitude , latLng.longitude);
                    guide(latlong);
                }
            });
    }


    //목표지점에 도달했는지 체크
    private boolean checkGoal(){
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        LatLng goalPosition = mMarkerGuide.getPosition();
        if(vehiclePosition != null){
            try {
                LatLng gps = new LatLng(vehiclePosition.getLatitude(),vehiclePosition.getLongitude());
                //가이드 모드일때 맵에 목적지를 클릭하면
                if(vehicleMode == VehicleMode.COPTER_GUIDED){
                    if(gps == goalPosition){
                        return true;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                Log.d("mylog","gps가 안잡혔어");
            }
        }
        return false;
    }

   /* protected void updateDistanceFromHome() {
        TextView distanceTextView = (TextView) findViewById(R.id.distanceValue);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getAltitude();
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }

        distanceTextView.setText(String.format("%3.1f", distanceFromHome) + "m");
    }*/



    //################################# UI ########################################

    private void ShowSatelliteCount(){
        // 잡히는 GPS 개수
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        TextView textView_gps = (TextView) findViewById(R.id.satelliteValue);
        textView_gps.setText(droneGps.getSatellitesCount()+"개");
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
        TextView textView_yaw = (TextView) findViewById(R.id.yawValue);
        textView_yaw.setText((int) yaw + "deg");
    }

    //배터리 업데이트
    private void BatteryUpdate(){
        TextView textView_Vol = (TextView) findViewById(R.id.voltageValueTextView);
        Battery battery = this.drone.getAttribute(AttributeType.BATTERY);
        double batteryVoltage = Math.round(battery.getBatteryVoltage() * 10) / 10.0;
        textView_Vol.setText(batteryVoltage + "V");
        Log.d("Position8", "Battery : "+ batteryVoltage);
    }

    //###################### 이륙고도 클릭 #######################################

    public int getTakeOffAltitude(){
        return this.takeOffAltitude;
    }

    public void setTakeOffAltitude(int Altitude){
        this.takeOffAltitude = Altitude;
    }



    //############################# 버튼 컨트롤 ##############################
    private void initButtons(){
        Log.e("mylog","컨트롤 버튼 들어갔다");
        //기본 ui 4개 버튼
        armBtn = (Button)findViewById(R.id.arm);
        lockBtn = (Button)findViewById(R.id.lock);
        mapTypeBtn = (Button)findViewById(R.id.mapType);
        cadastralOffBtn = (Button) findViewById(R.id.cadastralOff);
        clearBtn = (Button) findViewById(R.id.clear);
        connectBtn = (Button) findViewById(R.id.connectBtn);

        //지도타입 버튼 변수
        basicMap = (Button) findViewById(R.id.basicMap);
        terrainMap = (Button) findViewById(R.id.topoMap);
        satelliteMap = (Button) findViewById(R.id.satelliteMap);

        //이륙고도 버튼
        takeOffAltitudeBtn = (Button) findViewById(R.id.takeOffAltitudeBtn);
        // up/down버튼
        altitudeUpBtn = (Button) findViewById(R.id.altitudeUp);
        altitudeDownBtn = (Button) findViewById(R.id.altitudeDown);

        //미션버튼
        missionBtn = (Button) findViewById(R.id.missionBtn);
        abBtn = (Button) findViewById(R.id.ab);
        polygonBtn = (Button)findViewById(R.id.polygon);
        cancelBtn = (Button) findViewById(R.id.cancel);

        //*************************** 기본 ui 버튼 제어

        //LinearLayout 변수
        layout = (TableLayout) findViewById(R.id.mapTypeLayout);
        altitudeLayout = (LinearLayout) findViewById(R.id.altitudeState);
        missionLayout = (TableLayout) findViewById(R.id.missionTableLayout);

        /*앱 잠금 버튼 클릭 시 이벤트
        앱 잠금 상태 :맵을 드래그하면 맵이 자동으로 기체의 위치가 가운데인 생태로 조정된다. -> 드론 gps 값으로 카메라 고정
        앱 잠금 해제 상태 : 맵을 드래그하면 맵이 이동된 상태로 그대로 유지가 됩니다.*/
        lockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(lockCheck){
                    Toast.makeText(getApplicationContext(), "앱 잠금", Toast.LENGTH_SHORT).show();
                    uiSettings.setScrollGesturesEnabled(false);
                    lockCheck = false;
                }else{
                    Toast.makeText(getApplicationContext(), "앱 잠금 해제", Toast.LENGTH_SHORT).show();
                    uiSettings.setScrollGesturesEnabled(true);
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

        //이륙버튼
        takeOffAltitudeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(altitudeLayout.getVisibility() == View.INVISIBLE){
                    altitudeLayout.setVisibility(View.VISIBLE);
                }else{
                    altitudeLayout.setVisibility(View.INVISIBLE);
                }
            }
        });

        //고도 상승버튼
        altitudeUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAltitudeUp();
                updateAltitude();
            }
        });

        //고도 하강버튼
        altitudeDownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAltitudeDown();
                updateAltitude();
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
                if(mMarkerGuide != null){mMarkerGuide.setMap(null);}
                if(polyline != null){polyline.setMap(null);}
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_STABILIZE, new SimpleCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("stabilize 모드로 변경 성공");
                    }

                    @Override
                    public void onError(int executionError) {
                        alertUser("stabilize 모드로 변경 실패 : " + executionError);
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("stabilize 모드로 변경 실패.");
                    }
                });
                if(guideCoords != null){
                    for(LatLng latlng:guideCoords){
                        guideCoords.remove(latlng);
                    }
                }
                if(abCoords != null){
                    for(LatLng latlng:abCoords){
                        abCoords.remove(latlng);
                    }
                }
                if(polygonCoords != null){
                    for(LatLng latlng:polygonCoords){
                        polygonCoords.remove(latlng);
                    }
                }
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

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectButton();
            }
        });

        armBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                armButton();
            }
        });

        //미션버튼 이벤트
        missionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(missionLayout.getVisibility() ==  View.INVISIBLE){
                    missionLayout.setVisibility(View.VISIBLE);
                }else{
                    missionLayout.setVisibility(View.INVISIBLE);
                }
            }
        });

        abBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        polygonBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }


    //*************************** 버튼 컨트롤에 쓰이는 함수 ***********************************

    //연결버튼
    private void connectButton(){
        if(this.drone.isConnected()){
            Log.e("mylog","버튼 들어갔는데 여기는 isConnected");
            this.drone.disconnect();
        }else{
            Log.e("mylog","버튼 들어갔는데 여기는 Connected");
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }
    }

    //arm버튼
    private void armButton(){
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            landButton();
        } else if (vehicleState.isArmed()) {
            // Take off
            takeOffButton();
        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed
            AlertDialog.Builder arm_builder = new AlertDialog.Builder(this);
            arm_builder.setTitle("모터 시동")
                    .setMessage("모터를 가동합니다.\n모터가 고속으로 회전합니다.")
                    .setCancelable(false)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //확인 클릭시
                            VehicleApi.getApi(drone).arm(true, false, new SimpleCommandListener() {
                                @Override
                                public void onError(int executionError) {
                                    alertUser("Unable to arm vehicle.");
                                }

                                @Override
                                public void onTimeout() {
                                    alertUser("Arming operation timed out.");
                                }
                            });
                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            arm_builder.show();
        }
    }

    //land
    private void landButton(){
        AlertDialog.Builder land_builder = new AlertDialog.Builder(this);
        land_builder.setTitle("착륙확인")
                .setMessage("기체를 착륙합니다.\n안전거리를 유지하세요.")
                .setCancelable(false)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //land클릭시
                        VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                            @Override
                            public void onError(int executionError) {
                                alertUser("Unable to land the vehicle.");
                            }

                            @Override
                            public void onTimeout() {
                                alertUser("Unable to land the vehicle.");
                            }
                        });
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        land_builder.show();
    }

    //takeoff
    private void takeOffButton(){
        AlertDialog.Builder takeOff_builder = new AlertDialog.Builder(this);
        takeOff_builder.setTitle("기체상승 확인")
                .setMessage("지정한 이륙고도까지 기체가 상승합니다.\n안전거리를 유지하세요.")
                .setCancelable(false)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //takeoff클릭시
                        ControlApi.getApi(drone).takeoff(10, new AbstractCommandListener() {

                            @Override
                            public void onSuccess() {
                                alertUser("Taking off...");
                            }

                            @Override
                            public void onError(int i) {
                                alertUser("Unable to take off.");
                            }

                            @Override
                            public void onTimeout() {
                                alertUser("Unable to take off.");
                            }
                        });
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        takeOff_builder.show();
    }

    //이륙고도 up
    private void setAltitudeUp(){
        int altitude = getTakeOffAltitude();
        setTakeOffAltitude(++altitude);
    }

    //이륙고도 down
    private void setAltitudeDown(){
        int altitude = getTakeOffAltitude();
        setTakeOffAltitude(--altitude);
    }

    // ************************ 비행 모드 변경 ******************************

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
    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }
}
