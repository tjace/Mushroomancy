package shrooms;

import java.util.HashMap;

public class Node {
    public int depth;
    public String name;
    private boolean leaf;
    private HashMap<String, Node> children;

    public Node(String _name, int _depth, boolean _leaf) {

        leaf = _leaf;
        name = _name;
        depth = _depth;

        children = new HashMap<>();
    }

    public void add(String check, Node child) {
        children.put(check, child);
    }

    public boolean isLeaf() {
        return leaf;
    }

    public Node followPath(String path) {
        return children.get(path);
    }

    public int findMaxDepth()
    {
        if (leaf)
            return depth;

        int max = 0;
        for (Node each : children.values())
        {
            int eachDepth = each.findMaxDepth();

            if(eachDepth > max)
                max = eachDepth;
        }

        return max;

    }
}
