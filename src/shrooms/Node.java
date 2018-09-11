package shrooms;

import java.util.HashMap;

public class Node {
    public int depth;
    public String name;
    public boolean leaf;
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
}
