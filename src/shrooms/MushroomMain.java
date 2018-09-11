package shrooms;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class MushroomMain {
    //The list of shrooms.  Constantly queried.
    private static ArrayList<Mushroom> allShrooms;

    //A list of features mapped to possible answers
    private static HashMap<String, ArrayList<String>> featureList;


    private static boolean DEBUG = true;


    public static void main(String[] args) {

        createShrooms("src/train.csv");

        ArrayList<Mushroom> shrooms = new ArrayList<>();

        for (Mushroom mush : allShrooms) {

            //Grab each unique value for each feature, and add it to featureList
            for (String feat : Mushroom.featureList) {
                String att = mush.getAtt(feat);

                if (!featureList.get(feat).contains(att)) {
                    featureList.get(feat).add(att);
                }
            }

            if (DEBUG)
                System.out.println(mush);
        }

        HashSet<String> feats = new HashSet<>(featureList.keySet());
        Node root = ID3(shrooms, feats, 1);

    }

    private static Node ID3(ArrayList<Mushroom> shrooms, HashSet<String> features, int depth) {
        //If all shrooms have the same label, return a leaf with that label
        String sameyLabel = checkAllSameLabel(shrooms);
        if (sameyLabel != null) {
            return new Node(sameyLabel, depth, true);
        }


        //If this is as far as the features go, this node is a leaf using the most common label.
        if (features.isEmpty()) {
            String commonLabel = findCommonLabel(shrooms);
            return new Node(commonLabel, depth, true);
        }

        //Determine best feature to discriminate by at this point
        //Using InfoGain
        String bestFeature = "";
        
        double maxGain = 0;
        for (String eachFeat : features) {
            double infoGain = infoGain(eachFeat, "label", shrooms);

            if (infoGain > maxGain) {
                maxGain = infoGain;
                bestFeature = eachFeat;
            }
        }


        //Remove the used feature from later branches
        HashSet<String> nextFeatures = new HashSet<>(features);
        nextFeatures.remove(bestFeature);

        Node thisNode = new Node(bestFeature, depth, false);

        //For each value of the feature (e.g. sweet, spicy, mild)
        //enact again ID3 using only the surviving shrooms

        for (String nextAtt : featureList.get(bestFeature)) {
            //Construct Sv (subset of shrooms that have the specified value/attribute of the bestFeature)
            ArrayList<Mushroom> nextShrooms = new ArrayList<Mushroom>();

            for (Mushroom shroom : shrooms) {
                if (shroom.getAtt(bestFeature).equals(nextAtt)) {
                    nextShrooms.add(shroom);
                }
            }

            Node nextNode;
            if (nextShrooms.size() == 0) {
                String commonLabel = findCommonLabel(shrooms);
                nextNode = new Node(commonLabel, depth + 1, true);
            } else {
                nextNode = ID3(nextShrooms, nextFeatures, depth + 1);
            }

            thisNode.add(nextAtt, nextNode);

        }

        return thisNode;
    }

    private static double infoGain(String feature, String label, ArrayList<Mushroom> shrooms) {
        double bigEntropy = entropy(label, shrooms);

        ArrayList<String> labelValues = new ArrayList<>(featureList.get(label));

        double expectedEntropy = 0;

        for (String att : featureList.get(feature)) {
            //get the subset of shrooms with each value of the feature
            ArrayList<Mushroom> nextShrooms = new ArrayList<Mushroom>();
            for (Mushroom shroom : shrooms) {
                if (shroom.getAtt(feature).equals(att)) {
                    nextShrooms.add(shroom);
                }
            }

            double thisEntropy = entropy(label, nextShrooms);

            expectedEntropy += ((nextShrooms.size() / shrooms.size()) * thisEntropy);


        }

        return bigEntropy - expectedEntropy;
    }

    private static double entropy(String label, ArrayList<Mushroom> shrooms) {
        ArrayList<String> labelValues = new ArrayList<>(featureList.get(label));
        int[] valueCounts = new int[labelValues.size()];
        Arrays.fill(valueCounts, 0);

        double total = shrooms.size();

        for (Mushroom shroom : shrooms) {
            String value = shroom.getAtt(label);
            valueCounts[labelValues.indexOf(value)]++;
        }


        double entropy = 0;

        for (double eachP : valueCounts) {
            entropy -= ((eachP / total) * (logBase2(eachP)));
        }

        return entropy;
    }

    private static double logBase2(double input) {
        return Math.log(input) / Math.log(2);
    }


    private static String findCommonLabel(ArrayList<Mushroom> shrooms) {
        HashMap<String, Integer> counters = new HashMap<>();
        String maxLabel = "";

        for (Mushroom mush : shrooms) {
            String label = mush.getAtt("label");

            if (counters.containsKey(label))
                counters.put(label, 1);
            else
                counters.put(label, counters.get(label) + 1);
        }

        int max = 0;

        for (String eachLabel : counters.keySet()) {
            if (counters.get(eachLabel) > max) {
                max = counters.get(eachLabel);
                maxLabel = eachLabel;
            }
        }

        return maxLabel;
    }

    private static String checkAllSameLabel(ArrayList<Mushroom> shrooms) {

        String label = "";

        for (Mushroom shroom : shrooms) {
            if (label.equals("")) {
                label = shroom.getAtt("label");
                continue;
            }

            if (!(shroom.getAtt("label").equals(label)))
                return null;
        }

        return label;
    }


    private static void createShrooms(String fileName) {
        allShrooms = new ArrayList<Mushroom>();

        BufferedReader reader = null;
        String line = "";

        try {
            reader = new BufferedReader(new FileReader(fileName));

            // First, grab the features out.
            line = reader.readLine();
            Mushroom.featureList = new ArrayList<String>();
            featureList = new HashMap<>();
            for (String eachFeat : line.split(",")) {
                Mushroom.featureList.add(eachFeat);
                featureList.put(eachFeat, new ArrayList<String>());
            }


            while ((line = reader.readLine()) != null) {
                Mushroom next = new Mushroom(line);
                allShrooms.add(next);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File " + fileName + " not found.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
