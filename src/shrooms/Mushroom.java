package shrooms;

import java.util.ArrayList;
import java.util.HashMap;

public class Mushroom
{
	public static ArrayList<String> featureList;
	public HashMap<String, String> atts;
	
	public Mushroom(String[] features)
	{
		atts = new HashMap<String, String>();
		
		int i = 0;
		for(String each : features)
		{
			atts.put(featureList.get(i), each);
			i++;
		}
	}
	
	public Mushroom(String features)
	{
		this(features.split(","));
	}

	public String getAtt(String feature)
	{
		return atts.get(feature);
	}

	@Override
	public String toString()
	{
		String ret = "Shroom:\n";
		for(String feat : featureList)
		{
			ret += feat + ": " + atts.get(feat) + "\n";
		}
		
		return ret;
	}
}
