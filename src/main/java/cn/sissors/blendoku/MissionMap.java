package cn.sissors.blendoku;

import java.util.List;

/**
 * @author zyz
 * @version 2019-05-11
 */
public class MissionMap {
    public enum Type {
        NULL("NULL"), BLANK("BLANK"), FILLED("FILLED"), FIXED("FIXED");

        private String tag;

        Type(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return tag;
        }
    }

    public static class Coordinate {
        private int x, y;

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public static Coordinate clone(Coordinate other) {
            return new Coordinate(other.x, other.y);
        }
    }

    static class Point {
        private Type type;
        private ColorRGB color;

        public Point(Type type, ColorRGB color) {
            this.type = type;
            this.color = color;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public ColorRGB getColor() {
            return color;
        }

        public void setColor(ColorRGB color) {
            this.color = color;
        }
    }

    private final static Point NULL = new Point(Type.NULL, null);

    private Point[][] points;
    private int width;
    private int height;

    public MissionMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.points = new Point[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                setPoint(i, j, Type.NULL, null);
            }
        }
    }

    public void setPoint(int x, int y, Type type, ColorRGB color) {
        this.points[x][y] = new Point(type, color);
    }

    public Point getPoint(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return NULL;
        }
        return this.points[x][y];
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public static MissionMap clone(MissionMap other) {
        MissionMap missionMap = new MissionMap(other.width, other.height);
        for (int i = 0; i < missionMap.getWidth(); i++) {
            for (int j = 0; j < missionMap.getHeight(); j++) {
                missionMap.setPoint(i, j, other.getPoint(i, j).type, ColorRGB.clone(other.getPoint(i, j).color));
            }
        }
        return missionMap;
    }

    public void print() {
        for (int j = 0; j < this.getHeight(); j++) {
            for (int i = 0; i < this.getWidth(); i++) {
                System.out.print(String.format("%-10s ", this.getPoint(i, j).getType().toString()));
            }
            System.out.println();
            System.out.println();
        }
        System.out.println();
    }

    public void print(List<ColorRGB> candidateColorList) {
        for (int j = 0; j < this.getHeight(); j++) {
            for (int i = 0; i < this.getWidth(); i++) {
                if (this.getPoint(i, j).getType() == Type.FILLED) {
                    for (int k = 0; k < candidateColorList.size(); k++) {
                        if (candidateColorList.get(k).equals(this.getPoint(i, j).getColor())) {
                            System.out.print(String.format("%-10s ", this.getPoint(i, j).getType().toString() + "(" + k + ")"));
                        }
                    }
                } else {
                    System.out.print(String.format("%-10s ", this.getPoint(i, j).getType().toString()));
                }
            }
            System.out.println();
            System.out.println();
        }
        System.out.println();
    }
}
