// CannyStill.cpp

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <iostream>
#include <conio.h>
#include <math.h>
#include <iomanip>
#include <fstream>
#include <chrono>
#include <thread>

using namespace std;
using namespace cv;
using namespace cv::cuda;

//Stored as x,y,B,G,R,#ofwhitepixels,ballId.
//The final lines are stored as botthoriz. right vert. top hori, left verti //(xinit,yinit,xfin,yfin)
bool ballinfo(int balls[16][7], Vec4i lines[4], int N){
	ofstream myfile;
	int finalballs[16][7];
	int zero[2] = {lines[3][0], lines[0][1]};
	int stripecnt = 0, solidcnt = 1, full_dx = lines[1][0] - lines[3][0], full_dy = lines[2][1] - lines[0][1];
	double finalposition[16][2], realposition[16][2] = {0};
	double MILYLEN = 1880, MILXLEN = 960, milpery = MILYLEN / full_dy, milperx = MILXLEN / full_dx;
	float OFFSETX = 0.020, OFFSETY = 0.018;

	for(int k = 0; k < 16; k++){
		if(k >= N){
			realposition[k][0] = -1; realposition[k][1] = -1;
		}else{
			realposition[k][0] = (balls[k][0] - zero[0])*(milperx);
			realposition[k][1] = (balls[k][1] - zero[1])*(milpery);
			
			if(balls[k][4] < 110 && balls[k][3] < 110 && balls[k][2] < 110){
				balls[k][6] = 8; // black ball 
				finalballs[8][1] = balls[k][1];
				finalballs[8][2] = balls[k][2];
				finalballs[8][3] = balls[k][3];
				finalballs[8][4] = balls[k][4];
				finalballs[8][5] = balls[k][5];
				finalballs[8][6] = balls[k][6];
				finalballs[8][7] = balls[k][7];
				finalposition[8][0] = realposition[k][0];
				finalposition[8][1] = realposition[k][1];
			}else if(balls[k][5] > 35){
				balls[k][6] = 0; // cue ball 
				finalballs[0][1] = balls[k][1];
				finalballs[0][2] = balls[k][2];
				finalballs[0][3] = balls[k][3];
				finalballs[0][4] = balls[k][4];
				finalballs[0][5] = balls[k][5];
				finalballs[0][6] = balls[k][6];
				finalballs[0][7] = balls[k][7];
				finalposition[0][0] = realposition[k][0];
				finalposition[0][1] = realposition[k][1];
			}else if(balls[k][5] >= 8){
				balls[k][6] = 9; // stripe 
				finalballs[9+stripecnt][1] = balls[k][1];
				finalballs[9 + stripecnt][2] = balls[k][2];
				finalballs[9 + stripecnt][3] = balls[k][3];
				finalballs[9 + stripecnt][4] = balls[k][4];
				finalballs[9 + stripecnt][5] = balls[k][5];
				finalballs[9 + stripecnt][6] = balls[k][6];
				finalballs[9 + stripecnt][7] = balls[k][7];
				finalposition[9 + stripecnt][0] = realposition[k][0];
				finalposition[9 + stripecnt][1] = realposition[k][1];

				stripecnt = stripecnt + 1;
				if(stripecnt > 7){
					solidcnt = 0;	//TODO MAKE THAT NOT WIPE BALLS
				}
			}else if(balls[k][5] < 8){
				balls[k][6] = 2; // solid 
				finalballs[0+solidcnt][1] = balls[k][1];
				finalballs[0 + solidcnt][2] = balls[k][2];
				finalballs[0 + solidcnt][3] = balls[k][3];
				finalballs[0 + solidcnt][4] = balls[k][4];
				finalballs[0 + solidcnt][5] = balls[k][5];
				finalballs[0 + solidcnt][6] = balls[k][6];
				finalballs[0 + solidcnt][7] = balls[k][7];
				finalposition[0 + solidcnt][0] = realposition[k][0];
				finalposition[0 + solidcnt][1] = realposition[k][1];
				solidcnt = solidcnt + 1;
				if (solidcnt > 7) { solidcnt = 1; }
			}
		}
	}
	balls = finalballs; 

	myfile.open("C:\\Users\\Max\\Capstone\\Capstone-Project\\resources\\TableState.csv");
	for(int k = 0; k < 16; k++){
		myfile << (finalposition[k][1] * 0.001) - OFFSETX << "," << (finalposition[k][0] * 0.001) - OFFSETY << endl;
	}
	myfile.close();

	return true;
}//ballinfo()


bool iswhite(int B, int G, int R){
	if(G < 130 || R < 130 || B < 130){
		return false;
	}

	return (pow(double(pow(double(255 - R), 2) + pow(double(255 - B), 2) + pow(double(255 - G), 2)), 0.5)) <= 100;
}//iswhite()

int main(){
	vector<cv::Vec4i> lines;
	cv::Mat imgOriginal;        // input image
	cv::Mat imgGrayscale;       // grayscale of input image
	cv::Mat imgforwhitedetection;         // intermediate blured image
	cv::Mat imgCanny;           // Canny edge image
	cv::Mat imgHSV; // HSV image
	vector<Vec3f> output;
	Vec4i finallines[4] = {	 // begin by assuming square of 4800 pixels.
			{10, 900, 10000, 900},
			{2650, 100, 2650, 4800},
			{4000, 4900, 100, 4900},
			{500, 4800, 500, 100}
	};
	ifstream text;
	std::string s1, temp = "";
	string s2 = "\"C:\\Users\\Max\\Documents\\Visual Studio 2015\\Projects\\TableStateVR\\TESTER.jpg\"",
			s3 = "C:\\Users\\Max\\Capstone\\Capstone-Project\\resources\\TableImage.jpg",
			commd, commd2;
	int balls[16][7];//stored as x,y,B,G,R,#ofwhitepixels,ballid.
	int deciderarray[3], linedirection[20000] = { 0 };// so far: 0 ignore line, 1 vertical line, 2 horizontal line.
	int hueValue = 0, hueRange = 15, minSaturation = 175, minValue = 50, mintilt, M = 0,
			tempwhitepix = 0, tempnotwhite = 0, ballfound = 0, mindistfromedge = 80;
	char next = 'a';
	bool ignorel = false, whitechecker = false;

	system("adb shell input keyevent KEYCODE_POWER");
	std::this_thread::sleep_for(std::chrono::milliseconds(2000));
	system("adb shell input keyevent KEYCODE_POWER");
	std::this_thread::sleep_for(std::chrono::milliseconds(2000));
	system("adb shell input swipe 300 300 500 1000");
	system("adb shell am start -a android.media.action.IMAGE_CAPTURE");
	std::this_thread::sleep_for(std::chrono::milliseconds(1000));
	system("adb shell input swipe 300 300 500 1000");
	std::this_thread::sleep_for(std::chrono::milliseconds(1000));
	system("adb shell input keyevent 27");
	std::this_thread::sleep_for(std::chrono::milliseconds(3000));
	system("ADB ls sdcard/DCIM/Camera >> \"C:\\Users\\Max\\Documents\\Visual Studio 2015\\Projects\\TableStateVR\\PICTURES.txt\"");

	text.open("C:\\Users\\Max\\Documents\\Visual Studio 2015\\Projects\\TableStateVR\\PICTURES.txt");
	
	while(std::getline(text, s1)){
		if(size(s1) > 5){
			temp = s1;
		}
	}
	text.close();

	temp = "adb pull \"sdcard/DCIM/Camera/" + temp.substr(27) + "\" ";
	commd = temp + s2;
	commd2 = temp + s3;

	const char *formatcomm = commd.c_str();
	const char *formatcomm2 = commd2.c_str();

	if(size(temp) > 5){
		system(formatcomm);
	}

	imgOriginal = cv::imread("C:\\Users\\Max\\Documents\\Visual Studio 2015\\Projects\\TableStateVR\\TESTER.jpg");

	if(imgOriginal.empty()){                                  	// if unable to open image
		std::cout << "error: image not read from file\n\n";     // show error message on command line
		_getch();                                               // may have to modify this line if not using Windows
		return(0);                                              // and exit program
	}
	cv::cvtColor(imgOriginal, imgGrayscale, CV_BGR2GRAY);       // convert to grayscale
	cv::cvtColor(imgOriginal, imgforwhitedetection, CV_BGR2Lab);


	cv::Canny(imgGrayscale, imgCanny, 80, 180);
	cv::cvtColor(imgOriginal, imgHSV, CV_BGR2HSV);	//NEW LINE DETECTION 

	std::vector<cv::Mat> hsvChannels;				// split the channels
	cv::split(imgHSV, hsvChannels);

	cv::Mat hueImage = hsvChannels[0];
	cv::Mat hueMask;
	cv::inRange(hueImage, hueValue - hueRange, hueValue + hueRange, hueMask);

	if(hueValue - hueRange < 0 || hueValue + hueRange > 180){
		cv::Mat hueMaskUpper;
		int upperHueValue = hueValue + 180; //in reality this would be + 360 instead
		cv::inRange(hueImage, upperHueValue - hueRange, upperHueValue + hueRange, hueMaskUpper);

		hueMask = hueMask | hueMaskUpper;	// add this mask to the other one
	}

	cv::Mat saturationMask = hsvChannels[1] > minSaturation;	//filter out all the pixels where saturation and value do not fit the limits
	cv::Mat valueMask = hsvChannels[2] > minValue;

	hueMask = (hueMask & saturationMask) & valueMask;

	HoughLinesP(hueMask, lines, 1, CV_PI / 180, 75, 300, 15);
	
	for(size_t r = 0; r < lines.size(); r++){
		mintilt = 100;
		ignorel = false;
		Vec4i l = lines[r];
		Vec4i others = lines[r];

		if(abs((int(l[0]) - int(l[2]))) < mintilt){	// check x begin/x end within certain distance(vertical).
			linedirection[r] = 1;
		}
		if(abs((int(l[1]) - int(l[3]))) < mintilt){ // check y begin/y end within certain distance(horizontal). 
			linedirection[r] = 2;
		}

		if(linedirection[r] == 1){ // don't align, extend vertical lines. 
			l[1] = 10; l[3] = 100000;
		}
		if(linedirection[r] == 2){ // don't align, extend horizontal lines.
			l[0] = 10; l[2] = 100000;
		}

		for(int k = 1; k < r; k++){
			others = lines[r - k];

			if(int(l[0]) > int(others[0] - 20) && int(l[0] < int(others[0]) + 20){
				linedirection[r] = 0;
			}
			if(int(l[1]) > int(others[1] - 20) && int(l[1] < int(others[1]) + 20){
				linedirection[r] = 0;
			}
			if(int(l[2]) > int(others[2] - 20) && int(l[2]) < int(others[2]) + 20){
				linedirection[r] = 0;
			}
			if(int(l[3]) > int(others[3] - 20) && int(l[3] < int(others[3]) + 20){
				linedirection[r] = 0;
			}

			if(linedirection[r] == 1 && l[0] < 550 && l[0] > finallines[3][0]){
				finallines[3] = l;
			}
			if(linedirection[r] == 1 && l[0] > 2300 && l[0] < finallines[1][0]){
				finallines[1] = l;
			}
			if(linedirection[r] == 2 && (l[1] < 1000) && l[1] > finallines[0][1]){
				finallines[0] = l;
			}
			if(linedirection[r] == 2 && (l[1] > 4800) && l[1] < finallines[2][1]){
				finallines[2] = l;
			}
		}

		if(linedirection[r] == 0){
			M = M + 1;
		}
	}

	for(int k = 0; k < 4; k++){
		line(imgOriginal, Point(finallines[k][0], finallines[k][1]), Point(finallines[k][2], finallines[k][3]), Scalar(0, 255, 0), 3, CV_AA);
	}

	cv::HoughCircles(imgGrayscale, output, CV_HOUGH_GRADIENT, 5, 116, 65, 120, 59, 65);

	/*
	* Beginning of circle detection
	*/
	for (size_t i = 0; i < output.size(); i++){
		if((output[i][0]) < finallines[1][0] - mindistfromedge			//left of right vertical x 
				&& output[i][0] > finallines[3][0] + mindistfromedge	//right of left vertical x
				&& output[i][1] < finallines[2][1] - mindistfromedge	//below the top horizontal y
				&& output[i][1] > finallines[0][1] + mindistfromedge	//above the bottom horizontal y
				&& !(output[i][1] < finallines[0][1] + 300 && output[i][0] < finallines[3][0] + 300)//Corner checks
				&& !(output[i][1] > finallines[2][1] - 300 && output[i][0] < finallines[3][0] + 300)
				&& !(output[i][1] < finallines[0][1] + 300 && output[i][0] > finallines[1][0] - 300)
				&& !(output[i][1] > finallines[2][1] - 300 && output[i][0] > finallines[1][0] - 300)){
			Point center(cvRound(output[i][0]), cvRound(output[i][1]));
			cv::Vec3b COLA(0, 0, 0);
			int radius = cvRound(output[i][2]);

			deciderarray[0] = 0;
			deciderarray[1] = 0;
			deciderarray[2] = 0;

			tempwhitepix = 0;
			tempnotwhite = 0;
			whitechecker = 0;

			for(size_t j = 0; j < 10; j++){
				Point current(cvRound(output[i][0] + ((radius / 12) * j*pow(-1, j))), cvRound(output[i][1]));
				COLA = imgOriginal.at<cv::Vec3b>(current);

				whitechecker = iswhite(int(COLA.val[0]), int(COLA.val[1]), int(COLA.val[2]));

				if(whitechecker == true){
					tempwhitepix = tempwhitepix + 1;
				}else{
					deciderarray[0] = deciderarray[0] + int(COLA.val[0]);
					deciderarray[1] = deciderarray[1] + int(COLA.val[1]);
					deciderarray[2] = deciderarray[2] + int(COLA.val[2]);

					tempnotwhite = tempnotwhite + 1;
				}
			}

			for(size_t j = 0; j < 10; j++){		// positive diagonal 
				Point current(cvRound(output[i][0] + ((radius / 12) * j*pow(-1, j))), (cvRound(output[i][1]) + ((radius / 12) * j*pow(-1, j))));
				COLA = imgOriginal.at<cv::Vec3b>(current);

				whitechecker = iswhite(int(COLA.val[0]), int(COLA.val[1]), int(COLA.val[2]));

				if (whitechecker == true){
					tempwhitepix = tempwhitepix + 1;
				}else{
					deciderarray[0] = deciderarray[0] + int(COLA.val[0]);
					deciderarray[1] = deciderarray[1] + int(COLA.val[1]);
					deciderarray[2] = deciderarray[2] + int(COLA.val[2]);

					tempnotwhite = tempnotwhite + 1;
				}
			}

			for(size_t j = 0; j < 10; j++){		// negative diagonal 
				Point current(cvRound(output[i][0] + ((radius / 12) * j*pow(-1, j))), (cvRound(output[i][1]) + ((radius / 12) * j*pow(-1, j + 1))));
				COLA = imgOriginal.at<cv::Vec3b>(current);

				whitechecker = iswhite(int(COLA.val[0]), int(COLA.val[1]), int(COLA.val[2]));

				if (whitechecker == true) {
					tempwhitepix = tempwhitepix + 1;
				}else{
					deciderarray[0] = deciderarray[0] + int(COLA.val[0]);
					deciderarray[1] = deciderarray[1] + int(COLA.val[1]);
					deciderarray[2] = deciderarray[2] + int(COLA.val[2]);

					tempnotwhite = tempnotwhite + 1;
				}
			}

			for(size_t k = 0; k < 10; k++){
				Point current(cvRound(output[i][0]), cvRound((output[i][1]) + ((radius / 12) * k*pow(-1, k))));
				COLA = imgOriginal.at<cv::Vec3b>(current);

				whitechecker = iswhite(int(COLA.val[0]), int(COLA.val[1]), int(COLA.val[2]));

				if(whitechecker == true){
					tempwhitepix = tempwhitepix + 1;
				}else{
					deciderarray[0] = deciderarray[0] + int(COLA.val[0]);
					deciderarray[1] = deciderarray[1] + int(COLA.val[1]);
					deciderarray[2] = deciderarray[2] + int(COLA.val[2]);

					tempnotwhite = tempnotwhite + 1;
				}
			}

			if(tempnotwhite > 35){
				balls[ballfound][0] = cvRound(output[i][0]);
				balls[ballfound][1] = cvRound(output[i][1]);
				balls[ballfound][2] = cvRound(deciderarray[0] / tempnotwhite);
				balls[ballfound][3] = cvRound(deciderarray[1] / tempnotwhite);
				balls[ballfound][4] = cvRound(deciderarray[2] / tempnotwhite);
				balls[ballfound][5] = tempwhitepix;
			}else{
				balls[ballfound][0] = cvRound(output[i][0]);
				balls[ballfound][1] = cvRound(output[i][1]);
				balls[ballfound][2] = 255;
				balls[ballfound][3] = 255;
				balls[ballfound][4] = 255;
				balls[ballfound][5] = tempwhitepix;
			}

			ballfound = ballfound + 1;
			int C = 0;
			int R = 255;
			int G = 0;
			if(balls[ballfound][4] == 255 && balls[ballfound][3] == 255 && balls[ballfound][2] == 255){
				R = 0;
			}

			if(ballfound == 16){//for testing
				R = 255;
			}

			circle(imgOriginal, center, 3, Scalar(C, G, R), -1, 8, 0);		// draw the circle center
			circle(imgOriginal, center, radius, Scalar(C, G, R), 3, 8, 0);	// draw the circle outline
		}
	}
	ballinfo(balls, finallines, ballfound);
	
	return 0;
}//main()