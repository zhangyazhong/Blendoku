package cn.sissors.blendoku;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("Duplicates")
public class Blendoku {
    // 截图文件名
    private final static String IMAGE_FILENAME = "11.PNG";
    // 候选区与地图的交界Y值
    private final static int CANDIDATE_BOTTOM_BOUND = 420;
    // 地图与下方提示文字的交界Y值
    private final static int MISSION_MAP_BOTTOM_BOUND = 1360;
    // 色块检验的范围（正负30）
    private final static int COLOR_BLOCK_CHECK_BOUND = 30;
    // 背景黑色的RGB
    private final static ColorRGB BLACK = new ColorRGB(0, 0, 0);
    // 地图中空缺位置灰色的RGB
    private final static ColorRGB GREY = new ColorRGB(61, 61, 61);
    // 背景色色差
    private final static int BACKGROUND_DEVIATION = 100;
    // 灰色色差
    private final static int GREY_DEVIATION = 75;
    // 地图中空白位置的尺寸大小
    private final static int BLANK_BLOCK_SIZE = 130;
    // 地图中缝隙的尺寸大小
    private final static int CHINK_SIZE = 15;
    // 两个颜色的最大偏离值上限
    private final static int MAX_SINGLE_LOSS = 256 * 256 * 256;

    private List<ColorRGB> candidateColorList;
    private Set<ColorRGB> fixedColorSet;
    private MissionMap missionMap;
    private int totalBlanks;
    private boolean[] candidateColorUsed;
    private double bestDeviation;
    private MissionMap bestMap;

    private Blendoku() {
        ImageRGB imageRGB = readImage(IMAGE_FILENAME);
        candidateColorList = fetchCandidateColor(imageRGB);
        fixedColorSet = fetchFixedColor(imageRGB);
        missionMap = fetchMissionMap(imageRGB);
        missionMap.print();
        totalBlanks = 0;
        for (int i = 0; i < missionMap.getWidth(); i++) {
            for (int j = 0; j < missionMap.getHeight(); j++) {
                if (missionMap.getPoint(i, j).getType() == MissionMap.Type.BLANK) {
                    totalBlanks++;
                }
            }
        }
        candidateColorUsed = new boolean[candidateColorList.size()];
        for (int i = 0; i < candidateColorList.size(); i++) {
            candidateColorUsed[i] = false;
        }
        bestDeviation = -1;
        int n = expandMissionMap(missionMap);
        resolveMissionMap(missionMap, n);
        bestMap.print(candidateColorList);
    }

    /**
     * 读入截图文件
     *
     * @param imageName 截图文件名
     * @return 一个包含所有像素sRGB信息的ImageRGB对象
     */
    private ImageRGB readImage(String imageName) {
        ImageRGB imageRGB = null;
        String imagePath = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + imageName.trim();
        try {
            BufferedImage imageFile = ImageIO.read(new File(imagePath));
            int width = imageFile.getWidth();
            int height = imageFile.getHeight();
            imageRGB = new ImageRGB(width, height);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    imageRGB.setColor(i, j, new ColorRGB(imageFile.getRGB(i, j)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageRGB;
    }

    /**
     * 从ImageRGB中，提取待放置的色块颜色
     *
     * @param imageRGB ImageRGB对象
     * @return 一个包含候选色块sRGB的列表
     */
    private List<ColorRGB> fetchCandidateColor(ImageRGB imageRGB) {
        return fetchColorBlock(imageRGB, 0, imageRGB.getWidth(), 0, CANDIDATE_BOTTOM_BOUND);
    }

    /**
     * 从ImageRGB中，提取地图上初始时已经给定的色块颜色
     *
     * @param imageRGB ImageRGB对象
     * @return 一个包含初始放置好的色块sRGB的列表
     */
    private Set<ColorRGB> fetchFixedColor(ImageRGB imageRGB) {
        return Sets.newHashSet(fetchColorBlock(imageRGB, 0, imageRGB.getWidth(), CANDIDATE_BOTTOM_BOUND, MISSION_MAP_BOTTOM_BOUND));
    }

    /**
     * 从ImageRGB中，提取关卡要解决的地图，主体是一个n*m的二维数组
     *
     * @param imageRGB ImageRGB对象
     * @return 一个MissionMap对象，包含关卡的地图
     */
    @SuppressWarnings("Duplicates")
    private MissionMap fetchMissionMap(ImageRGB imageRGB) {
        int leftBound, rightBound, topBound, bottomBound;
        leftBound = rightBound = topBound = bottomBound = -1;
        for (int i = 0; i < imageRGB.getWidth(); i++) {
            for (int j = CANDIDATE_BOTTOM_BOUND; j < MISSION_MAP_BOTTOM_BOUND; j++) {
                if (!imageRGB.getColor(i, j).equals(BLACK)
                        && (imageRGB.getColor(i, j).isGrey() || fixedColorSet.contains(imageRGB.getColor(i, j)))) {
                    leftBound = i;
                    if (fixedColorSet.contains(imageRGB.getColor(i, j))) {
                        leftBound += 7;
                    }
                    break;
                }
            }
            if (leftBound > -1) {
                break;
            }
        }
        for (int i = imageRGB.getWidth() - 1; i >= 0; i--) {
            for (int j = CANDIDATE_BOTTOM_BOUND; j < MISSION_MAP_BOTTOM_BOUND; j++) {
                if (!imageRGB.getColor(i, j).equals(BLACK)
                        && (imageRGB.getColor(i, j).isGrey() || fixedColorSet.contains(imageRGB.getColor(i, j)))) {
                    rightBound = i;
                    if (fixedColorSet.contains(imageRGB.getColor(i, j))) {
                        rightBound -= 6;
                    }
                    break;
                }
            }
            if (rightBound > -1) {
                break;
            }
        }
        for (int j = CANDIDATE_BOTTOM_BOUND; j < MISSION_MAP_BOTTOM_BOUND; j++) {
            for (int i = 0; i < imageRGB.getWidth(); i++) {
                if (!imageRGB.getColor(i, j).equals(BLACK)
                        && (imageRGB.getColor(i, j).isGrey() || fixedColorSet.contains(imageRGB.getColor(i, j)))) {
                    topBound = j;
                    if (fixedColorSet.contains(imageRGB.getColor(i, j))) {
                        topBound += 7;
                    }
                    break;
                }
            }
            if (topBound > -1) {
                break;
            }
        }
        for (int j = MISSION_MAP_BOTTOM_BOUND; j > CANDIDATE_BOTTOM_BOUND; j--) {
            for (int i = 0; i < imageRGB.getWidth(); i++) {
                if (!imageRGB.getColor(i, j).equals(BLACK)
                        && (imageRGB.getColor(i, j).isGrey() || fixedColorSet.contains(imageRGB.getColor(i, j)))) {
                    bottomBound = j;
                    if (fixedColorSet.contains(imageRGB.getColor(i, j))) {
                        bottomBound -= 6;
                    }
                    break;
                }
            }
            if (bottomBound > -1) {
                break;
            }
        }
        System.out.println(leftBound + " " + rightBound + " " + topBound + " " + bottomBound);
        int missionWidth = (rightBound - leftBound + CHINK_SIZE) / (BLANK_BLOCK_SIZE + CHINK_SIZE);
        int missionHeight = (bottomBound - topBound + CHINK_SIZE) / (BLANK_BLOCK_SIZE + CHINK_SIZE);
        MissionMap missionMap = new MissionMap(missionWidth, missionHeight);
        for (int i = leftBound; i <= rightBound; i++) {
            for (int j = topBound; j <= bottomBound; j++) {
                MissionMap.Type blockType = null;
                ColorRGB blockColor = null;
                if (imageRGB.getColor(i, j).isGrey() && deviation(imageRGB.getColor(i, j), GREY) <= GREY_DEVIATION
                        && checkColorCluster(imageRGB, i, j, 15, 0, 6, 0)) {
                    blockType = MissionMap.Type.BLANK;
                } else if (fixedColorSet.contains(imageRGB.getColor(i, j))
                        && checkColorCluster(imageRGB, i, j, COLOR_BLOCK_CHECK_BOUND)) {
                    blockType = MissionMap.Type.FIXED;
                    blockColor = imageRGB.getColor(i, j);
                }
                if (blockType != null) {
                    int x = 0, y = 0;
                    for (int u = i - leftBound; u - CHINK_SIZE - BLANK_BLOCK_SIZE >= 0; u = u - CHINK_SIZE - BLANK_BLOCK_SIZE) {
                        x++;
                    }
                    for (int v = j - topBound; v - CHINK_SIZE - BLANK_BLOCK_SIZE >= 0; v = v - CHINK_SIZE - BLANK_BLOCK_SIZE) {
                        y++;
                    }
                    missionMap.setPoint(x, y, blockType, blockColor);
                }
            }
        }
        return missionMap;
    }

    /**
     * 优化一：对初始的地图，根据已经给定的初始方块位置，尝试推导空白位置的颜色，来扩展地图，缩小搜索树宽度
     *
     * @param missionMap 初始地图MissionMap对象
     * @return 推导并放置了色块的数量
     */
    private int expandMissionMap(MissionMap missionMap) {
        int n = 0;
        Set<Integer> usedColorSet = Sets.newHashSet();
        for (int i = 0; i < missionMap.getWidth(); i++) {
            for (int j = 0; j < missionMap.getHeight(); j++) {
                if (missionMap.getPoint(i, j).getType() == MissionMap.Type.FIXED) {
                    MissionMap.Coordinate[] queue = new MissionMap.Coordinate[missionMap.getWidth() * missionMap.getHeight()];
                    int head = 0, tail = 1;
                    queue[0] = new MissionMap.Coordinate(i, j);
                    while (head < tail) {
                        Set<MissionMap.Coordinate> expandSet = expand(missionMap, usedColorSet, queue[head].getX(), queue[head].getY());
                        n += expandSet.size();
                        for (MissionMap.Coordinate expandPoint : expandSet) {
                            queue[tail++] = expandPoint;
                        }
                        head++;
                    }
                }
            }
        }
        return n;
    }

    /**
     * 深度优先搜索，根据当前的地图，选择一个位置，枚举可以放置的颜色，标记后进入下一层
     *
     * @param missionMap 当前层的MissionMap对象
     * @param depth      当前层的搜索深度
     */
    private void resolveMissionMap(MissionMap missionMap, int depth) {
        if (depth >= totalBlanks) {
            double deviation = evaluateMissionMap(missionMap);
            if (bestDeviation < 1 || bestDeviation > deviation) {
                bestDeviation = deviation;
                bestMap = MissionMap.clone(missionMap);
            }
            return;
        }
        MissionMap _missionMap = MissionMap.clone(missionMap);
        int maxDegreeValue = -1;
        MissionMap.Coordinate maxDegreePoint = new MissionMap.Coordinate(0, 0);
        for (int x = 0; x < missionMap.getWidth(); x++) {
            for (int y = 0; y < missionMap.getHeight(); y++) {
                if (missionMap.getPoint(x, y).getType() == MissionMap.Type.BLANK) {
                    int degree = degree(missionMap, x, y);
                    if (degree > maxDegreeValue) {
                        maxDegreeValue = degree;
                        maxDegreePoint = new MissionMap.Coordinate(x, y);
                    }
                }
            }
        }
        int i = maxDegreePoint.getX();
        int j = maxDegreePoint.getY();
        Set<Integer> usedColorSet;
        if (missionMap.getPoint(i, j).getType() == MissionMap.Type.BLANK) {
            usedColorSet = Sets.newHashSet();
            for (int k = 0; k < candidateColorList.size(); k++) {
                if (!candidateColorUsed[k]) {
                    ColorRGB candidateColor = candidateColorList.get(k);
                    missionMap.setPoint(i, j, MissionMap.Type.FILLED, candidateColor);
                    usedColorSet.add(k);
                    candidateColorUsed[k] = true;
                    MissionMap.Coordinate[] queue = new MissionMap.Coordinate[missionMap.getWidth() * missionMap.getHeight()];
                    int n = 1, head = 0, tail = 1;
                    queue[0] = new MissionMap.Coordinate(i, j);
                    while (head < tail) {
                        Set<MissionMap.Coordinate> expandSet = expand(missionMap, usedColorSet, queue[head].getX(), queue[head].getY());
                        n += expandSet.size();
                        for (MissionMap.Coordinate expandPoint : expandSet) {
                            queue[tail++] = expandPoint;
                        }
                        head++;
                    }
                    resolveMissionMap(missionMap, depth + n);
                    for (int c : usedColorSet) {
                        candidateColorUsed[c] = false;
                    }
                    usedColorSet.clear();
                    missionMap = MissionMap.clone(_missionMap);
                }
            }
        }
    }

    /**
     * 对当前放置好的MissionMap，进行偏离值的计算，包括每行每列
     * 计算方法：
     * 1. 取出顶端两个色块的sRGB
     * 2. 根据两端差值和行列长度，计算中间每个色块期望的sRGB
     * 3. 遍历中间每个色块的sRGB，计算期望与实际的偏离值
     * 4. 所有偏离值加和返回
     *
     * @param missionMap 地图MissionMap
     * @return 当前地图的颜色偏离值总计
     */
    private double evaluateMissionMap(MissionMap missionMap) {
        double deviation = 0;
        for (int i = 0; i < missionMap.getWidth(); i++) {
            for (int j = 0; j < missionMap.getHeight(); j++) {
                if (missionMap.getPoint(i, j).getType() == MissionMap.Type.FILLED
                        || missionMap.getPoint(i, j).getType() == MissionMap.Type.FIXED) {
                    int x = j, y = j + 1;
                    for (; y < missionMap.getHeight(); y++) {
                        if (missionMap.getPoint(i, y).getType() == MissionMap.Type.NULL) {
                            break;
                        }
                    }
                    int l = y - x;
                    double deltaR = 1.0 * (missionMap.getPoint(i, y - 1).getColor().getR() - missionMap.getPoint(i, x).getColor().getR()) / l;
                    double deltaG = 1.0 * (missionMap.getPoint(i, y - 1).getColor().getG() - missionMap.getPoint(i, x).getColor().getG()) / l;
                    double deltaB = 1.0 * (missionMap.getPoint(i, y - 1).getColor().getB() - missionMap.getPoint(i, x).getColor().getB()) / l;
                    for (int k = x; k < y; k++) {
                        double expectedR = missionMap.getPoint(i, x).getColor().getR() + (k - x) * deltaR;
                        double expectedG = missionMap.getPoint(i, x).getColor().getG() + (k - x) * deltaG;
                        double expectedB = missionMap.getPoint(i, x).getColor().getB() + (k - x) * deltaB;
                        double actualR = missionMap.getPoint(i, k).getColor().getR();
                        double actualG = missionMap.getPoint(i, k).getColor().getG();
                        double actualB = missionMap.getPoint(i, k).getColor().getB();
                        deviation = deviation + deviation(expectedR, expectedG, expectedB, actualR, actualG, actualB);
                    }
                    j = y;
                }
            }
        }
        for (int j = 0; j < missionMap.getHeight(); j++) {
            for (int i = 0; i < missionMap.getWidth(); i++) {
                if (missionMap.getPoint(i, j).getType() == MissionMap.Type.FILLED
                        || missionMap.getPoint(i, j).getType() == MissionMap.Type.FIXED) {
                    int x = i, y = i + 1;
                    for (; y < missionMap.getWidth(); y++) {
                        if (missionMap.getPoint(y, j).getType() == MissionMap.Type.NULL) {
                            break;
                        }
                    }
                    int l = y - x;
                    double deltaR = 1.0 * (missionMap.getPoint(y - 1, j).getColor().getR() - missionMap.getPoint(x, j).getColor().getR()) / l;
                    double deltaG = 1.0 * (missionMap.getPoint(y - 1, j).getColor().getG() - missionMap.getPoint(x, j).getColor().getG()) / l;
                    double deltaB = 1.0 * (missionMap.getPoint(y - 1, j).getColor().getB() - missionMap.getPoint(x, j).getColor().getB()) / l;
                    for (int k = x; k < y; k++) {
                        double expectedR = missionMap.getPoint(x, j).getColor().getR() + (k - x) * deltaR;
                        double expectedG = missionMap.getPoint(x, j).getColor().getG() + (k - x) * deltaG;
                        double expectedB = missionMap.getPoint(x, j).getColor().getB() + (k - x) * deltaB;
                        double actualR = missionMap.getPoint(k, j).getColor().getR();
                        double actualG = missionMap.getPoint(k, j).getColor().getG();
                        double actualB = missionMap.getPoint(k, j).getColor().getB();
                        deviation = deviation + deviation(expectedR, expectedG, expectedB, actualR, actualG, actualB);
                    }
                    i = y;
                }
            }
        }
        return deviation;
    }

    public static void main(String[] args) {
        new Blendoku();
    }

    /**
     * 对(i, j)位置的行列进行扩展
     *
     * @param missionMap   地图MissionMap
     * @param usedColorSet 使用的颜色集合，用于回溯时还原候选颜色列表
     * @param i            横向坐标i
     * @param j            纵向坐标
     * @return 推导扩展出的一系列位置坐标
     */
    private Set<MissionMap.Coordinate> expand(MissionMap missionMap, Set<Integer> usedColorSet, int i, int j) {
        Set<MissionMap.Coordinate> expandSet = Sets.newHashSet();
        int left = bound(missionMap, i, j, -1, 0, false).getX();
        int right = bound(missionMap, i, j, 1, 0, false).getX();
        int top = bound(missionMap, i, j, 0, -1, false).getY();
        int bottom = bound(missionMap, i, j, 0, 1, false).getY();
        if (right - left > 0) {
            double deltaR = 1.0 * (missionMap.getPoint(right, j).getColor().getR() - missionMap.getPoint(left, j).getColor().getR()) / (right - left);
            double deltaG = 1.0 * (missionMap.getPoint(right, j).getColor().getG() - missionMap.getPoint(left, j).getColor().getG()) / (right - left);
            double deltaB = 1.0 * (missionMap.getPoint(right, j).getColor().getB() - missionMap.getPoint(left, j).getColor().getB()) / (right - left);
            int u = bound(missionMap, i, j, -1, 0, true).getX();
            int v = bound(missionMap, i, j, 1, 0, true).getX();
            for (int l = u; l <= v; l++) {
                if (missionMap.getPoint(l, j).getType() == MissionMap.Type.BLANK) {
                    double expectedR = missionMap.getPoint(left, j).getColor().getR() + (l - left) * deltaR;
                    double expectedG = missionMap.getPoint(left, j).getColor().getG() + (l - left) * deltaG;
                    double expectedB = missionMap.getPoint(left, j).getColor().getB() + (l - left) * deltaB;
                    double minimalDeviation = MAX_SINGLE_LOSS;
                    int minimalColor = -1;
                    for (int c = 0; c < candidateColorList.size(); c++) {
                        if (!candidateColorUsed[c]) {
                            ColorRGB _candidateColor = candidateColorList.get(c);
                            if (deviation(expectedR, expectedG, expectedB, _candidateColor.getR(), _candidateColor.getG(), _candidateColor.getB()) < minimalDeviation) {
                                minimalDeviation = deviation(expectedR, expectedG, expectedB, _candidateColor.getR(), _candidateColor.getG(), _candidateColor.getB());
                                minimalColor = c;
                            }
                        }
                    }
                    expandSet.add(new MissionMap.Coordinate(l, j));
                    usedColorSet.add(minimalColor);
                    candidateColorUsed[minimalColor] = true;
                    missionMap.setPoint(l, j, MissionMap.Type.FILLED, candidateColorList.get(minimalColor));
                }
            }
        }
        if (bottom - top > 0) {
            double deltaR = 1.0 * (missionMap.getPoint(i, bottom).getColor().getR() - missionMap.getPoint(i, top).getColor().getR()) / (bottom - top);
            double deltaG = 1.0 * (missionMap.getPoint(i, bottom).getColor().getG() - missionMap.getPoint(i, top).getColor().getG()) / (bottom - top);
            double deltaB = 1.0 * (missionMap.getPoint(i, bottom).getColor().getB() - missionMap.getPoint(i, top).getColor().getB()) / (bottom - top);
            int u = bound(missionMap, i, j, 0, -1, true).getY();
            int v = bound(missionMap, i, j, 0, 1, true).getY();
            for (int l = u; l <= v; l++) {
                if (missionMap.getPoint(i, l).getType() == MissionMap.Type.BLANK) {
                    double expectedR = missionMap.getPoint(i, top).getColor().getR() + (l - top) * deltaR;
                    double expectedG = missionMap.getPoint(i, top).getColor().getG() + (l - top) * deltaG;
                    double expectedB = missionMap.getPoint(i, top).getColor().getB() + (l - top) * deltaB;
                    double minimalDeviation = MAX_SINGLE_LOSS;
                    int minimalColor = -1;
                    for (int c = 0; c < candidateColorList.size(); c++) {
                        if (!candidateColorUsed[c]) {
                            ColorRGB _candidateColor = candidateColorList.get(c);
                            if (deviation(expectedR, expectedG, expectedB, _candidateColor.getR(), _candidateColor.getG(), _candidateColor.getB()) < minimalDeviation) {
                                minimalDeviation = deviation(expectedR, expectedG, expectedB, _candidateColor.getR(), _candidateColor.getG(), _candidateColor.getB());
                                minimalColor = c;
                            }
                        }
                    }
                    expandSet.add(new MissionMap.Coordinate(i, l));
                    usedColorSet.add(minimalColor);
                    candidateColorUsed[minimalColor] = true;
                    missionMap.setPoint(i, l, MissionMap.Type.FILLED, candidateColorList.get(minimalColor));
                }
            }
        }
        return expandSet;
    }

    /**
     * 以(x, y)位置，以(directionX, directionY)为方向，计算边界坐标
     * 边界的定义：
     * 1. 若方格为NULL，则一定为边界
     * 2. 若blank为true，则遇到BLANK也为边界；否则边界一定为FILLED或FIXED
     *
     * @param missionMap 地图MissionMap
     * @param x          中心点横向坐标
     * @param y          中心点纵向坐标
     * @param directionX 横向方向
     * @param directionY 纵向方向
     * @param blank      是否允许BLANK为边界
     * @return 边界坐标
     */
    private MissionMap.Coordinate bound(MissionMap missionMap, int x, int y, int directionX, int directionY, boolean blank) {
        MissionMap.Coordinate bound = new MissionMap.Coordinate(x, y);
        for (int k = 1; x + k * directionX >= 0 && x + k * directionX < missionMap.getWidth()
                && y + k * directionY >= 0 && y + k * directionY < missionMap.getHeight(); k++) {
            if (missionMap.getPoint(x + k * directionX, y + k * directionY).getType() == MissionMap.Type.FILLED
                    || missionMap.getPoint(x + k * directionX, y + k * directionY).getType() == MissionMap.Type.FIXED) {
                bound = new MissionMap.Coordinate(x + k * directionX, y + k * directionY);
            } else if (missionMap.getPoint(x + k * directionX, y + k * directionY).getType() == MissionMap.Type.NULL) {
                break;
            } else if (blank) {
                bound = new MissionMap.Coordinate(x + k * directionX, y + k * directionY);
            }
        }
        return bound;
    }

    /**
     * 计算(x, y)位置的度，若上下左右不为空，+10；若其中包含了FIXED或FILLED，+1
     *
     * @param missionMap 地图MissionMap
     * @param x          横向坐标
     * @param y          纵向坐标
     * @return 该位置的度，越大优先级越高
     */
    private int degree(MissionMap missionMap, int x, int y) {
        int[] deltaX = {-1, 1, 0, 0};
        int[] deltaY = {0, 0, -1, 1};
        int degree = 0;
        for (int k = 0; k < deltaX.length; k++) {
            if (missionMap.getPoint(x + deltaX[k], y + deltaY[k]).getType() != MissionMap.Type.NULL) {
                degree += 10;
            }
            if (missionMap.getPoint(x + deltaX[k], y + deltaY[k]).getType() == MissionMap.Type.FIXED
                    || missionMap.getPoint(x + deltaX[k], y + deltaY[k]).getType() == MissionMap.Type.FILLED) {
                degree += 1;
            }
        }
        return degree;
    }

    /**
     * 两个颜色的偏离值，欧式距离
     *
     * @param r1 第一个颜色的R
     * @param g1 第一个颜色的G
     * @param b1 第一个颜色的B
     * @param r2 第二个颜色的R
     * @param g2 第二个颜色的G
     * @param b2 第二个颜色的B
     * @return 偏离值
     */
    private double deviation(double r1, double g1, double b1, double r2, double g2, double b2) {
        return (r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2);
    }

    /**
     * 两个颜色的偏离值，欧式距离
     *
     * @param c1 第一个颜色
     * @param c2 第二个颜色
     * @return 偏离值
     */
    private double deviation(ColorRGB c1, ColorRGB c2) {
        return deviation(c1.getR(), c1.getG(), c1.getB(), c2.getR(), c2.getG(), c2.getB());
    }

    /**
     * 在ImageRGB中的给定区域内，抽取色块
     *
     * @param imageRGB ImageRGB对象
     * @param x1       横向坐标左边界
     * @param x2       横向坐标右边界
     * @param y1       纵向坐标上边界
     * @param y2       纵向坐标下边界
     * @return 色块sRGB列表
     */
    private List<ColorRGB> fetchColorBlock(ImageRGB imageRGB, int x1, int x2, int y1, int y2) {
        List<ColorRGB> colorList = Lists.newArrayList();
        for (int i = x1; i < x2; i++) {
            for (int j = y1; j < y2; j++) {
                if (!imageRGB.getColor(i, j).equals(BLACK) && !colorList.contains(imageRGB.getColor(i, j))) {
                    if (checkColorCluster(imageRGB, i, j, COLOR_BLOCK_CHECK_BOUND)) {
                        colorList.add(imageRGB.getColor(i, j));
                    }
                }
            }
        }
        return colorList;
    }

    /**
     * 检查以(x, y)为中心，size大小内，颜色是否成簇
     *
     * @param imageRGB ImageRGB对象
     * @param x        横向坐标
     * @param y        纵向坐标
     * @param size     簇大小，正负size
     * @return 是否成簇
     */
    private boolean checkColorCluster(ImageRGB imageRGB, int x, int y, int size) {
        ColorRGB centerColor = imageRGB.getColor(x, y);
        if (deviation(BLACK.getR(), BLACK.getG(), BLACK.getB(), centerColor.getR(), centerColor.getG(), centerColor.getB()) <= BACKGROUND_DEVIATION) {
            return false;
        }
        for (int i = -size; i <= size; i++) {
            for (int j = -size; j <= size; j++) {
                if (x + i < 0 || x + i > imageRGB.getWidth() || y + j < 0 || y + j > imageRGB.getHeight()
                        || !imageRGB.getColor(x + i, y + j).equals(centerColor)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 检查以(x, y)为中心，左leftX，右rightX，上topY，下bottomY内，颜色是否成簇
     *
     * @param imageRGB ImageRGB对象
     * @param x        横向坐标
     * @param y        纵向坐标
     * @param leftX    左offset
     * @param rightX   右offset
     * @param topY     上offset
     * @param bottomY  下offset
     * @return 是否成簇
     */
    private boolean checkColorCluster(ImageRGB imageRGB, int x, int y, int leftX, int rightX, int topY, int bottomY) {
        ColorRGB centerColor = imageRGB.getColor(x, y);
        if (deviation(BLACK.getR(), BLACK.getG(), BLACK.getB(), centerColor.getR(), centerColor.getG(), centerColor.getB()) <= BACKGROUND_DEVIATION) {
            return false;
        }
        boolean flag = true;
        for (int i = x - leftX; i <= x + rightX; i++) {
            for (int j = y - topY; j <= y + bottomY; j++) {
                if (!imageRGB.getColor(i, j).equals(centerColor)) {
                    flag = false;
                    break;
                }
            }
            if (!flag) {
                break;
            }
        }
        return flag;
    }
}
