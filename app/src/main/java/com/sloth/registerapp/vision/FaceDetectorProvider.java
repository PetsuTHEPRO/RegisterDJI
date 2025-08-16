package com.sloth.registerapp.vision;

import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class FaceDetectorProvider {

    private static FaceDetector faceDetector;

    public static FaceDetector getFaceDetector() {
        if (faceDetector == null) {
            FaceDetectorOptions options =
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                            //.enableTracking()
                            .setMinFaceSize(0.35f)
                            .build();

            faceDetector = FaceDetection.getClient(options);
        }
        return faceDetector;
    }
}