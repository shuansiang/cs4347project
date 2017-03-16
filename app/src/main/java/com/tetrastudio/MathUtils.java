package com.tetrastudio;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Games on 15/3/2017.
 */

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
}
