%playing with Virtual Recognition Matlab libraries.
[pic,mymap] = imread('../../resources/good_balls.jpg');
image(pic)
%try
%takes image read file, and if indexed changes it to RGB colour mapping
%if mymap is empty it is already a RGB image.
if ~isempty(mymap)
    MyIm = ind2rgb(pic,mymap);
else 
    MyIm = pic;
end


%{
this just prints out the picture, as normal
whos pic
figure('Name','RGB Truecolor Image')
imagesc(MyIm)
axis image
%}


% % zoom(1)
% Convert RGB to an indexed image with 32 colors


%{ 
%This section of code reduces the colours in MyIm to 4, and saves it in 
%ind, before showing it (unzoomed)

[IND,map] = rgb2ind(MyIm,4);
figure('Name','Indexed image with 32 Colors')
imagesc(IND)
colormap(map)
axis image
zoom(1)
%}
imshow(pic)

%d = imdistline() %used this for finding distance across circle
%distance across circle is 180-220
%so radius is 90-110



%internet says we should check if they are brighter or darker, so greyscale
%make it grey, make it greeeeyt!
grayscale = rgb2gray(pic);
imshow(grayscale)
%many are brighter (more reflective)

%this should be our first attempt at finding circles.
%[centers, radiiBright, metricBright] = imfindcircles(grayscale,[70 90], ...
%    'ObjectPolarity','bright','Sensitivity',0.92,'EdgeThreshold',0.1);
[centersdark, radiidark, metricBright] = imfindcircles(grayscale,[70 90], ...
    'ObjectPolarity','dark','Sensitivity',0.95,'EdgeThreshold',0.05);
[centers, radii] = imfindcircles(grayscale,[70 90],'Sensitivity',.91,'Method','twostage'...
    ,'EdgeThreshold',0.05)
length(centers)
centersStrong5 = centers(1:(length(centers)),:);
radiiStrong5 = radii(1:(length(centers)));
viscircles(centersStrong5, radiiStrong5,'EdgeColor','b');


length(centersdark)
centersStrongdark5 = centersdark(1:4,:);
radiiStrongdark5 = radiidark(1:4);
viscircles(centersStrongdark5, radiiStrongdark5,'EdgeColor','b');
