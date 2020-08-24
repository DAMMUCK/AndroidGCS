package com.kunsan.ac.kr.suagcs;

import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.MissionItemType;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.SetServo;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.util.MathUtils;


import org.droidplanner.services.android.impl.core.polygon.Polygon;

import java.util.ArrayList;
import java.util.List;

public class PolygonSpray {
    private static final String TAG = "PolygonSpray";
    public ArrayList<LatLong> polygonPointList = new ArrayList<>();
    public ArrayList<LatLong> sprayPointList = new ArrayList<>();
    protected double sprayAngle;
    private MainActivity mainActivity;

    protected PolygonSprayState polygonSprayState;

    //ab모드
    private LatLong pointA = null;
    private LatLong pointB = null;
    private double sprayDistance = 5.5f;
    private int maxSprayDistance = 50;
    private int capacity = 0;

    public static enum PolygonSprayState{
        NONE,
        STARTED,
        STORED_A,
        MAKED_SPRAYPOINT,
        UPLOADED_MISSION,
        PLAYING_MISSION,
        PAUSE_MISSION,
        FINISH_MISSION
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    PolygonSpray(MainActivity activity) {
        this.mainActivity = activity;
        //manageOverlays = mainActivity.myDrone.getManageOverlays();
    }

    public void addPolygonPoint(LatLong latLong){

    }

    // 다각형 방제에서 방제영역을 수정하면 호출되는 메소드로 방제영역과 경로를 다시 그려 준다.
    public void modifyPolygonPoint() {

    }

    /**
     * Set 1 for rotateAmount for now, Increase if want more rotate.
     * @param rotateAmount : ClockWise+, CounterClock-
     */
    public void rotatePath(double rotateAmount) {

    }

    public void makeGrid() throws Exception {

    }

    private Polygon makePoly() {
        Polygon poly = new Polygon();
        List<LatLong> latLongList = new ArrayList<>();
        for(LatLong latLong : polygonPointList) {
            latLongList.add(latLong);
        }
        poly.addPoints(latLongList);
        return poly;
    }

    private void makeMission() {

    }

    public void resetPolygonSpray() {

    }
    protected void pauseProcess() {

    }
    public int getSprayPointCnt() {
        int cnt =0;
        return cnt;
    }

    public PolygonSprayState getPolygonSprayState() {
        return polygonSprayState;
    }

    public void setPolygonSprayState(PolygonSprayState polygonSprayState) {
        this.polygonSprayState = polygonSprayState;
    }

    // AB방제 관련 메소드
    public void setPointA(LatLong latLong) {

    }
    public void setPointB(LatLong latLong) {


    }

    public void makeWaypoint() {

    }

    public void setSprayDistance(double sprayDistance) {

    }

    protected void processPrepareRetryMission() {

    }
}
