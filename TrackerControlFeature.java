package com.htc_cs.android.vuforia.features;

import android.util.Log;

import com.htc_cs.android.vuforia.VuforiaException;
import com.htc_cs.android.vuforia.activity.ArFeaturesControl;
import com.vuforia.DataSet;
import com.vuforia.HINT;
import com.vuforia.ImageTargetBuilder;
import com.vuforia.ObjectTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.TrackableSource;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import java.util.ArrayList;

public class TrackerControlFeature {

    private ArFeaturesControl arFeaturesControl;

    private ObjectTracker myCurrentTracker;
    private DataSet myCurrentDataset;

    private int myCurrentTrackableId;

    public boolean isUserDefinedTargetBuildingStarted = false;

    public void onInit(int maxTargetsCount) throws VuforiaException {
        TrackerManager trackerManager = TrackerManager.getInstance();
        if (trackerManager == null) throw new VuforiaException.TrackerManagerInitializationException();

        Tracker tracker = trackerManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null) throw new VuforiaException.TrackersInitializationException();

        boolean success = Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, maxTargetsCount);
        if (!success) throw new VuforiaException.TrackersInitializationException();
    }

    public void onDeinit() throws VuforiaException {
        TrackerManager trackerManager = TrackerManager.getInstance();
        if (trackerManager == null) throw new VuforiaException.TrackerManagerInitializationException();

        boolean success = trackerManager.deinitTracker(ObjectTracker.getClassType());
        if (!success) throw new VuforiaException.TrackersDeinitializationException();
    }

    public void onLoadData(String pathInAssets) throws VuforiaException {
        TrackerManager trackerManager = TrackerManager.getInstance();
        if (trackerManager == null) throw new VuforiaException.TrackerManagerInitializationException();

        myCurrentTracker = (ObjectTracker) trackerManager.getTracker(ObjectTracker.getClassType());
        if (myCurrentTracker == null) throw new VuforiaException.TrackersInitializationException();

        myCurrentDataset = myCurrentTracker.createDataSet();
        if (myCurrentDataset == null) throw new VuforiaException.TrackersInitializationException();

        boolean success = myCurrentDataset.load(pathInAssets, STORAGE_TYPE.STORAGE_APPRESOURCE);
        if (!success) throw new VuforiaException.TrackersLoadingException();

        success = myCurrentTracker.activateDataSet(myCurrentDataset);
        if (!success) throw new VuforiaException.TrackersLoadingException();
        Log.d("countOfTrackers", String.valueOf(myCurrentDataset.getNumTrackables()));
    }

    public void onUnloadData() throws VuforiaException {
        myCurrentDataset = null;
    }

    public void onStartTracker() throws VuforiaException {
        if (myCurrentTracker == null) throw new VuforiaException.TrackersInitializationException();

        boolean success = myCurrentTracker.start();
        if (!success) throw new VuforiaException.TrackersStartException();
    }

    public void onStopTracker() throws VuforiaException {
        if (myCurrentTracker == null) throw new VuforiaException.TrackersInitializationException();
        myCurrentTracker.stop();
        myCurrentTracker = null;
    }

    public void onTrackerStartScanningForUserDefiningTarget() throws VuforiaException.TrackersUserDefinedTargetScanningError {
        if (myCurrentTracker == null) throw new VuforiaException.TrackersUserDefinedTargetScanningError();
        ImageTargetBuilder imageTargetBuilder = myCurrentTracker.getImageTargetBuilder();

        if (imageTargetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE) {
            imageTargetBuilder.stopScan();
        }

        myCurrentTracker.stop();

        imageTargetBuilder.startScan();
    }

    public int onTrackerBuildUserDefinedTarget() throws VuforiaException.TrackersUserDefinedTargetBuildingException {
        if (myCurrentTracker == null) throw new VuforiaException.TrackersUserDefinedTargetBuildingException();
        ImageTargetBuilder imageTargetBuilder = myCurrentTracker.getImageTargetBuilder();

        int imageQuality = imageTargetBuilder.getFrameQuality();
        if (imageQuality < 1) {
            throw new VuforiaException.UserDefinedTargetToLowImageQualityException(imageQuality);
        } else {
            boolean successfullyStarted = imageTargetBuilder.build("udt-target", 760);
            if (successfullyStarted) {
                isUserDefinedTargetBuildingStarted = true;
                return imageQuality;
            } else {
                isUserDefinedTargetBuildingStarted = false;
                return 0;
            }
        }
    }

    public void onTrackerActivateUserDefinedTarget() throws VuforiaException.TrackersUserDefinedTargetActivateException {
        if (myCurrentTracker == null) throw new VuforiaException.TrackersUserDefinedTargetActivateException();
        ImageTargetBuilder imageTargetBuilder = myCurrentTracker.getImageTargetBuilder();

        TrackableSource trackableSource = imageTargetBuilder.getTrackableSource();
        if (trackableSource == null) throw new VuforiaException.TrackersUserDefinedTargetActivateException();

        if (myCurrentDataset == null) throw new VuforiaException.TrackersUserDefinedTargetActivateException();

        boolean deactivated = myCurrentTracker.deactivateDataSet(myCurrentDataset);
        if (!deactivated) throw new VuforiaException.TrackersUserDefinedTargetActivateException();

        boolean destroyed = myCurrentTracker.destroyDataSet(myCurrentDataset);
        if (!destroyed) throw new VuforiaException.TrackersUserDefinedTargetActivateException();

        myCurrentDataset = myCurrentTracker.createDataSet();
        if (myCurrentDataset == null) throw new VuforiaException.TrackersUserDefinedTargetActivateException();

        Trackable trackable = myCurrentDataset.createTrackable(imageTargetBuilder.getTrackableSource());
        if (trackable == null) throw new VuforiaException.TrackersUserDefinedTargetActivateException();

        boolean activated = myCurrentTracker.activateDataSet(myCurrentDataset);
        if (!activated) throw new VuforiaException.TrackersUserDefinedTargetActivateException();

        myCurrentTracker.start();

        Log.d("trackerControl", "Successfully created udt");
    }
}


