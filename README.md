# PervasiveSmartwatchApplication

The project goal is to determine the status of the room (Lecture, Transition or Empty). 
The project is part of the Pervasive design and development tutorial.

Every user in the room wears smart-watch which sends the predicted status of that user(walking, standing or sitting) to server. Using the activities of each user, my smart watch determine the status of each user(listener, lecturer or transition). In addition it determines the status of the room. Then users' status and room status on the watch's screen and send it to the server.

**Individual Classification**

Data from the accelerometer sensor in android smart-watch is captured when doing different activities which are walking, standing and sitting. Data are exported to ARFF format, which include the values for mean and variance for accelerometer of x, y and Z and magnitude of variance for window sizes 300ms, 1000ms and 3000ms. Data for mean accelerometer of Z and magnitude of variance for window size 3000ms give the best separation. Therefore this data was trained with K nearest neighbor algorithm.

For prediction, using sliding window of 3000ms, magnitude of variance and mean accelerometer of Z axis are calculated for the captured data. Using the trained algorithm I predict the status of the user.
