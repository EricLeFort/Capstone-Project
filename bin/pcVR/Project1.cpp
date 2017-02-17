// CannyStill.cpp

#include<opencv2/core/core.hpp>
#include<opencv2/highgui/highgui.hpp>
#include<opencv2/imgproc/imgproc.hpp>

#include<iostream>
#include<conio.h>           // may have to modify this line if not using Windows
#include<math.h>
#include <iomanip>


using namespace std;
using namespace cv;
using namespace cv::cuda;

///////////////////////////////////////////////////////////////////////////////////////////////////
int main() {

	cv::Mat imgOriginal;        // input image
	cv::Mat imgGrayscale;       // grayscale of input image
	cv::Mat imgBlurred;         // intermediate blured image
	cv::Mat imgCanny;           // Canny edge image
	vector<Vec3f> output;

	imgOriginal = cv::imread("image2.jpg");          // open image

	if (imgOriginal.empty()) {                                  // if unable to open image
		std::cout << "error: image not read from file\n\n";     // show error message on command line
		_getch();                                               // may have to modify this line if not using Windows
		return(0);                                              // and exit program
	}

	cv::cvtColor(imgOriginal, imgGrayscale, CV_BGR2GRAY);       // convert to grayscale

	cv::GaussianBlur(imgGrayscale,          // input image
		imgBlurred,                         // output image
		cv::Size(5, 5),                     // smoothing window width and height in pixels
		1.5);                               // sigma value, determines how much the image will be blurred
	
	
	cv::HoughCircles(imgGrayscale, output, CV_HOUGH_GRADIENT, 2, imgGrayscale.rows / 65, 70, 70, 70, 80);
	
	int balls[16][6];//stored as x,y,B,G,R,#ofwhitepixels.
	

	for (size_t i = 0; i < output.size(); i++)
	{
		
		Point center(cvRound(output[i][0]), cvRound(output[i][1])); \
		cv::Vec3b colour(0,0,0);
		colour = imgOriginal.at<cv::Vec3b>(cvRound(output[i][0]), cvRound(output[i][1]));
		cout <<colour[0] << endl;
		int radius = cvRound(output[i][2]);

		Vec3b intensity = imgOriginal.at<Vec3b>(100, 100);
		uchar blue = intensity.val[0];
		uchar green = intensity.val[1];
		uchar red = intensity.val[2];
		cout << blue << "pleasefuck" << endl;

		
		//Vec3b colour = imgOriginal.at<Vec3b>(cvRound(output[i][0]), cvRound(output[i][1]));
		int deciderarray[3];
		int tempwhitepix = 0;
		int tempnotwhite = 0;
		colour = imgOriginal.at<cv::Vec3b>(cvRound(output[i][0]), cvRound(output[i][1]));
		
		cout << colour.val[1] << endl;
		for (size_t j = 0; j < 10; j++) {
			/*+(4*j*pow(-1, j))*/
			colour = imgOriginal.at<cv::Vec3b>(cvRound(output[i][0] + (4 * j*pow(-1, j))), cvRound(output[i][1]));
			
			cout << colour.val[1] << endl;
			if (colour[0] > 180 && colour[1] > 180 && colour[2] > 180) {
				tempwhitepix = tempwhitepix + 1;
			}
			else {
				
				deciderarray[0] = deciderarray[0] + colour[0];
				deciderarray[1] = deciderarray[1] + colour[1];
				deciderarray[2] = deciderarray[2] + colour[2];
				tempnotwhite = tempnotwhite + 1;
			}
		}

		for (size_t k = 0; k < 10; k++) {
			colour = imgOriginal.at<cv::Vec3b>(cvRound(output[i][0]), cvRound(output[i][1]) + (4 * k*pow(-1, k)));
			cout << colour << endl;

			//cout << colour; 
			//cout << (colour[0] > 180 && colour[1] > 180 && colour[2] > 180) << tempwhitepix<<endl;
			if (colour[0] > 180 && colour[1] > 180 && colour[2] > 180) {
				tempwhitepix = tempwhitepix + 1;
			}
			else {
				deciderarray[0] = deciderarray[0] + colour.val[0];
				deciderarray[1] = deciderarray[1] + colour.val[1];
				deciderarray[2] = deciderarray[2] + colour.val[2];
				tempnotwhite = tempnotwhite + 1;
			}
		}
		balls[i][0] = cvRound(output[i][0]);
		balls[i][1] = cvRound(output[i][1]);
		balls[i][2] = cvRound(deciderarray[0] / tempnotwhite);
		balls[i][3] = cvRound(deciderarray[1] / tempnotwhite);
		balls[i][4] = cvRound(deciderarray[2] / tempnotwhite);
		balls[i][5] = tempwhitepix;

		//cout << "white"<<tempwhitepix<< i;
		//cout << "not"<<tempnotwhite<<i;
		cout << balls[i][2]<<"|2|"<< balls[i][3] << "|3|"<< balls[i][4] << "|4|"<<endl;






		// draw the circle center
		circle(imgOriginal, center, 3, Scalar(0, 255, 0), -1, 8, 0);
		// draw the circle outline
		circle(imgOriginal, center, radius, Scalar(0, 0, 255), 3, 8, 0);
	}

	

	cv::namedWindow("circles", CV_WINDOW_NORMAL);
	cv::imshow("circles", imgOriginal);
	//cout << output.size();



	/*
	cv::Canny(imgBlurred,           // input image
		imgCanny,                   // output image
		100,                        // low threshold
		200);                       // high threshold

									// declare windows
	cv::namedWindow("imgOriginal", CV_WINDOW_NORMAL);     // note: you can use CV_WINDOW_NORMAL which allows resizing the window
	cv::namedWindow("imgCanny", CV_WINDOW_NORMAL);        // or CV_WINDOW_AUTOSIZE for a fixed size window matching the resolution of the image
															// CV_WINDOW_AUTOSIZE is the default
	cv::imshow("imgOriginal", imgOriginal);     // show windows
	cv::imshow("imgCanny", imgCanny);
	*/
	cv::waitKey(0);                 // hold windows open until user presses a key
	
	return(0);
}



