package cn.sissors.blendoku;

/**
 * @author zyz
 * @version 2019-05-11
 */
public class ImageRGB {
    private int width;
    private int height;
    private ColorRGB[][] colors;

    public ImageRGB(int width, int height) {
        this.width = width;
        this.height = height;
        this.colors = new ColorRGB[width][height];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ColorRGB getColor(int x, int y) {
        return colors[x][y];
    }

    public void setColor(int x, int y, ColorRGB color) {
        colors[x][y] = color;
    }
}
