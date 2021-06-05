//
// Created by Administrator on 2020/3/2.
//

#ifndef DOUYIN_FACETRACK_H
#define DOUYIN_FACETRACK_H

#include <seeta/FaceLandmarker.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/types_c.h>

#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"_opengl_c",__VA_ARGS__)

using namespace cv;
using namespace seeta;

class CascadeDetectorAdapter : public DetectionBasedTracker::IDetector {
public:
    CascadeDetectorAdapter(cv::Ptr<cv::CascadeClassifier> detector) :
            IDetector(),
            Detector(detector) {
        CV_Assert(detector);
    }

    void detect(const cv::Mat &Image, std::vector<cv::Rect> &objects) {

        Detector->detectMultiScale(Image, objects, scaleFactor, minNeighbours, 0, minObjSize,
                                   maxObjSize);

    }

    virtual ~CascadeDetectorAdapter() {

    }

private:
    CascadeDetectorAdapter();

    cv::Ptr<cv::CascadeClassifier> Detector;
};


class FaceTrack {
public:
    FaceTrack(const char *faceModel, const char *landmarkerModel);

    ~FaceTrack();

    void stop();

    void run();

    void process(Mat src, cv::Rect &face,std::vector<SeetaPointF>& points);

private:
    DetectionBasedTracker *tracker;
    FaceLandmarker *landmarker;
};


#endif //DOUYIN_FACETRACK_H
