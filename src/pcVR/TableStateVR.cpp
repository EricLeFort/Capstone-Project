// CannyStill.cpp

#include<opencv2/core/core.hpp>
#include<opencv2/highgui/highgui.hpp>
#include<opencv2/imgproc/imgproc.hpp>

#include<iostream>
#include<conio.h>           // may have to modify this line if not using Windows
#include<math.h>
#include<iomanip>
#include<fstream>
#include <chrono>
#include <thread>


using namespace std;
using namespace cv;
using namespace cv::cuda;

bool ballinfo(int balls[16][7], Vec4i lines[4], int N) {

	//stored as x,y,B,G,R,#ofwhitepixels,ballid.// the final lines are stored as botthoriz. right vert. top hori, left verti //(xinit,yinit,xfin,yfin)
	double MILYLEN = 1880;
	double MILXLEN = 960;
	double realposition[16][2] = { 0 };
	int full_dy = lines[2][1] - lines[0][1]; //top horizontal init y - bott horizontal init y
	int full_dx = lines[1][0] - lines[3][0]; //right horizontal x - left horizontal init x 
											 //cout << balls[0][0] << " why is this bad??1 " << endl; 
	double milpery = MILYLEN / full_dy;
	double milperx = MILXLEN / full_dx;

	int zero[2] = { lines[3][0],lines[0][1] };


	for (int k = 0; k < 16; k++) {
		if (k >= N) { realposition[k][0] = -1; realposition[k][1] = -1; }
		else {
			//cout << "ballx " << balls[k][0] << " bally " << balls[k][1] << endl;

			realposition[k][0] = (balls[k][0] - zero[0])*(milperx);
			realposition[k][1] = (balls[k][1] - zero[1])*(milpery);
			//cout << balls[k][3] << "if this is greater than 210 " << balls[k][4] << "and this is small" << balls[k][2] << endl;

			// ok lets get these colours :/. 
			//4,3,2 because thats RGB instead of GBR. 
			/*
			if (balls[k][4] <20 && balls[k][3] <20 && balls[k][2] <20) {
			balls[k][6] = 8; // black ball
			}
			else if (balls[k][4] >250 && balls[k][3] >250 && balls[k][2] >250) {
			balls[k][6] = 0; // cue ball
			}
			else if ((balls[k][4] > 180) && (balls[k][3] > 180) && (balls[k][2] < 70) && (balls[k][5] < 6)) {
			balls[k][6] = 1; // yellow ball (no stripe)
			}
			else if (balls[k][4] >180 && balls[k][3] >180 && balls[k][2] <70 && balls[k][5] >= 6) {
			balls[k][6] = 9; // yellow ball (stripe)
			}
			else if ((balls[k][4] >200) && (balls[k][3] <  (1/2)*balls[k][4]) && balls[k][5] >= 6) {
			balls[k][6] = 11; // bright red ball (stripe)
			}
			else if ((balls[k][4] >200) && (balls[k][3] <  (1 / 2)*balls[k][4]) && balls[k][5] < 6) {
			balls[k][6] = 3; // bright red ball ( no stripe)
			}
			else if ((balls[k][4] >200) && (balls[k][3] > balls[k][2]) && balls[k][5] < 6) {
			balls[k][6] = 5; // OG  ball ( no stripe)
			}
			else if ((balls[k][4] >200) && (balls[k][3] > balls[k][2]) && balls[k][5] >= 6) {
			balls[k][6] = 13; // OG  ball (stripe)
			}
			else if ((balls[k][4] >150) && balls[k][5] >= 6) {
			balls[k][6] = 15; // dark red  ball (stripe)
			}
			else if ((balls[k][4] >150) && balls[k][5] < 6) {
			balls[k][6] = 7; // dark red ball (no stripe)
			}
			else if ((balls[k][4] > balls[k][3]) && (balls[k][2] > balls[k][3]) && (balls[k][2]>100) && balls[k][5] < 6) {
			balls[k][6] = 4; // purple ball (no stripe)
			}
			else if ((balls[k][4] > balls[k][3]) && (balls[k][2] > balls[k][3]) && (balls[k][2]>100) && balls[k][5] >= 6) {
			balls[k][6] = 12; // purple red ball (stripe)
			}
			else if ((90 > balls[k][3]) && (90> balls[k][2]) && (balls[k][4]<90) && balls[k][5] >= 6) {
			balls[k][6] = 15; // dark red ball (stripe)
			}
			else if ((90 > balls[k][3]) && (90> balls[k][2]) && (balls[k][4]<90) && balls[k][5] < 6) {
			balls[k][6] = 7; // dark red ball (no stripe)
			}
			// this second set catches dark red edge cases.
			else if ((balls[k][3] > balls[k][2]) && (balls[k][4]> balls[k][2]) && (balls[k][2]>100) && balls[k][5] < 6) {
			balls[k][6] = 2; // dark blue ball (no stripe)
			}
			else if ((balls[k][3] > balls[k][2]) && (balls[k][4]> balls[k][2]) && (balls[k][2]>100) && balls[k][5] >= 6) {
			balls[k][6] = 10; // dark blue ball (stripe)
			}
			else if ((balls[k][3]>100) && balls[k][5] < 6) {
			balls[k][6] = 6; // green blue ball (no stripe)
			}
			else if ((balls[k][3]>100) && balls[k][5] >= 6) {
			balls[k][6] = 14; // green blue ball (stripe)
			}
			*/
			
			if (balls[k][4] <110 && balls[k][3] <110 && balls[k][2] <110) {
				balls[k][6] = 8; // black ball 
			}
			else if (balls[k][5] > 35) {
				balls[k][6] = 0; // cue ball 
			}
			else if (balls[k][5] >= 8) {
				balls[k][6] = 9; // stripe 
			}
			else if (balls[k][5] < 8) {
				balls[k][6] = 2; // solid 
			}
			//cout << balls[k][2] << " B " << balls[k][3] << " G " << balls[k][4] << " R " << balls[k][6] << " ID " << endl;

			//THIS IS BUBBLE SORT>>> HAHAHA
			for (int in = 0; in < 16; in++){
					if (balls[in][6] == 0) {
						swap(balls[in], balls[0]);
						break;
					}
				if (balls[in][6] == 8) {
					swap(balls[in], balls[8]);
					break;
				}
			}
		}
	}










	ofstream myfile;

	float OFFSETX = 0.020;
	float OFFSETY = 0.018;
	if (remove("C:\\Users\\Max\\Capstone\\Capstone-Project\\resources\\TableState.csv") != 0) {
		//cout << "first run, nothing to clean (or error)" << endl;
	}
	else {
		//cout << "File successfully reset" << endl;
	}

	// this just deletes the previous output ^
	myfile.open("C:\\Users\\Max\\Capstone\\Capstone-Project\\resources\\TableState.csv");

	for (int k = 0; k < 16; k++) {
		//TODO sort balls
		myfile << (realposition[k][1] * 0.001) - OFFSETX << "," << (realposition[k][0] * 0.001) - OFFSETY << endl;
	}

	myfile.close();


	return(true);

}


bool iswhite(int B, int G, int R)
{
	bool B_ok = true;
	bool G_ok = true;
	bool R_ok = true;

	if (B < 130) { B_ok = false; }if (G < 130) { G_ok = false; }if (R < 130) { R_ok = false; } // knock out low outliers
	float colourdist = (pow(double(pow(double(255 - R), 2) + pow(double(255 - B), 2) + pow(double(255 - G), 2)), 0.5));


	if (colourdist > 100) { B_ok = false; } // knock out numbers very far in distance from white.

											/*
											float avg = (G + B + R) / 3;
											float percent_thresh =10;
											*/

											/*
											if (avg > 200) {
											float percent_thresh = 43;
											}
											if ((avg <= 190) && (avg>180)) {
											float percent_thresh = 40.5;
											}
											if ((avg <= 190) && (avg>170)) {
											float percent_thresh = 38;
											}
											if ((avg <= 160) && (avg>150)) {
											float percent_thresh = 33;
											}
											if ((avg <= 150) && (avg>140)) {
											float percent_thresh = 28;
											}
											*/

											/*
											float low_B = B*(1-(percent_thresh/100));
											float high_B = B*(1+(percent_thresh/100));
											float low_R = R*(1 - (percent_thresh / 100));
											float high_R = R*(1 + (percent_thresh / 100));

											if ((low_B > float(G)) || (float(G) > high_B)) { G_ok = false; } //knock out greens far from blues
											if ((low_B > float(R)) || (float(R) > high_B)) { R_ok = false; } //knock out reds far from blues
											if ((low_R > float(G)) || (float(G) > high_R)) { G_ok = false; } //knock out greens far from reds

											int culmulative_distance = abs(B - G) + abs(B - R) + abs(R - G);
											if (culmulative_distance > 115) { R_ok = false; }

											if (colourdist < 80) { return(true); } // override all if very small distance from white.

											*/
	return (G_ok && B_ok && R_ok);

}
//adb pull sdcard / DCIM / Camera / TESTER.jpg "C:\Users\Max\Documents\Visual Studio 2015\Projects\Project1\TESTER.png"
///////////////////////////////////////////////////////////////////////////////////////////////////
int main() {


	cv::Mat imgOriginal;        // input image
	cv::Mat imgGrayscale;       // grayscale of input image
	cv::Mat imgforwhitedetection;         // intermediate blured image
	cv::Mat imgCanny;           // Canny edge image
	cv::Mat imgHSV; // HSV image
	vector<Vec3f> output;

	//system("C:\\Users\\Max\\Desktop\\switchscript2.exe");


	system("adb shell input keyevent KEYCODE_POWER");
	std::this_thread::sleep_for(std::chrono::milliseconds(2000));
	//system("timeout -t 2");
	system("adb shell input keyevent KEYCODE_POWER");
	std::this_thread::sleep_for(std::chrono::milliseconds(2000));
	//system("timeout -t 2");
	system("adb shell input swipe 300 300 500 1000");
	system("adb shell am start -a android.media.action.IMAGE_CAPTURE");
	std::this_thread::sleep_for(std::chrono::milliseconds(1000));
	//system("timeout -t 1");
	system("adb shell input swipe 300 300 500 1000");
	std::this_thread::sleep_for(std::chrono::milliseconds(1000));
	//system("timeout -t 1");
	system("adb shell input keyevent 27");
	std::this_thread::sleep_for(std::chrono::milliseconds(3000));
	//system("timeout -t 8");

	/*
	system("adb shell am start -a android.media.action.IMAGE_CAPTURE");
	system("timeout -t 3");
	system("adb shell input keyevent 27");
	system("timeout -t 5");
	
	//HOPEFULLY THIS WILL GRAB THE PHOTO
	////

	if (remove("PICTURES.txt") != 0) {
		cout << "first run, nothing to clean (or error)" << endl;
	}
	else {
		cout << "File successfully reset" << endl;
	}
	*/
	ifstream text; //("PICTURES.txt");

				   // this just deletes the previous output ^
	system("ADB ls sdcard/DCIM/Camera >> \"C:\\Users\\Max\\Documents\\Visual Studio 2015\\Projects\\TableStateVR\\PICTURES.txt\"");
	//system("ADB ls sdcard/DCIM/Camera >> \".\\PICTURES.txt\"");



	text.open("C:\\Users\\Max\\Documents\\Visual Studio 2015\\Projects\\TableStateVR\\PICTURES.txt");
	char next = 'a';
	std::string s1;
	std::string temp = "";


	while (std::getline(text, s1))
	{
		if (size(s1) > 5) { temp = s1; }
		//cout << s1 << "this is totally a thing";
		//cout << temp << " AND THIS WAS SAVED !!!";
	}
	text.close();
	//cout << "atleast we got here" << "1";
	temp = temp.substr(27);
	//cout << endl << temp << endl;
	temp = "adb pull \"sdcard/DCIM/Camera/" + temp + "\" ";
	string s2 = "\"C:\\Users\\Max\\Documents\\Visual Studio 2015\\Projects\\TableStateVR\\TESTER.jpg\"";
	string s3 = "C:\\Users\\Max\\Capstone\\Capstone-Project\\resources\\TableImage.jpg";
	string commd = temp + s2;
	string commd2 = temp + s3;
	//cout << commd;
	//cout << "atleast we got here" << "2";

	const char * formatcomm = commd.c_str();
	const char * formatcomm2 = commd2.c_str();
	//cout << "atleast we got here" << "3";
	if (size(temp) > 5) {
		system(formatcomm);
	}
	else { cout << "error reading"; }
	//cout << "we got " << s1 << "photo";
	///////////






	imgOriginal = cv::imread("C:\\Users\\Max\\Documents\\Visual Studio 2015\\Projects\\TableStateVR\\TESTER.jpg");
	//imgOriginal = cv::imread("image9.jpg");          // open image// using full reference now. tester 1 is proper

	if (imgOriginal.empty()) {                                  // if unable to open image
		std::cout << "error: image not read from file\n\n";     // show error message on command line
		_getch();                                               // may have to modify this line if not using Windows
		return(0);                                              // and exit program
	}
	cv::cvtColor(imgOriginal, imgGrayscale, CV_BGR2GRAY);       // convert to grayscale
	cv::cvtColor(imgOriginal, imgforwhitedetection, CV_BGR2Lab);
	//cv::GaussianBlur(imgGrayscale,          // input image
	//	imgBlurred,                         // output image
	//	cv::Size(5, 5),                     // smoothing window width and height in pixels
	//	1.5);                               // sigma value, determines how much the image will be blurred


	cv::Canny(imgGrayscale,           // input image
		imgCanny,                   // output image
		80,                        // low threshold was 50
		180);                       // high threshold was 200

									//NEW LINE DETECTION 
	cv::cvtColor(imgOriginal, imgHSV, CV_BGR2HSV);

	// split the channels
	std::vector<cv::Mat> hsvChannels;
	cv::split(imgHSV, hsvChannels);


	int hueValue = 0; // red color
	int hueRange = 15; // how much difference from the desired color we want to include to the result If you increase this value, for example a red color would detect some orange values, too.

	int minSaturation = 175; // I'm not sure which value is good here...
	int minValue = 50; // not sure whether 50 is a good min value here...

	cv::Mat hueImage = hsvChannels[0];

	cv::Mat hueMask;
	cv::inRange(hueImage, hueValue - hueRange, hueValue + hueRange, hueMask);

	if (hueValue - hueRange < 0 || hueValue + hueRange > 180)
	{
		cv::Mat hueMaskUpper;
		int upperHueValue = hueValue + 180; // in reality this would be + 360 instead
		cv::inRange(hueImage, upperHueValue - hueRange, upperHueValue + hueRange, hueMaskUpper);

		// add this mask to the other one
		hueMask = hueMask | hueMaskUpper;
	}

	// now we have to filter out all the pixels where saturation and value do not fit the limits:
	cv::Mat saturationMask = hsvChannels[1] > minSaturation;
	cv::Mat valueMask = hsvChannels[2] > minValue;

	hueMask = (hueMask & saturationMask) & valueMask;

	//cv::imshow("desired color", hueMask);











	/*
	///
	line detection
	///
	*/











	//used imgCanny before...
	vector<cv::Vec4i> lines;
	HoughLinesP(hueMask, lines, 1, CV_PI / 180, 75, 300, 15);
	//cout << lines.size() << " lines found" << endl;
	bool ignorel = false;
	int linedirection[20000] = { 0 };// so far: 0 ignore line, 1 vertical line, 2 horizontal line. 
	int M = 0;
	// the final lines are stored as botthoriz. right vert. top hori, left verti
	/**/

	Vec4i finallines[4] = { { 10,900,10000,900 },{ 2650,100,2650,4800 },{ 4000,4900,100,4900 },{ 500,4800,500,100 } }; // begin by assuming square of 4800 pixels.
	
	
for (size_t r = 0; r < lines.size(); r++)
	{
		int mintilt = 100;
		ignorel = false;
		Vec4i l = lines[r];

		Vec4i others = lines[r];

		if (abs((int(l[0]) - int(l[2]))) < mintilt) { linedirection[r] = 1; } // check x begin/x end within certain distance(vertical).
		if (abs((int(l[1]) - int(l[3]))) < mintilt) { linedirection[r] = 2; } // check y begin/y end within certain distance(horizontal). 

		if (linedirection[r] == 1) { l[1] = 10; l[3] = 100000; /*l[0] = l[2]*/; } // don't align, extend vertical lines. 
		if (linedirection[r] == 2) { l[0] = 10; l[2] = 100000; /*l[1] = l[3]*/; } // don't align, extend horizontal lines.

		if (r == 0) { cout << finallines[0] << finallines[2] << "ORIGINAL TOP/BOTTOM" << endl; }

		for (int k = 1; k < r; k++) {
			others = lines[r - k];

			if ((int(l[0]) > int(others[0]) - 20) && (int(l[0]) < int(others[0]) + 20)) { linedirection[r] = 0; }
			if ((int(l[1]) > int(others[1]) - 20) && (int(l[1]) < int(others[1]) + 20)) { linedirection[r] = 0; }
			if ((int(l[2]) > int(others[2]) - 20) && (int(l[2]) < int(others[2]) + 20)) { linedirection[r] = 0; }
			if ((int(l[3]) > int(others[3]) - 20) && (int(l[3]) < int(others[3]) + 20)) { linedirection[r] = 0; }
			/*
			if ((linedirection[r] == 2)) { cout << l << "this is the line" << endl;
			cout << l[1] << "needs to be lower than 2000 and higher than  ->" << finallines[0][1] << endl;
			cout << "or" << l[1] << "needs to be higher than 2000 and lower than  ->" << finallines[2][1] << endl;
			}
			*/



			//values were 600 2300 1000 4800

			if ((linedirection[r] == 1) && (l[0] < 550) && (l[0]) > finallines[3][0]) { finallines[3] = l; }
			if ((linedirection[r] == 1) && (l[0] > 2300) && (l[0]) < finallines[1][0]) { finallines[1] = l; }
			if ((linedirection[r] == 2) && (l[1] < 1000) && (l[1]) > finallines[0][1]) { finallines[0] = l; }
			if ((linedirection[r] == 2) && (l[1] > 4800) && (l[1]) < finallines[2][1]) { finallines[2] = l; }


			//ingore lines generated from points very close to eachother. 

		}

		if (linedirection[r] == 0) {
			M = M + 1;
			//cout << M << "line code" << l[0] << " < xstart " << l[1] << " <ystart " << l[2] << " <xend " << l[3] << "yend"<<endl ;
			//line(imgOriginal, Point(l[0], l[1]), Point(l[2], l[3]), Scalar(0, 0, 255), 3, CV_AA);
		}
	}

	for (int k = 0; k < 4; k++) {


		line(imgOriginal, Point(finallines[k][0], finallines[k][1]), Point(finallines[k][2], finallines[k][3]), Scalar(0, 255, 0), 3, CV_AA);

	}

	









	//cout << "before circles" << endl;

	//after 5 used to be imgGrayscale.rows / 40
	cv::HoughCircles(imgGrayscale, output, CV_HOUGH_GRADIENT, 5, 116, 65, 120, 59, 65);
	int balls[16][7];//stored as x,y,B,G,R,#ofwhitepixels,ballid.
	int deciderarray[3];
	int tempwhitepix = 0;
	int tempnotwhite = 0;
	bool whitechecker = false;
	int ballfound = 0;
	//cout << lines.size() << " numlines " << endl;
	//eginning of circle detection 
	int mindistfromedge = 80;
	for (size_t i = 0; i < output.size(); i++)
	{

		/* for testing if statement.
		cout << output[i][0] << " < " << finallines[1][0] << " RIGHT VERTI " << endl;
		cout << output[i][0] << " > " << finallines[3][0] << " LEFT VERTI" << endl;
		cout << output[i][1] << " < " << finallines[2][1] << " BELOW TOP" << endl;
		cout << output[i][1] << " > " << finallines[0][1] << " ABOVE BOTT" << endl;
		*/


		if (		// only takes balls in the lines. 
			((output[i][0]) < (finallines[1][0]) - mindistfromedge)		// to the left of right verti x 

			&& ((output[i][0]) > (finallines[3][0]) + mindistfromedge) // to the right of left verti x

			&& ((output[i][1]) < (finallines[2][1]) - mindistfromedge) // below the top hori y

			&& ((output[i][1]) > (finallines[0][1]) + mindistfromedge) // above the bottom hori y

																	   // this part checks for the corners specifically, a bit more gratuitously than the edges

			&& !(((output[i][1]) < (finallines[0][1]) + 300) && ((output[i][0]) < (finallines[3][0]) + 300))

			&& !(((output[i][1]) > (finallines[2][1]) - 300) && ((output[i][0]) < (finallines[3][0]) + 300))

			&& !(((output[i][1]) < (finallines[0][1]) + 300) && ((output[i][0]) > (finallines[1][0]) - 300))

			&& !(((output[i][1]) > (finallines[2][1]) - 300) && ((output[i][0]) > (finallines[1][0]) - 300))

			) { //for testing



			Point center(cvRound(output[i][0]), cvRound(output[i][1]));
			cv::Vec3b COLA(0, 0, 0);
			int radius = cvRound(output[i][2]);




			//Vec3b colour = imgOriginal.at<Vec3b>(cvRound(output[i][0]), cvRound(output[i][1]));


			deciderarray[0] = 0; deciderarray[1] = 0; deciderarray[2] = 0;
			tempwhitepix = 0; tempnotwhite = 0; whitechecker = 0;

			for (size_t j = 0; j < 10; j++) {

				Point current(cvRound(output[i][0] + ((radius / 12) * j*pow(-1, j))), cvRound(output[i][1]));
				COLA = imgOriginal.at<cv::Vec3b>(current);
				//imgOriginal.at<cv::Vec3b>(current) = (0, 0, 255);

				whitechecker = iswhite(int(COLA.val[0]), int(COLA.val[1]), int(COLA.val[2]));
				//cout << COLA << whitechecker << endl;

				//cout << whitechecker << "whitechecker" << endl;
				if (whitechecker == true) {
					tempwhitepix = tempwhitepix + 1;
					//cout << COLA << endl;

				}
				else {

					deciderarray[0] = deciderarray[0] + int(COLA.val[0]);
					deciderarray[1] = deciderarray[1] + int(COLA.val[1]);
					deciderarray[2] = deciderarray[2] + int(COLA.val[2]);
					//cout << deciderarray[0] << "< decider array" << int(COLA.val[0]) << "< the colour" << tempnotwhite << "< numcol" << endl;

					tempnotwhite = tempnotwhite + 1;
				}
			}

			for (size_t j = 0; j < 10; j++) {
				// positive diagonal 
				Point current(cvRound(output[i][0] + ((radius / 12) * j*pow(-1, j))), (cvRound(output[i][1]) + ((radius / 12) * j*pow(-1, j))));
				COLA = imgOriginal.at<cv::Vec3b>(current);
				//imgOriginal.at<cv::Vec3b>(current) = (0, 0, 255);

				whitechecker = iswhite(int(COLA.val[0]), int(COLA.val[1]), int(COLA.val[2]));
				//cout << COLA << whitechecker << endl;

				//cout << whitechecker << "whitechecker" << endl;
				if (whitechecker == true) {
					tempwhitepix = tempwhitepix + 1;
					//cout << COLA << endl;

				}
				else {

					deciderarray[0] = deciderarray[0] + int(COLA.val[0]);
					deciderarray[1] = deciderarray[1] + int(COLA.val[1]);
					deciderarray[2] = deciderarray[2] + int(COLA.val[2]);
					//cout << deciderarray[0] << "< decider array" << int(COLA.val[0]) << "< the colour" << tempnotwhite << "< numcol" << endl;

					tempnotwhite = tempnotwhite + 1;
				}
			}

			for (size_t j = 0; j < 10; j++) {
				// negative diagonal 
				Point current(cvRound(output[i][0] + ((radius / 12) * j*pow(-1, j))), (cvRound(output[i][1]) + ((radius / 12) * j*pow(-1, j + 1))));
				COLA = imgOriginal.at<cv::Vec3b>(current);

				//imgOriginal.at<cv::Vec3b>(current) = (0, 0, 255);

				whitechecker = iswhite(int(COLA.val[0]), int(COLA.val[1]), int(COLA.val[2]));
				//cout << COLA << whitechecker << endl;

				//cout << whitechecker << "whitechecker" << endl;
				if (whitechecker == true) {
					tempwhitepix = tempwhitepix + 1;
					//cout << COLA << endl;

				}
				else {

					deciderarray[0] = deciderarray[0] + int(COLA.val[0]);
					deciderarray[1] = deciderarray[1] + int(COLA.val[1]);
					deciderarray[2] = deciderarray[2] + int(COLA.val[2]);
					//cout << deciderarray[0] << "< decider array" << int(COLA.val[0]) << "< the colour" << tempnotwhite << "< numcol" << endl;

					tempnotwhite = tempnotwhite + 1;
				}
			}






			for (size_t k = 0; k < 10; k++) {
				Point current(cvRound(output[i][0]), cvRound((output[i][1]) + ((radius / 12) * k*pow(-1, k))));
				COLA = imgOriginal.at<cv::Vec3b>(current);
				//imgOriginal.at<cv::Vec3b>(current) = (0, 0, 255);
				whitechecker = iswhite(int(COLA.val[0]), int(COLA.val[1]), int(COLA.val[2]));
				//cout << COLA << whitechecker << endl;
				if (whitechecker == true) {
					tempwhitepix = tempwhitepix + 1;
					//cout << COLA << "iswhite" << endl;
				}
				else {
					deciderarray[0] = deciderarray[0] + int(COLA.val[0]);
					deciderarray[1] = deciderarray[1] + int(COLA.val[1]);
					deciderarray[2] = deciderarray[2] + int(COLA.val[2]);
					//cout << deciderarray[0] << "< decider array" << int(COLA.val[0]) << "< the colour" << tempnotwhite << "< numcol" << endl;
					tempnotwhite = tempnotwhite + 1;
				}
			}




			if (tempnotwhite > 35) {
				//cout << "coloured numcolouredpixels >> " << tempnotwhite << endl;
				balls[ballfound][0] = cvRound(output[i][0]);
				balls[ballfound][1] = cvRound(output[i][1]);
				balls[ballfound][2] = cvRound(deciderarray[0] / tempnotwhite);
				balls[ballfound][3] = cvRound(deciderarray[1] / tempnotwhite);
				balls[ballfound][4] = cvRound(deciderarray[2] / tempnotwhite);
				balls[ballfound][5] = tempwhitepix;
			}
			else
			{
				//cout << "ball" << i << "iswhite" << endl;
				balls[ballfound][0] = cvRound(output[i][0]);
				balls[ballfound][1] = cvRound(output[i][1]);
				balls[ballfound][2] = 255;
				balls[ballfound][3] = 255;
				balls[ballfound][4] = 255;
				balls[ballfound][5] = tempwhitepix;
			}

			//cout << balls[ballfound][0] << "|0|" << balls[ballfound][1] << "|1|" << balls[ballfound][2] << "|2|" << balls[ballfound][3] << "<|3|" << balls[ballfound][4] << "<|4|" << balls[ballfound][5] << "<|5|" << endl;

			ballfound = ballfound + 1;
			int C = 0;
			int R = 255;
			int G = 0;
			if (balls[ballfound][4] == 255 && balls[ballfound][3] == 255 && balls[ballfound][2] == 255) {
				C = 0; G = 0; R = 0;
			}

			//for testing
			if (ballfound == 16) {
				R = 255;
			}

			// draw the circle center
			circle(imgOriginal, center, 3, Scalar(C, G, R), -1, 8, 0);
			// draw the circle outline
			circle(imgOriginal, center, radius, Scalar(C, G, R), 3, 8, 0);



		}
		//cout << i << "------------------";

	}


	//cv::namedWindow("circles", CV_WINDOW_NORMAL);
	//cv::imshow("circles", imgOriginal);





	ballinfo(balls, finallines, ballfound);

	// declare windows
	//cv::namedWindow("hueMask", CV_WINDOW_NORMAL);
	//cv::namedWindow("imgOriginal", CV_WINDOW_NORMAL);     // note: you can use CV_WINDOW_NORMAL which allows resizing the window
	//cv::namedWindow("imgCanny", CV_WINDOW_NORMAL);        // or CV_WINDOW_AUTOSIZE for a fixed size window matching the resolution of the image
	// CV_WINDOW_AUTOSIZE is the default
	//cv::imshow("hueMask", imgOriginal);
	//cv::imshow("imgOriginal", imgOriginal);     // show windows
	//cv::imshow("imgCanny", imgCanny);
	//cv::waitKey(0);                 // hold windows open until user presses a key
	//cout << "ITS GOT THIS FARRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR";
	cout << "finished" << endl;
	return(0);
}



