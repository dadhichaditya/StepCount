# StepCount
Graph : 
The graph shows the filtered value of accelerometer data collected from phone sensor Sensor.TYPE_ACCELEROMETER

Complimentary Filter was applied to the raw data collected :
The first value is taken as it is.
acc[prev]=raw accelerometer data;
acc[next]=0.9*acc[prev]+0.1*acc[next];

We approximate(filter) the next value by considering 90% of previous value and 10% of new raw value.


Step Count :

Initial Approach : 

We apply a threshold determined by experimentation on the filtered data.We then find the number of peaks occuring above this threshold.But one problem will still persist that is : When we apply this algorithm to different users it shows wrong result due to differnce in walking(gait) style.

Approach :

We do not check for threshold but for difference in the peak and last valley, we define this value as jerk.We apply threshold on jerk. 


Algorithm:
    • Check wheather it’s a peak above threshold(10.8 taken here)
    • If yes then Calculate the jerk(absolute difference between peak value and last valley).
    • Apply threshold on jerk as well as on absolute difference between current peak value and previous peak value because there might be case when the user is walking the peak values are not supposed to be far which will indicate it is irregular motion and not the user is not walking.

 
Everytime above conditions are true the phone should vibrate and increment the step count.

