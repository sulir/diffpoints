package sk.tuke.diffpoints.ui;

import com.intellij.ui.SimpleColoredComponent;

import java.awt.*;

public class ObjectColoredComponent extends SimpleColoredComponent {

    private Color customBackground;
    private Color selectedBackground; // example
    private boolean selected;

    public ObjectColoredComponent(Color customBackground) {
        this.customBackground = customBackground;

        if (customBackground == UiColours.MODIFIED_DIFF.colour)
            this.selectedBackground = UiColours.SELECTED_MODIFIED_DIFF.colour;
        else if (customBackground == UiColours.CREATED_DIFF.colour)
            this.selectedBackground = UiColours.SELECTED_CREATED_DIFF.colour;
        else if (customBackground == UiColours.REMOVED_DIFF.colour)
            this.selectedBackground = UiColours.SELECTED_REMOVED_DIFF.colour;
        else
            this.selectedBackground = UiColours.SELECTED.colour;
        this.setPreferredSize(new Dimension(0, 22));
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        this.setMinimumSize(new Dimension(0, 22));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color bg = selected && selectedBackground != null
                ? selectedBackground
                : customBackground;

        if (bg != null) {
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paintComponent(g);
    }

    public void setCustomBackground(Color customBackground) {
        this.customBackground = customBackground;
        if (!selected) repaint();
    }

    public void setSelectedBackground(Color selectedBackground) {
        this.selectedBackground = selectedBackground;
        if (selected) repaint();
    }
}