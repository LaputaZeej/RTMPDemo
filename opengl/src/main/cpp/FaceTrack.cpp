//
// Created by Administrator on 2020/3/2.
//

#include "FaceTrack.h"


FaceTrack::FaceTrack(const char *faceModel, const char *landmarkerModel) {
    Ptr<CascadeDetectorAdapter> mainDetector = makePtr<CascadeDetectorAdapter>(
            makePtr<CascadeClassifier>(faceModel));
    LOGI("111111111111111111   ");
    Ptr<CascadeDetectorAdapter> trackingDetector = makePtr<CascadeDetectorAdapter>(
            makePtr<CascadeClassifier>(faceModel));
    LOGI("222222222222222222   ");
    //跟踪器
    DetectionBasedTracker::Parameters DetectorParams;
    tracker = new DetectionBasedTracker(
            DetectionBasedTracker(mainDetector, trackingDetector,
                                  DetectorParams));
    LOGI("333333333333333333   ");
    if (!tracker) {
        return;
    }
    ModelSetting::Device device = seeta::ModelSetting::CPU;
    int id = 0;
    ModelSetting FD_model(landmarkerModel, device, id);
    LOGI("444444444444444444   ");
    landmarker = new FaceLandmarker(FD_model);
}

FaceTrack::~FaceTrack() {
    delete tracker;
    delete landmarker;
}

void FaceTrack::stop() {
    tracker->stop();
}

void FaceTrack::run() {
    tracker->run();
}

void FaceTrack::process(Mat src, cv::Rect &face, std::vector<SeetaPointF> &points) {
    // 定位人脸

    if (!tracker) {
        LOGI("process tracker is null  ");
        return;
    }
    tracker->process(src);
    std::vector<cv::Rect> faces;
    tracker->getObjects(faces);
    LOGI("process size %d", faces.size());
    // 定位眼睛
    if (faces.size() != 0) {

        face = faces[0];
        seeta::ImageData simage(src.data, src.cols, src.rows, src.channels());
        seeta::Rect rect(face.x, face.y, face.width, face.height);
        points = landmarker->mark(simage, rect);
    }


}
