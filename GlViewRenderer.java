package com.melcosoft.money_screenholder.glrenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.melcosoft.money_screenholder.R;
import com.melcosoft.money_screenholder.sensors.GyroscopeSensorManager;

import javax.microedition.khronos.opengles.GL10;

import ru.cnv.opengl.renderer.GlRenderer;
import ru.cnv.opengl.features.GLAnimation;
import ru.cnv.opengl.features.GLElementsDrawer;
import ru.cnv.opengl.features.GlModels;

public class GlViewRenderer extends GlRenderer implements
        GLAnimation.GlFrameAnimation.GlAnimationFinishListener {

    private final String TAG = this.getClass().getSimpleName();

    private static final float LEFT_GROUP_TRANS_X = -3f;
    private static final float LEFT_GROUP_RANDOM_TRANS_Z_MIN = -8f;
    private static final float LEFT_GROUP_RANDOM_TRANS_Z_MAX = 8f;

    private static final float RIGHT_GROUP_TRANS_X = 3f;
    private static final float RIGHT_GROUP_RANDOM_TRANS_Z_MIN = -8f;
    private static final float RIGHT_GROUP_RANDOM_TRANS_Z_MAX = 8f;

    private static final int MODELS_ARRAY_CAPACITY = 7;
    private static final int ANIMATIONS_ARRAY_CAPACITY = 7;

    private static final int TEXTURES_MAX_WIDTH = 1280;
    private static final int TEXTURES_MAX_HEIGHT = 1800;

    private static final float PERSPECTIVE_ANGLE = 60f;
    private static final float PERSPECTIVE_NEAR_PLANE = 0.1f;
    private static final float PERSPECTIVE_FAR_PLANE = 60f;
    private static final float PERSPECTIVE_ASPECT_RATIO = 0.56f;

    private static final float CAMERA_INITIAL_Y = 22f;
    private static final float CAMERA_INITIAL_DIR_Y = 0;
    private static final float[] CAMERA_INITIAL_ORIENT_VECTOR = {0, 1, 0};

    public static final float GYROSCOPE_VALUE_DIVIDER = 50f;

    private int mMoneySpeed;
    private int mMoneyCount;
    private int mMoneyTextureUnitId, mBgTextureUnitId;

    private int mProgramId;

    private GlModels.GlBufferedModel[] mGlBufferedModels;
    private GLAnimation.GlFrameAnimation[] mFrameAnimations;

    private volatile float[] mRotationSensorValues;

    public GlViewRenderer(Context context) {
        super(context);
        mGlBufferedModels = new GlModels.GlBufferedModel[MODELS_ARRAY_CAPACITY];
        mFrameAnimations = new GLAnimation.GlFrameAnimation[ANIMATIONS_ARRAY_CAPACITY];
        mGlRandomGenerator = new GLAnimation.GlRandomGenerator();
        mRotationSensorValues = new float[]{0, 0, 0};
    }

    public void setRotationSensorValues(float[] vector) {
        mRotationSensorValues = vector;
    }

    public void onPreferenceChanged(int texturePref, int countPref) {
        PreferenceSelector preferenceSelector = new PreferenceSelector();

        if (preferenceSelector.getMoneyTexture(texturePref) != mMoneyTextureUnitId) {
            mMoneyTextureUnitId = preferenceSelector.getMoneyTexture(texturePref);
            mBgTextureUnitId = preferenceSelector.getBgTexture(texturePref);
            onCreateModels();
        }

        if (preferenceSelector.getMoneyCount(countPref) != mMoneyCount) {
            mMoneyCount = preferenceSelector.getMoneyCount(countPref);
            mGlBufferedModels = new GlModels.GlBufferedModel[mMoneyCount + 1];
            mFrameAnimations = new GLAnimation.GlFrameAnimation[mMoneyCount + 1];
            onCreateModels();
            onCreateAnimations();
        }
    }

    @Override
    protected void onConfigureEnvironment() {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0f, 1f, 1f, 0f);
    }

    @Override
    public void onCreateProgram() {
        String vertexShaderSource = mGlResource.getGlResourceString(R.raw.scene_vertex_shader);
        String fragmentShaderSource = mGlResource.getGlResourceString(R.raw.scene_fragment_shader);

        int fragmentShaderId = mGlShaders.createFragmentShader(fragmentShaderSource);
        int vertexShaderId = mGlShaders.createVertexShader(vertexShaderSource);

        mProgramId = mGlProgram.createProgram(vertexShaderId, fragmentShaderId);

        // Important! need to be called before setting any uniforms
        mGlProgram.useProgram(mProgramId);
    }

    @Override
    public void onCreateTextures() {
        Bitmap bgDollarTextureSource = mGlResource.getGlResourceBitmap(context, R.drawable.gl_texture_bg_dollar,
                TEXTURES_MAX_WIDTH, TEXTURES_MAX_HEIGHT);
        mGlTextures.create2DTexture(bgDollarTextureSource, GLES20.GL_TEXTURE0);

        Bitmap dollarTextureSource = mGlResource.getGlResourceBitmap(context, R.drawable.gl_texture_dollars,
                TEXTURES_MAX_WIDTH, TEXTURES_MAX_HEIGHT);
        mGlTextures.create2DTexture(dollarTextureSource, GLES20.GL_TEXTURE1);

        Bitmap bgEuroTextureSource = mGlResource.getGlResourceBitmap(context, R.drawable.gl_texture_bg_euro,
                TEXTURES_MAX_WIDTH, TEXTURES_MAX_HEIGHT);
        mGlTextures.create2DTexture(bgEuroTextureSource, GLES20.GL_TEXTURE2);

        Bitmap euroTextureSource = mGlResource.getGlResourceBitmap(context, R.drawable.gl_texture_euros,
                TEXTURES_MAX_WIDTH, TEXTURES_MAX_HEIGHT);
        mGlTextures.create2DTexture(euroTextureSource, GLES20.GL_TEXTURE3);
    }

    @Override
    public void onCreateDrawer() {
        mGlElementsDrawer = new GLElementsDrawer.GlTexturedElementsDrawer(
                mProgramId, PERSPECTIVE_ANGLE, PERSPECTIVE_NEAR_PLANE,
                PERSPECTIVE_FAR_PLANE, PERSPECTIVE_ASPECT_RATIO);
    }

    @Override
    public void onCreateModels() {
        String objModelSource = mGlResource.getGlResourceString(R.raw.money_two_sided_texture);
        GlModels.GlObjModel objModel = mGlModels.createGlObjModel(objModelSource);
        byte[] indicesByte = new byte[objModel.getIndices().length];

        for (int i = 0; i < objModel.getIndices().length; i++) {
            indicesByte[i] = (byte) objModel.getIndices()[i];
        }

        String objCrumpledModelSource = mGlResource.getGlResourceString(R.raw.money_two_sided_texture_crampled);
        GlModels.GlObjModel objCrumpledModel = mGlModels.createGlObjModel(objCrumpledModelSource);
        byte[] crumpledIndicesByte = new byte[objCrumpledModel.getIndices().length];

        for (int i = 0; i < objCrumpledModel.getIndices().length; i++) {
            crumpledIndicesByte[i] = (byte) objCrumpledModel.getIndices()[i];
        }

        mGlBufferedModels[0] = mGlModels.createGlBufferedModel(mGlTools, mGlResource,
                R.raw.bg_vertices, R.raw.bg_textures, R.raw.bg_indices, mBgTextureUnitId);

        for (int i = 1; i < mGlBufferedModels.length; i++) {
            if (i % 2 == 0) {
                mGlBufferedModels[i] = mGlModels.createGlBufferedModel(objModel.getVertices(),
                        objCrumpledModel.getTextures(), crumpledIndicesByte, mGlTools, mMoneyTextureUnitId);
            } else {
                mGlBufferedModels[i] = mGlModels.createGlBufferedModel(objModel.getVertices(),
                        objModel.getTextures(), indicesByte, mGlTools, mMoneyTextureUnitId);
            }

            if (i < mGlBufferedModels.length / 2) {

                // Generate and stock model initial
                // transition on Z for left group
                mGlRandomGenerator.stockRandomFloat(i, 0,
                        LEFT_GROUP_RANDOM_TRANS_Z_MIN,
                        LEFT_GROUP_RANDOM_TRANS_Z_MAX);
            } else {

                // Generate and stock model initial
                // transition on Z for right group
                mGlRandomGenerator.stockRandomFloat(i, 0,
                        RIGHT_GROUP_RANDOM_TRANS_Z_MIN,
                        RIGHT_GROUP_RANDOM_TRANS_Z_MAX);
            }
        }
    }

    @Override
    public void onCreateAnimations() {
        for (int i = 0; i < mFrameAnimations.length; i++) {

            float[][] floatMatrix;
            if (i % 3 == 0) {
                floatMatrix = mGlResource.getGlResourceFloatMatrix(R.raw.animation_rotation_two_axis);
            } else if (i % 2 == 0) {
                floatMatrix = mGlResource.getGlResourceFloatMatrix(R.raw.animation_rotation_clockwise);
            } else {
                floatMatrix = mGlResource.getGlResourceFloatMatrix(R.raw.animation_rotation_counterclockwise);
            }

            mFrameAnimations[i] = new GLAnimation.GlFrameAnimation(floatMatrix, i);

            int animationDelay = Math.round(mGlRandomGenerator.getRandomFloat(0, 800));
            mFrameAnimations[i].setDelay(animationDelay);

            mFrameAnimations[i].setFinishListener(this);
        }
    }

    @Override
    public void onGlAnimationFinished(int id) {
        if (id < mGlBufferedModels.length / 2) {

            // Generate and stock model initial
            // transition Z for left group
            mGlRandomGenerator.stockRandomFloat(id, 0,
                    LEFT_GROUP_RANDOM_TRANS_Z_MIN,
                    LEFT_GROUP_RANDOM_TRANS_Z_MAX);
        } else {

            // Generate and stock model initial
            // transition Z for right group
            mGlRandomGenerator.stockRandomFloat(id, 0,
                    RIGHT_GROUP_RANDOM_TRANS_Z_MIN,
                    RIGHT_GROUP_RANDOM_TRANS_Z_MAX);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mGlElementsDrawer.applyCameraMatrix(
                mRotationSensorValues[GyroscopeSensorManager.GYROSCOPE_Y] / GYROSCOPE_VALUE_DIVIDER,
                CAMERA_INITIAL_Y,
                mRotationSensorValues[GyroscopeSensorManager.GYROSCOPE_X] / GYROSCOPE_VALUE_DIVIDER,
                mRotationSensorValues[GyroscopeSensorManager.GYROSCOPE_Y] / GYROSCOPE_VALUE_DIVIDER,
                CAMERA_INITIAL_DIR_Y,
                mRotationSensorValues[GyroscopeSensorManager.GYROSCOPE_X] / GYROSCOPE_VALUE_DIVIDER - 1f,
                CAMERA_INITIAL_ORIENT_VECTOR);

        mGlElementsDrawer.drawBufferedModel(mGlBufferedModels[0]);

        for (int i = 1; i < mGlBufferedModels.length; i++) {
            mGlBufferedModels[i].applyTransparency(
                    mFrameAnimations[i].getFrame()[6]);

            // First, apply model initial transition on X and Z
            mGlBufferedModels[i].applyTransition(
                    i < mGlBufferedModels.length / 2 ? LEFT_GROUP_TRANS_X : RIGHT_GROUP_TRANS_X,
                    0,
                    mGlRandomGenerator.getStockedFloat(i, 0),
                    true
            );

            // Second, apply animation transition
            mGlBufferedModels[i].applyTransition(
                    mFrameAnimations[i].getFrame()[0],
                    mFrameAnimations[i].getFrame()[2],
                    mFrameAnimations[i].getFrame()[1],
                    false);

            // Apply animation rotation
            mGlBufferedModels[i].applyRotation(
                    mFrameAnimations[i].getFrame()[3],
                    mFrameAnimations[i].getFrame()[5],
                    mFrameAnimations[i].getFrame()[4],
                    true);

            mGlElementsDrawer.drawBufferedModel(mGlBufferedModels[i]);

            mFrameAnimations[i].nextFrame();
        }
    }

    // This runnable is responsible for setting rotation vector
    // came from another thread. So to set variables in this thread (GlThread)
    // we need to use inter process communication such us queueEvent() method of GlSurfaceView
    public static class RotationVectorSetter implements Runnable {

        private float[] mVector;
        private GlViewRenderer mRenderer;

        public RotationVectorSetter(float[] vector, GlViewRenderer renderer) {
            mVector = vector;
            mRenderer = renderer;
        }

        @Override
        public void run() {
            mRenderer.setRotationSensorValues(mVector);
        }
    }

    // This runnable is responsible for setting preferences
    // for this renderer object in GlThread
    public static class PreferencesSetter implements Runnable {

        private int mTexture;
        private int mCount;
        private GlViewRenderer mRenderer;

        public PreferencesSetter(int count, int texture, GlViewRenderer renderer) {
            mCount = count;
            mTexture = texture;
            mRenderer = renderer;
        }

        @Override
        public void run() {
            mRenderer.onPreferenceChanged(mTexture, mCount);
        }
    }

    // This class is responsible for choosing correspond preference
    // from incoming constants
    public static class PreferenceSelector {

        private static final int MONEY_COUNT_LITTLE = 4;
        private static final int MONEY_COUNT_MIDDLE = 6;
        private static final int MONEY_COUNT_MANY = 10;

        private static final int MONEY_SPEED_SLOW = 4;
        private static final int MONEY_SPEED_MIDDLE = 6;
        private static final int MONEY_SPEED_FAST = 10;

        private static final int MONEY_TEXTURE_DOLLAR_BG = 0;
        private static final int MONEY_TEXTURE_DOLLAR = 1;
        private static final int MONEY_TEXTURE_EURO_BG = 2;
        private static final int MONEY_TEXTURE_EURO = 3;

        public int getMoneyTexture(int texture) {
            switch (texture) {
                case 0:
                    return MONEY_TEXTURE_DOLLAR;
                case 1:
                    return MONEY_TEXTURE_EURO;
                default:
                    throw new IllegalArgumentException(
                            "Illegal preference type for money count");
            }
        }

        public int getBgTexture(int texture) {
            switch (texture) {
                case 0:
                    return MONEY_TEXTURE_DOLLAR_BG;
                case 1:
                    return MONEY_TEXTURE_EURO_BG;
                default:
                    throw new IllegalArgumentException(
                            "Illegal preference type for money count");
            }
        }

        public int getMoneyCount(int countPreference) {
            switch (countPreference) {
                case 0:
                    return MONEY_COUNT_LITTLE;
                case 1:
                    return MONEY_COUNT_MIDDLE;
                case 2:
                    return MONEY_COUNT_MANY;
                default:
                    throw new IllegalArgumentException(
                            "Illegal preference type for money count");
            }
        }

        public int getMoneySpeed(int speedPreference) {
            switch (speedPreference) {
                case 0:
                    return MONEY_SPEED_SLOW;
                case 1:
                    return MONEY_SPEED_MIDDLE;
                case 2:
                    return MONEY_SPEED_FAST;
                default:
                    throw new IllegalArgumentException(
                            "Illegal preference type for money speed");
            }
        }
    }
}
