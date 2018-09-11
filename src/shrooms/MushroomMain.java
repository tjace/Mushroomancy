package shrooms;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MushroomMain {
    private static ArrayList<Mushroom> allShrooms;
    private static HashMap<String, ArrayList<String>> featureList;
    private static boolean DEBUG = true;


    public static void main(String[] args) {

        createShrooms("src/train.csv");

        ArrayList<Mushroom> shrooms = new ArrayList<>();

            for (Mushroom mush : allShrooms) {

                //Grab each unique value for each feature, and add it to featureList
                for (String feat : Mushroom.featureList)
                {
                    String att = mush.getAtt(feat);

                    if (!featureList.get(feat).contains(att))
                    {
                        featureList.get(feat).add(att);
                    }
                }

                if(DEBUG)
                System.out.println(mush);
            }


        Node root = ID3(shrooms);

    }

    public static Node ID3(ArrayList<Mushroom> shrooms, ArrayList<String> features, int depth)
    {
        //Determine best feature to discriminate by at this point
        //Using InfoGain
        String bestFeature = "";

        //For each value of the feature (e.g. sweet, spicy, mild)
        //enact again ID3 using only the surviving allShrooms

        for (String att : featureList.get(bestFeature))
        {
            //Construct Sv
            ArrayList<Mushroom> nextShrooms = new ArrayList<Mushroom>();

            for(Mushroom shroom : shrooms)
            {
                if (shroom.getAtt(bestFeature).equals(att))
                {
                    nextShrooms.add(shroom);
                }
            }

            if (nextShrooms.size() == 0)
            {
                String commonLabel = findCommonLabel(shrooms);
                Node endNode = new Node(depth + 1)
            }
            else{

            }

        }

        return null;
    }


    private static void createShrooms(String fileName) {
        allShrooms = new ArrayList<Mushroom>();

        BufferedReader reader = null;
        String line = "";

        try {
            reader = new BufferedReader(new FileReader(fileName));

            // First, grab the features out.
            line = reader.readLine();
            Mushroom.featureList = new ArrayList<String>(Arrays.asList(line.split(",")));

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
