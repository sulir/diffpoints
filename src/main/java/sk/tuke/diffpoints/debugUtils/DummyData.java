package sk.tuke.diffpoints.debugUtils;

import sk.tuke.diffpoints.objectSaving.TreeNode;
import sk.tuke.diffpoints.objectSaving.nodes.ArrayNode;
import sk.tuke.diffpoints.objectSaving.nodes.ObjectNode;
import sk.tuke.diffpoints.objectSaving.nodes.RootNode;
import sk.tuke.diffpoints.objectSaving.nodes.StringNode;

public class DummyData {


    public static TreeNode getDummyObject(int version) {
        RootNode root = new RootNode();
        root.addChild(new StringNode("os", "Windows", null));


        ObjectNode addressObj = new ObjectNode("address", "Address", Randomness.randomInRange(100,999), null);
        addressObj.addChild(new StringNode("street", "Main St", null));
        addressObj.addChild(new StringNode("city", "Springfield", null));

        ArrayNode arrayNode = new ArrayNode("ints", "int", 514, null);
        arrayNode.addChild(new TreeNode("0", "1", null));
        arrayNode.addChild(new TreeNode("1", "2", null));
        arrayNode.addChild(new TreeNode("2", "3", null));
        arrayNode.addChild(new TreeNode("3", "4", null));
        root.addChild(arrayNode);


        ObjectNode job = new ObjectNode("work", "Work", (Randomness.randomInRange(100,999)), null);
        job.addChild(new StringNode("name", "Coder", null));
        for (int i = 0; i < 1; i++) {
            ObjectNode person = new ObjectNode("person" + i, "Person", Randomness.randomInRange(100,999), null);
            if (version == 0) {
                person.addChild(new StringNode("name", "John", null));
                person.addChild(new TreeNode("age", "30", null));
                person.addChild(new ObjectNode("address", null, -1, null));
                person.addChild(new StringNode("pet", null, null));
                person.addChild(job);
            } else if (version == 1) {
                person.addChild(new StringNode("name", "Smith", null));
                person.addChild(new TreeNode("age", null, null));
                person.addChild(addressObj);
                person.addChild(new StringNode("pet", null, null));
                person.addChild(job);
            } else {
                person.addChild(new StringNode("name", "John Smith", null));
                person.addChild(new TreeNode("age", "32", null));
                person.addChild(addressObj);
                person.addChild(new StringNode("pet", "dog", null));
                person.addChild(new ObjectNode("work", null, -1, null));
            }
            root.addChild(person);
        }


        root.addChild(new StringNode("day", "Friday", null));

        if (version == 0)
            root.addChild(new TreeNode("minute", "30", null));
        else
            root.addChild(new TreeNode("minute", "32", null));

        return root;
    }

}
