package cn.sissors.blendoku;

import java.awt.*;

/**
 * @author zyz
 * @version 2019-05-11
 */
public class ColorRGB {
    private int r;
    private int g;
    private int b;

    public ColorRGB(int sRGB) {
        Color color = new Color(sRGB);
        this.r = color.getRed();
        this.g = color.getGreen();
        this.b = color.getBlue();
    }

    public ColorRGB(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

    public int getG() {
        return g;
    }

    public void setG(int g) {
        this.g = g;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    public boolean isGrey() {
        return (r == g) && (r == b);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ColorRGB) {
            ColorRGB other = (ColorRGB) obj;
            return other.r == this.r && other.g == this.g && other.b == this.b;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (r << 16) + (g << 8) + b;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d)", r, g, b);
    }

    public static ColorRGB clone(ColorRGB other) {
        return other != null ? new ColorRGB(other.getR(), other.getG(), other.getB()) : null;
    }
}
