package ru.cnv.opengl.features;

import android.opengl.Matrix;
import android.support.annotation.RawRes;
import android.text.TextUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import ru.cnv.opengl.R;

/**
 * Created by cnv on 25.05.2016.
 */
public class GlModels {

    public GlObjModel createGlObjModel(String objFileString) {
        ArrayList<String> verticesLineList = createVerticesLineList(objFileString);
        ArrayList<String> texturesLineList = createTexturesLineList(objFileString);
        ArrayList<String> fragmentsLineList = createFragmentLineList(objFileString);

        String[] orderedTexturesLineArray = new String[verticesLineList.size()];
        ArrayList<Integer> orderedIndicesArray = new ArrayList<>(fragmentsLineList.size());
        for (String fragmentLine : fragmentsLineList) {
            String[] fragmentLineValues = fragmentLine.split(" ");
            for (String fragmentLineValue : fragmentLineValues) {
                if (fragmentLineValue.contains("f")) continue;
                String[] fragmentValueComponents = fragmentLineValue.split("/");
                int fragmentValueIndex = Integer.valueOf(fragmentValueComponents[0]);
                int fragmentValueTexture = Integer.valueOf(fragmentValueComponents[1]);
                orderedTexturesLineArray[fragmentValueIndex - 1] = texturesLineList.get(fragmentValueTexture - 1);
                orderedIndicesArray.add(fragmentValueIndex);
            }
        }

        float[] floatVerticesArray = new float[verticesLineList.size() * 3];
        int arrayIndex = 0;
        for (String verticesLine : verticesLineList) {
            String[] verticesLineValues = verticesLine.split(" ");
            for (String verticesLineValue : verticesLineValues) {
                if (verticesLineValue.contains("v") || TextUtils.isEmpty(verticesLineValue))
                    continue;
                floatVerticesArray[arrayIndex] = Float.valueOf(verticesLineValue);
                arrayIndex++;
            }
        }

        float[] floatTexturesArray = new float[orderedTexturesLineArray.length * 2];
        arrayIndex = 0;
        for (String textureLine : orderedTexturesLineArray) {
            String[] textureLineValues = textureLine.split(" ");
            for (String textureLineValue : textureLineValues) {
                if (textureLineValue.contains("vt") || TextUtils.isEmpty(textureLineValue))
                    continue;
                float floatValue = Float.valueOf(textureLineValue);
                if (floatValue != 0f) {
                    floatTexturesArray[arrayIndex] = floatValue;
                    arrayIndex++;
                }
            }
        }

        short[] shortIndicesArray = new short[orderedIndicesArray.size()];
        for (int i = 0; i < orderedIndicesArray.size(); i++) {
            shortIndicesArray[i] = Integer.valueOf(orderedIndicesArray.get(i) - 1).shortValue();
        }

        return new GlObjModel(floatVerticesArray, floatTexturesArray, shortIndicesArray);
    }

    private ArrayList<String> createVerticesLineList(String objString) {
        ArrayList<String> verticesLineList = new ArrayList<>();
        String[] lines = objString.split("\r\n");
        String linePrefixString;
        for (String line : lines) {
            if (line.length() < 3) continue;
            linePrefixString = line.substring(0, 2);
            if (linePrefixString.equals("v ")) {
                verticesLineList.add(line);
            }
        }
        return verticesLineList;
    }

    private ArrayList<String> createTexturesLineList(String objString) {
        ArrayList<String> texturesLineList = new ArrayList<>();
        String[] lines = objString.split("\r\n");
        String linePrefixString;
        for (String line : lines) {
            if (line.length() < 3) continue;
            linePrefixString = line.substring(0, 2);
            if (linePrefixString.equals("vt")) {
                texturesLineList.add(line);
            }
        }
        return texturesLineList;
    }

    private ArrayList<String> createFragmentLineList(String objString) {
        ArrayList<String> fragmentLineList = new ArrayList<>();
        String[] lines = objString.split("\r\n");
        String linePrefixString;
        for (String line : lines) {
            if (line.length() < 3) continue;
            linePrefixString = line.substring(0, 2);
            if (linePrefixString.equals("f ")) {
                fragmentLineList.add(line);
            }
        }
        return fragmentLineList;
    }

    public GlBufferedModel createGlBufferedModel(
            GlTools glTools, GlResources glResources, @RawRes int verticesResource,
            @RawRes int texturesResource, @RawRes int indicesResource, int glTexturesUnitId) {

        float[] verticesFloat = glResources.getGlResourceFloatArray(verticesResource);
        FloatBuffer verticesBuffer = glTools.convertToNativeBuffer(verticesFloat);

        float[] texturesFloat = glResources.getGlResourceFloatArray(texturesResource);
        FloatBuffer texturesBuffer = glTools.convertToNativeBuffer(texturesFloat);

        byte[] indicesByte = glResources.getGlResourceByteArray(indicesResource);
        ByteBuffer indicesBuffer = glTools.convertToNativeBuffer(indicesByte);

        return new GlBufferedModel(verticesBuffer, indicesBuffer, texturesBuffer, indicesByte.length, glTexturesUnitId);
    }

    public GlBufferedModel createGlBufferedModel(
            float[] verticesFloat, float[] texturesFloat, byte[] indicesByte, GlTools glTools, int glTextureUnitId) {

        FloatBuffer verticesBuffer = glTools.convertToNativeBuffer(verticesFloat);
        FloatBuffer texturesBuffer = glTools.convertToNativeBuffer(texturesFloat);
        ByteBuffer indicesBuffer = glTools.convertToNativeBuffer(indicesByte);

        return new GlBufferedModel(verticesBuffer, indicesBuffer,
                texturesBuffer, indicesByte.length, glTextureUnitId);
    }

    public static class GlObjModel {

        private float[] vertices;
        private float[] textures;
        private short[] indices;

        public GlObjModel(float[] vertices, float[] textures, short[] indices) {
            this.vertices = vertices;
            this.textures = textures;
            this.indices = indices;
        }

        public float[] getVertices() {
            return vertices;
        }

        public float[] getTextures() {
            return textures;
        }

        public short[] getIndices() {
            return indices;
        }
    }

    public static class GlBufferedModel {

        private FloatBuffer verticesBuffer;
        private ByteBuffer indicesBuffer;
        private FloatBuffer texturesBuffer;

        private float[] modelMatrix;
        private float[] translationMatrix;
        private float[] rotationMatrix;
        private float[] scaleMatrix;

        private int indicesLength;
        private int glTextureUnitId;
        private float transparency;

        public GlBufferedModel(FloatBuffer verticesBuffer, ByteBuffer indicesBuffer,
                               FloatBuffer texturesBuffer, int indicesLength, int glTextureUnitId) {
            this.verticesBuffer = verticesBuffer;
            this.indicesBuffer = indicesBuffer;
            this.texturesBuffer = texturesBuffer;
            this.indicesLength = indicesLength;
            this.glTextureUnitId = glTextureUnitId;

            modelMatrix = new float[16];
            Matrix.setIdentityM(modelMatrix, 0);

            translationMatrix = new float[16];
            Matrix.setIdentityM(translationMatrix, 0);

            rotationMatrix = new float[16];
            Matrix.setIdentityM(rotationMatrix, 0);

            scaleMatrix = new float[16];
            Matrix.setIdentityM(scaleMatrix, 0);

            transparency = 1;
        }

        public void setTextureUnitId(int textureUnitId) {
            glTextureUnitId = textureUnitId;
        }

        public FloatBuffer getVerticesBuffer() {
            return verticesBuffer;
        }

        public ByteBuffer getIndicesBuffer() {
            return indicesBuffer;
        }

        public FloatBuffer getTexturesBuffer() {
            return texturesBuffer;
        }

        public int getIndicesLength() {
            return indicesLength;
        }

        public void setIndicesLength(int indicesLength) {
            this.indicesLength = indicesLength;
        }

        public int getGlTextureUnitId() {
            return glTextureUnitId;
        }

        public float[] getModelMatrix() {
            return modelMatrix;
        }

        public float getTransparency() {
            return transparency;
        }

        public void applyTransparency(float transparency) {
            this.transparency = transparency;
        }

        public void applyTransition(float transitionX, float transiztionY, float transitionZ, boolean resetPrevious) {
            if (resetPrevious) Matrix.setIdentityM(translationMatrix, 0);
            if (transitionX != 0 || transiztionY != 0 || transitionZ != 0) {
                Matrix.translateM(translationMatrix, 0, transitionX, transiztionY, transitionZ);
            }
            applyResultTransformation();
        }

        public void applyRotation(float rotationAngle, float rotationX, float rotationY, float rotationZ, boolean resetPrevious) {
            if (resetPrevious) Matrix.setIdentityM(rotationMatrix, 0);
            if (rotationAngle != 0) {
                Matrix.rotateM(rotationMatrix, 0, rotationAngle, rotationX, rotationY, rotationZ);
            }
            applyResultTransformation();
        }

        public void applyRotation(float x, float y, float z, boolean resetPrevious) {
            if (resetPrevious) Matrix.setIdentityM(rotationMatrix, 0);
            if (x != 0 || y != 0 || z != 0) {
                GlMath.setRotateEulerM(rotationMatrix, 0, x, y, z);
            }
            applyResultTransformation();
        }

        public void applyScale(float scaleX, float scaleY, float scaleZ, boolean resetPrevious) {
            if (resetPrevious) Matrix.setIdentityM(scaleMatrix, 0);
            if (scaleX != 0 || scaleY != 0 || scaleZ != 0) {
                Matrix.scaleM(scaleMatrix, 0, scaleX, scaleY, scaleZ);
            }
            applyResultTransformation();
        }

        private void applyResultTransformation() {
            float[] translationAndRotationMatrix = new float[16];
            Matrix.multiplyMM(translationAndRotationMatrix, 0, translationMatrix, 0, rotationMatrix, 0);
            Matrix.multiplyMM(modelMatrix, 0, scaleMatrix, 0, translationAndRotationMatrix, 0);
        }
    }
}
