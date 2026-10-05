package com.naturalinteraction.LEA;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;

import de.darkblue.leaclassifier.model.Classifier;
import de.darkblue.leaclassifier.model.ClassifierTree;
import de.darkblue.leaclassifier.model.ScanArea;
import de.darkblue.lea.model.LEAImplementation;

import java.awt.Point;
import java.io.InputStream;
import java.lang.reflect.Field;

public final class LeaFaceDataLoader {

    private static final String FACE_DATA_RESOURCE = "/faceData.xml";
    private static final String CLASSIFIER_TREE_FIELD = "classifierTree";

    private LeaFaceDataLoader() {
    }

    public static boolean isFaceDataPresent() {
        try (InputStream in = ClassifierTree.class.getResourceAsStream(FACE_DATA_RESOURCE)) {
            return in != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static LEAImplementation createImplementation() throws Exception {
        ClassifierTree tree = loadTree();
        LEAImplementation implementation = new LEAImplementation();
        Field field = LEAImplementation.class.getDeclaredField(CLASSIFIER_TREE_FIELD);
        field.setAccessible(true);
        field.set(implementation, tree);
        return implementation;
    }

    public static ClassifierTree loadTree() throws Exception {
        try (InputStream in = ClassifierTree.class.getResourceAsStream(FACE_DATA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("faceData.xml is not on the classpath");
            }
            XStream xstream = new XStream(new DomDriver());
            xstream.alias("ClassifierTree", ClassifierTree.class);
            xstream.alias("Classifier", Classifier.class);
            xstream.alias("ScanArea", ScanArea.class);
            xstream.registerConverter(new PointConverter());
            Object root = xstream.fromXML(in);
            if (!(root instanceof ClassifierTree)) {
                throw new IllegalStateException("unexpected face data root: " + root);
            }
            return (ClassifierTree) root;
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class PointConverter implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return type == Point.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            Point point = (Point) source;
            writer.startNode("x");
            writer.setValue(Integer.toString(point.x));
            writer.endNode();
            writer.startNode("y");
            writer.setValue(Integer.toString(point.y));
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            int x = 0;
            int y = 0;
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                String name = reader.getNodeName();
                if ("x".equals(name)) {
                    x = parseInt(reader.getValue());
                } else if ("y".equals(name)) {
                    y = parseInt(reader.getValue());
                }
                reader.moveUp();
            }
            return new Point(x, y);
        }

        private static int parseInt(String value) {
            if (value == null) {
                return 0;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
