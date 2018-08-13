package com.melonfishy.fruitsee.utils;

import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by bakafish on 8/8/18.
 *
 */

public class ClassifyUtils {
    public static ArrayList<Integer> findMode(int[] arr) {
        SparseIntArray histogram = new SparseIntArray();
        for (int i : arr) {
            if (histogram.indexOfKey(i) == -1) {
                histogram.put(i, 1);
            } else {
                histogram.put(i, histogram.get(i) + 1);
            }
        }
        ArrayList<Integer> candidates = new ArrayList<>();
        int maxCount = -1;
        for (int i = 0; i < histogram.size(); i++) {
            if (histogram.valueAt(i) > maxCount) {
                maxCount = histogram.valueAt(i);
                candidates = new ArrayList<>();
                candidates.add(histogram.keyAt(i));
            } else if (histogram.valueAt(i) == maxCount) {
                candidates.add(histogram.keyAt(i));
            }
        }
        return candidates;
    }

    public static ArrayList<Integer> findMode(int[] arr, float[] weights, int resultSize) {
        if (arr.length != weights.length) {
            throw new IllegalArgumentException("Data and weights arrays must be of the same length");
        }
        ArrayList<Float> histogram = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            histogram.add(0f);
        }
        for (int i = 0; i < arr.length; i++) {
            histogram.set(arr[i], histogram.get(arr[i]) + weights[i]);
        }
        for (int i = 0; i < 75; i++) {
            Log.d("ClassifyUtils", String.format("Histogram entry %d: %f", i, histogram.get(i)));
        }
        ArrayList<Integer> candidates = new ArrayList<>();
        ArrayList<Float> sorted = new ArrayList<>(histogram);
        Collections.sort(sorted, Collections.reverseOrder());
        for (int i = 0; i < 75; i++) {
            Log.d("ClassifyUtils", String.format("Sorted entry %d: %f", i, sorted.get(i)));
        }
        int index = 0;
        while (candidates.size() < resultSize && index < sorted.size()) {
            for (int i = 0; i < 75; i++) {
                if (!candidates.contains(i) && histogram.get(i).equals(sorted.get(index))) {
                    candidates.add(i);
                    index++;
                    break;
                }
            }
        }
        return candidates;
    }

    public static ArrayList<Float> getHistogram(int[] arr, float[] weights) {
        if (arr.length != weights.length) {
            throw new IllegalArgumentException("Data and weights arrays must be of the same length");
        }
        ArrayList<Float> histogram = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            histogram.add(0f);
        }
        for (int i = 0; i < arr.length; i++) {
            histogram.set(arr[i], histogram.get(arr[i]) + weights[i]);
        }
        return histogram;
    }

    public static boolean contains(ArrayList<Integer> ints, int item) {
        for (int i = 0; i < ints.size(); i++) {
            if (ints.get(i) == item) {
                return true;
            }
        }
        return false;
    }

    public static String typeToString(int type) {
        switch(FruitType.values()[type]) {
            case Apple_Braeburn:
            case Apple_Golden_1:
            case Apple_Golden_2:
            case Apple_Golden_3:
            case Apple_Granny_Smith:
            case Apple_Red_1:
            case Apple_Red_2:
            case Apple_Red_3:
            case Apple_Red_Delicious:
            case Apple_Red_Yellow:
                return "Apple";
            case Apricot:
                return "Apricot";
            case Avocado:
            case Avocado_Ripe:
                return "Avocado";
            case Banana:
            case Banana_Red:
                return "Banana";
            case Cactus_fruit:
                return "Cactus Fruit";
            case Cantaloupe_1:
            case Cantaloupe_2:
                return "Cantaloupe";
            case Carambula:
                return "Starfruit";
            case Cherry_1:
            case Cherry_2:
            case Cherry_Rainier:
            case Cherry_Wax_Black:
            case Cherry_Wax_Red:
            case Cherry_Wax_Yellow:
                return "Cherry";
            case Clementine:
                return "Clementine";
            case Cocos:
                return "Coconut";
            case Dates:
                return "Date";
            case Granadilla:
                return "Grandilla";
            case Grape_Pink:
            case Grape_White:
            case Grape_White_2:
                return "Grape";
            case Grapefruit_Pink:
            case Grapefruit_White:
                return "Grapefruit";
            case Guava:
                return "Guava";
            case Huckleberry:
                return "Huckleberry";
            case Kaki:
                return "Persimmon";
            case Kiwi:
                return "Kiwi";
            case Kumquats:
                return "Kumquats";
            case Lemon:
            case Lemon_Meyer:
                return "Lemon";
            case Limes:
                return "Lime";
            case Lychee:
                return "Lychee";
            case Mandarine:
                return "Mandarin";
            case Mango:
                return "Mango";
            case Maracuja:
                return "Maracuja";
            case Melon_Piel_de_Sapo:
                return "Santa Claus Melon";
            case Mulberry:
                return "Mulberry";
            case Nectarine:
                return "Nectarine";
            case Orange:
                return "Orange";
            case Papaya:
                return "Papaya";
            case Passion_Fruit:
                return "Passion Fruit";
            case Peach:
            case Peach_Flat:
                return "Peach";
            case Pear:
            case Pear_Abate:
            case Pear_Monster:
            case Pear_Williams:
                return "Pear";
            case Pepino:
                return "Pepino";
            case Physalis:
            case Physalis_with_Husk:
                return "Ground Cherry";
            case Pineapple:
            case Pineapple_Mini:
                return "Pineapple";
            case Pitahaya_Red:
                return "Dragonfruit";
            case Plum:
                return "Plum";
            case Pomegranate:
                return "Pomegranate";
            case Quince:
                return "Quince";
            case Rambutan:
                return "Rambutan";
            case Raspberry:
                return "Raspberry";
            case Salak:
                return "Salak";
            case Strawberry:
            case Strawberry_Wedge:
                return "Strawberry";
            case Tamarillo:
                return "Tamarillo";
            case Tangelo:
                return "Tangelo";
            case Walnut:
                return "Walnut";
            default:
                return "Unknown";
        }
    }
}
