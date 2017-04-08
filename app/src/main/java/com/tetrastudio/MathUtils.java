package com.tetrastudio;

import java.util.Collection;
import java.util.List;


public class MathUtils {
    public static float ALPHA = 0.25f;

    public static float max(Collection<float[]> list, int elementId) {
        float max = Float.MIN_VALUE;
        for (float[] array : list) {
            if (array[elementId] > max) {
                max = array[elementId];
            }
        }

        return max;
    }

    public static float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public static float getOverallSlope(List<float[]> input, int index) {
        int slopeDirection = 0;
        if (input == null) return 0;
        for (int i = 0; i < input.size()-1; i++) {
            float currentInput = input.get(i)[index];
            float nextInput = input.get(i+1)[index];
            if (currentInput - nextInput > 0) {
                slopeDirection--;
            } else if (currentInput - nextInput < 0) {
                slopeDirection++;
            }
        }
        return Math.signum(slopeDirection);
    }
}
