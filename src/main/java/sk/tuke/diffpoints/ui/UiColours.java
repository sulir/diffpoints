package sk.tuke.diffpoints.ui;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;

import java.awt.*;

public enum UiColours {

    // JBColour(LightModeColour, DarkModeColour);


    EQUAL_SIGN(new JBColor(Gray._35, Gray._227)),
    VALUE(new JBColor(Gray._0, Gray._216)),
    BORDER(new JBColor(Gray._10, Gray._10)),
    STRING_VALUE(new JBColor(new Color(64, 155, 1), new Color(116, 159, 98))),
    ADDRESS_VALUE(new JBColor(Gray._96, Gray._150)),
    VARIABLE_NAME(new JBColor(new Color(112, 10, 46), new Color(225, 166, 113))),

    MODIFIED_DIFF(new JBColor(new Color(190, 223, 255), new Color(71, 94, 115))),
    CREATED_DIFF(new JBColor(new Color(206, 243, 200), new Color(68, 84, 66))),
    REMOVED_DIFF(new JBColor(new Color(238, 189, 189), new Color(101, 47, 47))),

    SELECTED(new JBColor(new Color(177, 184, 187), new Color(121, 121, 121))),
    SELECTED_MODIFIED_DIFF(new JBColor(new Color(119, 162, 211), new Color(72, 125, 176))),
    SELECTED_CREATED_DIFF(new JBColor(new Color(119, 197, 111), new Color(56, 108, 54))),
    SELECTED_REMOVED_DIFF(new JBColor(new Color(246, 153, 153), new Color(141, 51, 51)));


    public final Color colour;

    UiColours(Color color) {
        this.colour = color;
    }
}
