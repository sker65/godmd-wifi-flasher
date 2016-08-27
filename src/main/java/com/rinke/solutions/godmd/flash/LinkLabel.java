package com.rinke.solutions.godmd.flash;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleText;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;


/**
 * Label, which may contain link tags in the HTML markup, which will be clickable.<br>
 * The link positions will be calculated automatically using pattern matching.<br>
 * This class is based on an idea found here: http://www.java-forum.org/codeschnipsel-u-projekte/107710-klickbare-link-jlabel.html

 * <br>
 * Usage:<br>
 * The links must have the form &lt;a href='YOUR-URL'&gt;YOUR-LABEL&lt;/a&gt; also HTML markup for the label is supported.
 * 
 * @author sknull
 */
public final class LinkLabel extends JLabel {

    private static final long serialVersionUID = 1L;
    
    /** Pattern for a link */
    private static final Pattern PATTERN_LINK = Pattern.compile("<a href='([^']*?)'[^>]*?>.*?</a>");
    
    /** Pattern for HTML start tag */
    private static final Pattern PATTERN_TAG = Pattern.compile("<[^>]*?>");

    /** The lazy initialized list of determined links */
    private List<LinkDescriptor> listLinks;


    /**
     * Creates a <code>JLabel</code> instance with 
     * no image and with an empty string for the title.
     * The label is centered vertically 
     * in its display area.
     * The label's contents, once set, will be displayed on the leading edge 
     * of the label's display area.
     */
    public LinkLabel() {
        this("", null, SwingConstants.LEADING);
    }


    /**
     * Creates a <code>JLabel</code> instance with the specified image.
     * The label is centered vertically and horizontally
     * in its display area.
     *
     * @param icon  The image to be displayed by the label.
     */
    public LinkLabel(Icon icon) {
        this("", icon, SwingConstants.CENTER);
    }


    /**
     * Creates a <code>JLabel</code> instance with the specified
     * image and horizontal alignment.
     * The label is centered vertically in its display area.
     *
     * @param icon  The image to be displayed by the label.
     * @param horizontalAlignment  One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>LEFT</code>,
     *           <code>CENTER</code>, 
     *           <code>RIGHT</code>,
     *           <code>LEADING</code> or
     *           <code>TRAILING</code>.
     */
    public LinkLabel(Icon icon, int horizontalAlignment) {
        this("", icon, horizontalAlignment);
    }


    /**
     * Creates a <code>JLabel</code> instance with the specified
     * text, image, and horizontal alignment.
     * The label is centered vertically in its display area.
     * The text is on the trailing edge of the image.
     *
     * @param text  The text to be displayed by the label.
     * @param icon  The image to be displayed by the label.
     */
    public LinkLabel(String text, Icon icon) {
        this(text, icon, SwingConstants.LEADING);
    }

    
    /**
     * Creates a <code>JLabel</code> instance with the specified text.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     *
     * @param text  The text to be displayed by the label.
     */
    public LinkLabel(String text) {
        this(text, null, SwingConstants.LEADING);
    }


    /**
     * Creates a <code>JLabel</code> instance with the specified
     * text and horizontal alignment.
     * The label is centered vertically in its display area.
     *
     * @param text  The text to be displayed by the label.
     * @param horizontalAlignment  One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>LEFT</code>,
     *           <code>CENTER</code>,
     *           <code>RIGHT</code>,
     *           <code>LEADING</code> or
     *           <code>TRAILING</code>.
     */
    public LinkLabel(String text, int horizontalAlignment) {
        this(text, null, horizontalAlignment);
    }


    /**
     * Creates a <code>JLabel</code> instance with the specified
     * text, image, and horizontal alignment.
     * The label is centered vertically in its display area.
     * The text is on the trailing edge of the image.
     *
     * @param text  The text to be displayed by the label.
     * @param icon  The image to be displayed by the label.
     * @param horizontalAlignment  One of the following constants
     *           defined in <code>SwingConstants</code>:
     *           <code>LEFT</code>,
     *           <code>CENTER</code>,
     *           <code>RIGHT</code>,
     *           <code>LEADING</code> or
     *           <code>TRAILING</code>.
     */
    public LinkLabel(String text, Icon icon, int horizontalAlignment) {

        super(text, icon, horizontalAlignment);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final String link = getLink(e.getPoint());
                if (link == null)
                    return;
                try {
                    Desktop.getDesktop().browse(new URI(link));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "error open link", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {

            boolean pointedOnALink = false;


            @Override
            public void mouseMoved(MouseEvent e) {
                boolean pointsOnALink = getLink(e.getPoint()) != null;
                if (pointsOnALink != pointedOnALink) {
                    if (pointsOnALink) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }
                    pointedOnALink = pointsOnALink;
                }
            }
        });
    }
    
    
    /**
     * Little bean to describe a link with its start- and end char index and the associated URL.
     * @author sknull
     */
    private final class LinkDescriptor {
        
        private int start;
        
        private int end;
        
        private String url;
        
        public LinkDescriptor() {
        }

        
        @Override
        public String toString() {
            return "LinkDescriptor [start=" + start + ", end=" + end + ", url=" + url + "]";
        }


        public LinkDescriptor(int start, int stop, String url) {
            super();
            this.start = start;
            this.end = stop;
            this.url = url;
        }


        public int getStart() {
            return start;
        }

        
        public void setStart(int start) {
            this.start = start;
        }

        
        public int getEnd() {
            return end;
        }

        
        public void setEnd(int stop) {
            this.end = stop;
        }

        
        public String getUrl() {
            return url;
        }

        
        public void setUrl(String url) {
            this.url = url;
        }
    }


    @Override
    public void setText(String text) {
        super.setText(text);
        determineLinks(text);
    }


    /**
     * Determines the contained links in the given HTML markup.
     * 
     * @param text The HTML markup to parse.
     */
    public void determineLinks(String text) {
        
        if (listLinks == null) {
            listLinks = new ArrayList<LinkDescriptor>();
        } else {
            listLinks.clear();
        }

        final Matcher mLink = PATTERN_LINK.matcher(text);
        final List<LinkDescriptor> lLinks = new ArrayList<LinkDescriptor>();
        while (mLink.find()) {
            lLinks.add(new LinkDescriptor(mLink.start(), mLink.end(), mLink.group(1)));
        }
        if (lLinks.isEmpty()) {
            return;
        }
        final Matcher mTag = PATTERN_TAG.matcher(text);
        final List<Integer[]> lTags = new ArrayList<Integer[]>();
        while (mTag.find()) {
            lTags.add(new Integer[] {mTag.start(), mTag.end(), 0});
        }
        final StringBuilder rawText = new StringBuilder(text.substring(0, lTags.get(0)[0]));
        lTags.get(0)[2] = rawText.length();
        for (int i = 1; i < lTags.size(); i++) {
            rawText.append(text.substring(lTags.get(i - 1)[1], lTags.get(i)[0]));
            lTags.get(i)[2] = rawText.length();
        }
        LinkDescriptor entry = new LinkDescriptor();
        for (final LinkDescriptor link : lLinks) {
            for (final Integer[] tag : lTags) {
                if (tag[0].equals(link.getStart())) {
                    entry.setStart(tag[2]);
                } else if (tag[1].equals(link.getEnd())) {
                    entry.setEnd(tag[2]);
                    entry.setUrl(link.getUrl());
                    listLinks.add(entry);
                    entry = new LinkDescriptor();
                }
            }
        }
        //      for (final LinkDescriptor descriptor : listLinks) {
        //          System.out.println(rawText.substring(descriptor.getStart(), descriptor.getEnd())+": "+descriptor.getUrl());
        //      }
    }


    /**
     * Determines the associated link to a clicked point within the label.
     * 
     * @param p Point where the user clicked within the label.
     * @return The associated url for this link (if any), null if point was outside any link.
     */
    protected String getLink(Point p) {
        final AccessibleContext aC = getAccessibleContext();
        if (aC instanceof AccessibleJLabel) {
            final AccessibleJLabel aL = (AccessibleJLabel) aC;
            final AccessibleText aT = aL.getAccessibleText();
            if (aT == null) {
                return null;
            }
            final int index = aL.getIndexAtPoint(p);
            for (final LinkDescriptor entry : listLinks) {
                if (index >= entry.getStart() && index <= entry.getEnd()) {
                    return entry.getUrl();
                }
            }
        }
        return null;
    }
}