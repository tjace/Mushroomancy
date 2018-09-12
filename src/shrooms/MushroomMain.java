package shrooms;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MushroomMain {
    //The list of shrooms.  Constantly queried.

    //A list of features mapped to possible answers
    private static HashMap<String, ArrayList<String>> featureList;


    private static boolean DEBUG = true;


    public static void main(String[] args) {

        //createShrooms("src/train.csv");
        ArrayList<Mushroom> trainShrooms = createShrooms("src/train.csv", true);


        ArrayList<Mushroom> shrooms = new ArrayList<>(trainShrooms);

        //Grab each unique value for each feature, and add it to featureList
        fillFeatures(trainShrooms);

        HashSet<String> feats = new HashSet<>(featureList.keySet());
        feats.remove("label");

        Node root = ID3(shrooms, feats, 1, -1);

        if (DEBUG) System.out.println(root.name);

        int maxDepth = root.findMaxDepth();

        double error = shroomError(root, "src/test.csv");
        System.out.println("Error: " + error);
        System.out.println("Max depth: " + maxDepth);


        //Now, part 2: 5-fold cross-validation with various levels of depth.


        ArrayList<String> files = new ArrayList<>();
        files.add("src/fold1.csv");
        files.add("src/fold2.csv");
        files.add("src/fold3.csv");
        files.add("src/fold4.csv");
        files.add("src/fold5.csv");

        ArrayList<Integer> depths = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            depths.add(i);
        }
        depths.add(10);
        depths.add(15);

        int bestDepth = findBestDepth(files, depths);

        for (int i = 1; i <= 5; i++) {
            enactKFold(files, i);
        }
        enactKFold(files, 10);
        enactKFold(files, 15);


    }

    private static int findBestDepth(List<String> fileNames, List<Integer> depths) {
        int bestDepth = 0;
        double leastError = -1.0;

        for (int i = 0; i < depths.size(); i++) {
            int depth = depths.get(i);
            double avgError = enactKFold(fileNames, depth);

            if (avgError < leastError || leastError == -1.0) {
                bestDepth = depth;
                leastError = avgError;
            }
        }

        System.out.println("Best depth is " + bestDepth + "; average error was " + leastError);

        return bestDepth;
    }

    /**
     * @param fileNames
     * @param maxDepth
     * @return the
     */
    private static double enactKFold(List<String> fileNames, int maxDepth) {
        int k = fileNames.size();
        double totalError = 0.0;
        ArrayList<Double> errors = new ArrayList<Double>(k);

        for (int i = 0; i < k; i++) {
            //Create the shrooms from all but entry i
            ArrayList<String> filesMinusOne = new ArrayList<>();
            String outcast = "";

            for (int j = 0; j < k; j++) {
                if (j != i)
                    filesMinusOne.add(fileNames.get(j));
                else
                    outcast = fileNames.get(j);
            }

            ArrayList<Mushroom> shrooms = createShrooms(filesMinusOne);

            fillFeatures(shrooms);
            HashSet<String> feats = new HashSet<>(featureList.keySet());
            feats.remove("label");


            //Train
            Node root = ID3(shrooms, feats, 1, maxDepth);


            //Test
            double error = shroomError(root, fileNames.get(i));


            //Print the data
            System.out.println("\n~~~~~~~");
            System.out.println("outcast: " + outcast + "(max depth: " + maxDepth + ")");
            System.out.println("Error: " + error);

            errors.add(error);
            totalError += error;
        }

        double avgError = totalError / (double) k;
        double errorDeviation = findStandardDeviation(avgError, errors);

        System.out.println("\n##########################");
        System.out.println("Max Depth: " + maxDepth);
        System.out.println("Average Error: " + avgError);
        System.out.println("Standard Deviation: " + errorDeviation + "\n\n\n");

        return avgError;
    }


    private static double findStandardDeviation(double avgError, ArrayList<Double> errors) {
        double squaresSum = 0;

        for (double err : errors) {
            double x = (err - avgError);
            x = x * x;

            squaresSum += x;
        }

        double ret = squaresSum / ((double) (errors.size() - 1));
        ret = Math.sqrt(ret);

        return ret;
    }


    private static void fillFeatures(ArrayList<Mushroom> shrooms) {
        for (Mushroom mush : shrooms) {

            for (String feat : Mushroom.featureList) {
                String att = mush.getAtt(feat);

                if (!featureList.get(feat).contains(att)) {
                    featureList.get(feat).add(att);
                }
            }
        }
    }


    /**
     * if maxDepth == -1, there is no max.
     */
    private static Node ID3(ArrayList<Mushroom> shrooms, HashSet<String> features, int depth, int maxDepth) {
        //If all shrooms have the same label, return a leaf with that label
        String sameyLabel = checkAllSameLabel(shrooms);
        if (sameyLabel != null) {
            return new Node(sameyLabel, depth, true);
        }

        //If this is the lowest a node can be, it'll have to be a leaf.
        if (maxDepth != -1 && depth == maxDepth) {
            String commonLabel = findCommonLabel(shrooms);
            return new Node(commonLabel, depth, true);
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
                nextNode = ID3(nextShrooms, nextFeatures, depth + 1, maxDepth);
            }

            thisNode.add(nextAtt, nextNode);
        }

        //Set a default node, just in case.
        String defaultLabel = findCommonLabel(shrooms);
        thisNode.add("", new Node(defaultLabel, depth + 1, true));

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

            expectedEntropy += (((double) (nextShrooms.size()) / (double) (shrooms.size())) * thisEntropy);


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
            if (eachP == 0.0)
                continue;

            double proportion = (eachP / total);
            double logResult = logBase2(proportion);
            entropy -= (proportion * logResult);
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
                counters.put(label, counters.get(label) + 1);
            else
                counters.put(label, 1);
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

    private static double shroomError(Node root, String fileName) {
        int fail = 0;

        ArrayList<Mushroom> testShrooms = createShrooms(fileName);

        for (Mushroom shroom : testShrooms) {
            Node currentNode = root;
            String expected = shroom.getAtt("label");

            StringBuilder debug = new StringBuilder();

            while (!currentNode.isLeaf()) {
                String nextPath = shroom.getAtt(currentNode.name);

                //System.out.println(currentNode.name + " ==> " + nextPath);
                //debug.append(currentNode.name).append(": ").append(nextPath).append(" ==> ");
                if (currentNode.has(nextPath))
                    currentNode = currentNode.followPath(nextPath);
                else
                    currentNode = currentNode.followDefaultPath();
            }

            //debug.append(currentNode.name);
            //System.out.println(debug + "");

            if (!expected.equals(currentNode.name))
                fail++;
        }

        double percentFailed = (double) fail / (double) (testShrooms.size());
        return percentFailed;
    }

    /**
     * Creates one array of shrooms from multiple files.
     */
    private static ArrayList<Mushroom> createShrooms(Collection<String> fileNames) {
        ArrayList<Mushroom> shrooms = new ArrayList<Mushroom>();

        boolean init = true;
        for (String fileName : fileNames) {
            shrooms.addAll(createShrooms(fileName, init));
            init = false;
        }

        return shrooms;
    }


    private static ArrayList<Mushroom> createShrooms(String fileName) {
        return createShrooms(fileName, true);
    }


    private static ArrayList<Mushroom> createShrooms(String fileName, boolean initializeFeatures) {
        ArrayList<Mushroom> shrooms = new ArrayList<Mushroom>();

        BufferedReader reader = null;
        String line = "";

        try {
            reader = new BufferedReader(new FileReader(fileName));

            // First, grab the features out.
            line = reader.readLine();

            if (initializeFeatures) {
                Mushroom.featureList = new ArrayList<String>();
                featureList = new HashMap<>();
                for (String eachFeat : line.split(",")) {
                    Mushroom.featureList.add(eachFeat);
                    featureList.put(eachFeat, new ArrayList<String>());
                }
            }

            while ((line = reader.readLine()) != null) {
                Mushroom next = new Mushroom(line);
                shrooms.add(next);
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

        return shrooms;
    }
}
