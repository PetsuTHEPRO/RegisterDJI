package edu.ifma.ifma_sdia.handlers;

import java.nio.ByteBuffer;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointTurnMode;
import dji.common.model.LocationCoordinate2D;
import dji.sdk.mission.MissionControl;
import edu.ifma.ifma_sdia.controllers.DataStruct;

public class Parsers {

    public static WaypointMission parseMissionData(DataStruct data){
        WaypointMission.Builder builder = new WaypointMission.Builder();
        ByteBuffer buffer = ByteBuffer.wrap(data.data);

        double lat = buffer.getDouble();
        double lng = buffer.getDouble();
        LocationCoordinate2D poi = new LocationCoordinate2D(lat, lng);
        float afs = buffer.getFloat();
        float mfs = buffer.getFloat();
        boolean eosl = buffer.get() != 0;
        WaypointMissionFinishedAction finishedAction = WaypointMissionFinishedAction.find(buffer.get());
        WaypointMissionFlightPathMode fpm = WaypointMissionFlightPathMode.find(buffer.get());
        WaypointMissionGotoWaypointMode gotoMode = WaypointMissionGotoWaypointMode.find(buffer.get());
        WaypointMissionHeadingMode headingMode = WaypointMissionHeadingMode.find(buffer.get());
        boolean gimbalRotation = buffer.get() != 0;
        int repeatTimes = buffer.get();

        builder = builder.setPointOfInterest(poi).autoFlightSpeed(afs).maxFlightSpeed(mfs)
                .setExitMissionOnRCSignalLostEnabled(eosl).finishedAction(finishedAction)
                .flightPathMode(fpm).gotoFirstWaypointMode(gotoMode).headingMode(headingMode)
                .setGimbalPitchRotationEnabled(gimbalRotation).repeatTimes(repeatTimes);

        int waypointCount = buffer.get();
        for(int i=0;i<waypointCount;i++){
            double wpLat = buffer.getDouble();
            double wpLng = buffer.getDouble();
            float wpAlt = buffer.getFloat();
            Waypoint wp = new Waypoint(wpLat, wpLng, wpAlt);
            wp.turnMode = WaypointTurnMode.find(buffer.get());
            int actionCount = buffer.get();
            for(int j=0; j<actionCount; j++){
                WaypointActionType actionType = WaypointActionType.find(buffer.get());
                int actionParam = buffer.getInt();
                WaypointAction action = new WaypointAction(actionType, actionParam);
                wp.addAction(action);
            }
            builder.addWaypoint(wp);
        }
        return builder.build();
    }
}
