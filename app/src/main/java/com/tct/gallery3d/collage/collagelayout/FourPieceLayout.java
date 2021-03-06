package com.tct.gallery3d.collage.collagelayout;

import com.tct.gallery3d.collage.border.Line;

/**
 * Created by liuxiaoyu on 16-10-10.
 */
public class FourPieceLayout extends NumberPieceLayout {
    private static final String TAG = "FourPieceLayout";

    public FourPieceLayout(int theme) {
        super(theme);
    }

    @Override
    public int getThemeCount() {
        return 4;
    }

    @Override
    public void layout() {
        switch (mTheme) {
            case 0:
                cutBorderEqualPart(getOuterBorder(), 4, Line.Direction.HORIZONTAL);
                break;
            case 1:
                cutBorderEqualPart(getOuterBorder(), 4, Line.Direction.VERTICAL);
                break;
            case 2:
                addCross(getOuterBorder(), 1f / 2);
                break;
            case 3:
                //addLine(getOuterBorder(), Line.Direction.HORIZONTAL, 1f / 3);
                //cutBorderEqualPart(getBorder(0), 3, Line.Direction.VERTICAL);
                addLine(getOuterBorder(), Line.Direction.VERTICAL, 2f / 3);
                cutBorderEqualPart(getBorder(1), 3, Line.Direction.HORIZONTAL);
                break;
            /*case 4:
                addLine(getOuterBorder(), Line.Direction.HORIZONTAL, 2f / 3);
                cutBorderEqualPart(getBorder(1), 3, Line.Direction.VERTICAL);
                break;
            case 5:
                addLine(getOuterBorder(), Line.Direction.VERTICAL, 1f / 3);
                cutBorderEqualPart(getBorder(0), 3, Line.Direction.HORIZONTAL);
                break;
            case 6:
                addLine(getOuterBorder(), Line.Direction.VERTICAL, 2f / 3);
                cutBorderEqualPart(getBorder(1), 3, Line.Direction.HORIZONTAL);
                break;
            case 7:
                addLine(getOuterBorder(), Line.Direction.VERTICAL, 1f / 2);
                addLine(getBorder(1), Line.Direction.HORIZONTAL, 2f / 3);
                addLine(getBorder(1), Line.Direction.HORIZONTAL, 1f / 3);
                break;*/
            default:
                cutBorderEqualPart(getOuterBorder(), 4, Line.Direction.HORIZONTAL);
                break;
        }
    }
}
