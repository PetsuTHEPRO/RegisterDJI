package edu.ifma.ifma_sdia.controllers;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Objects;

import dji.common.error.DJIError;
import dji.common.mission.MissionState;
import dji.common.mission.waypoint.WaypointExecutionProgress;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecuteState;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.KeyListener;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineEvent;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;
import edu.ifma.ifma_sdia.handlers.BuildCodes;
import edu.ifma.ifma_sdia.handlers.Builders;
import edu.ifma.ifma_sdia.handlers.Parsers;
import edu.ifma.ifma_sdia.listeners.DataReceiverListener;
import edu.ifma.ifma_sdia.listeners.key_listeners.AircraftLocationListener;
import edu.ifma.ifma_sdia.listeners.key_listeners.GenericBoolListener;
import edu.ifma.ifma_sdia.listeners.key_listeners.GenericDoubleListener;
import edu.ifma.ifma_sdia.listeners.key_listeners.GenericFloatListener;
import edu.ifma.ifma_sdia.listeners.key_listeners.GenericIntListener;

public class MainClientController {
    private BaseClient client;
    private DataReceiverListener listener;
    private WaypointMissionOperatorListener missionEventListener;
    public MainClientController(String host, int port){
        listener = new DataReceiverListener(this);
        client = new BaseClient(host, port, listener);
        client.connect(true, true);


        // add key listeners
        KeyListener locationListener = new AircraftLocationListener(client);
        KeyListener velocityXListener = new GenericFloatListener(client, BuildCodes.VELOCITY_X);
        KeyListener velocityYListener = new GenericFloatListener(client, BuildCodes.VELOCITY_Y);
        KeyListener velocityZListener = new GenericFloatListener(client, BuildCodes.VELOCITY_Z);

        KeyListener pitchListener = new GenericDoubleListener(client, BuildCodes.ATTITUDE_PITCH);
        KeyListener yawListener = new GenericDoubleListener(client, BuildCodes.ATTITUDE_YAW);
        KeyListener rollListener = new GenericDoubleListener(client, BuildCodes.ATTITUDE_ROLL);

        KeyListener flyingListener = new GenericBoolListener(client, BuildCodes.IS_FLYING);
        KeyListener flyTimeListener = new GenericIntListener(client, BuildCodes.FLY_TIME_IN_SECONDS);

        //

        KeyManager.getInstance().addListener(FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION), locationListener);
        KeyManager.getInstance().addListener(FlightControllerKey.create(FlightControllerKey.VELOCITY_X), velocityXListener);
        KeyManager.getInstance().addListener(FlightControllerKey.create(FlightControllerKey.VELOCITY_Y), velocityYListener);
        KeyManager.getInstance().addListener(FlightControllerKey.create(FlightControllerKey.VELOCITY_Z), velocityZListener);

        KeyManager.getInstance().addListener(FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH), pitchListener);
        KeyManager.getInstance().addListener(FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW), yawListener);
        KeyManager.getInstance().addListener(FlightControllerKey.create(FlightControllerKey.ATTITUDE_ROLL), rollListener);

        KeyManager.getInstance().addListener(FlightControllerKey.create(FlightControllerKey.IS_FLYING), flyingListener);
        KeyManager.getInstance().addListener(FlightControllerKey.create(FlightControllerKey.FLY_TIME_IN_SECONDS), flyTimeListener);

        // end add key listeners


        // mission status listener
        missionEventListener = new WaypointMissionOperatorListener() {
            @Override
            public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {
                if(waypointMissionDownloadEvent.getProgress()==null){
                    Log.i("MISSION_EVENT", "[DOWNLOAD] - error during download");
                    String current_state = "DOWNLOAD_ERROR";
                    byte[] data = Builders.missionEvent((byte)Builders.CurrentMissionState.valueOf(current_state).getValue());
                    client.addSenderData(data);
                }
            }

            @Override
            public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {
                String current_state = waypointMissionUploadEvent.getCurrentState().toString();
                Log.i("MISSION_EVENT", "[UPLOAD] - STATE:"+current_state);
                byte[] data = Builders.missionEvent((byte)Builders.CurrentMissionState.valueOf(current_state).getValue());
                client.addSenderData(data);
            }

            @Override
            public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
                String current_state = waypointMissionExecutionEvent.getCurrentState().toString();
                int event_exec = -2;

                if(waypointMissionExecutionEvent.getProgress()!=null)
                    event_exec = waypointMissionExecutionEvent.getProgress().executeState.value();
                Log.i("MISSION_EVENT", "[EXECUTING] - STATE:"+current_state+" PROGRESS:"+event_exec);
                byte[] data = Builders.missionEvent((byte)Builders.CurrentMissionState.valueOf(current_state).getValue(), event_exec);
            }

            @Override
            public void onExecutionStart() {
                Log.i("MISSION_EVENT", "[EXECUTING] STARTED!");
            }

            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {
                String msg = "";
                if(djiError!=null) msg = djiError.getDescription();
                Log.i("MISSION_EVENT", "[EXECUTING] FINISH! "+msg);
            }
        };
        MissionControl.getInstance().getWaypointMissionOperator().addListener(missionEventListener);
    }
    public BaseClient getClient(){return client;}
    public void sendHeartBeat(byte[] check){
        byte[] data = Builders.heartBeat(check);
        client.addSenderData(data);
    }

    public void missionRegister(DataStruct data){
        try{
            WaypointMission mission = Parsers.parseMissionData(data);
            WaypointMissionOperator operator =  MissionControl.getInstance().getWaypointMissionOperator();
            operator.loadMission(mission);
            operator.uploadMission(djiError -> {
                boolean success = djiError == null;
                byte[] sendData = Builders.genericBoolData(BuildCodes.WAYPOINT_MISSION_UPLOAD_RESULT.value, success);
                client.addSenderData(sendData);
            });

        }catch(Exception e){
            String msg = e.getMessage();
            Log.e("[MISSION_LOAD]", msg!=null?msg:"NULL");
        }
    }
    public void missionStop(DataStruct data){
        try{

            WaypointMissionOperator operator =  MissionControl.getInstance().getWaypointMissionOperator();
            operator.stopMission(djiError -> {
                boolean success = djiError == null;
//                byte[] sendData = Builders.genericBoolData(BuildCodes.WAYPOINT_MISSION_UPLOAD_RESULT.value, success);
//                client.addSenderData(sendData);
                Log.e("[MISSION_STOP]", "result: "+success);
            });

        }catch(Exception e){
            String msg = e.getMessage();
            Log.e("[MISSION_STOP]", msg!=null?msg:"NULL");
        }
    }
    public void missionStart(DataStruct data){
        try{

            WaypointMissionOperator operator =  MissionControl.getInstance().getWaypointMissionOperator();
            operator.startMission(djiError -> {
                boolean success = djiError == null;
//                byte[] sendData = Builders.genericBoolData(BuildCodes.WAYPOINT_MISSION_UPLOAD_RESULT.value, success);
//                client.addSenderData(sendData);
                Log.e("[MISSION_START]", "result: "+success);
            });

        }catch(Exception e){
            String msg = e.getMessage();
            Log.e("[MISSION_START]", msg!=null?msg:"NULL");
        }
    }
    public void missionPause(DataStruct data){
        try{

            WaypointMissionOperator operator =  MissionControl.getInstance().getWaypointMissionOperator();
            operator.pauseMission(djiError -> {
                boolean success = djiError == null;
//                byte[] sendData = Builders.genericBoolData(BuildCodes.WAYPOINT_MISSION_UPLOAD_RESULT.value, success);
//                client.addSenderData(sendData);
                Log.e("[MISSION_PAUSE]", "result: "+success);
            });

        }catch(Exception e){
            String msg = e.getMessage();
            Log.e("[MISSION_PAUSE]", msg!=null?msg:"NULL");
        }
    }
    public void missionResume(DataStruct data){
        try{

            WaypointMissionOperator operator =  MissionControl.getInstance().getWaypointMissionOperator();
            operator.resumeMission(djiError -> {
                boolean success = djiError == null;
//                byte[] sendData = Builders.genericBoolData(BuildCodes.WAYPOINT_MISSION_UPLOAD_RESULT.value, success);
//                client.addSenderData(sendData);
                Log.e("[MISSION_RESUME]", "result: "+success);
            });

        }catch(Exception e){
            String msg = e.getMessage();
            Log.e("[MISSION_RESUME]", msg!=null?msg:"NULL");
        }
    }

    public void handleData(DataStruct data){
        byte codeValue = data.data[0];
        switch (codeValue){
            case 0x01: // heart beat
                Log.i("HANDLE_DATA", "HEART_BEAT: "+ Arrays.toString(data.data));
                sendHeartBeat(Arrays.copyOfRange(data.data, 1, 5));
                break;
            case 0x10: // waypoint mission register->upload
                missionRegister(data);
                break;
            case 0x11: // waypoint mission start
                missionStart(data);
                break;
            case 0x12: // waypoint mission stop
                missionStop(data);
                break;
            case 0x16: // waypoint mission pause
                missionPause(data);
                break;
            case 0x17: // waypoint mission resume
                missionResume(data);
                break;
        }
    }
}
