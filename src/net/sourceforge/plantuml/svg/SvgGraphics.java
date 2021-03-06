/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2017, Arnaud Roques
 *
 * Project Info:  http://plantuml.com
 * 
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * Original Author:  Arnaud Roques
 * 
 * Revision $Revision: 20128 $
 *
 */
package net.sourceforge.plantuml.svg;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.plantuml.Log;
import net.sourceforge.plantuml.StringUtils;
import net.sourceforge.plantuml.code.Base64Coder;
import net.sourceforge.plantuml.eps.EpsGraphics;
import net.sourceforge.plantuml.graphic.HtmlColorGradient;
import net.sourceforge.plantuml.ugraphic.ColorMapper;
import net.sourceforge.plantuml.ugraphic.UPath;
import net.sourceforge.plantuml.ugraphic.USegment;
import net.sourceforge.plantuml.ugraphic.USegmentType;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SvgGraphics {

	// http://tutorials.jenkov.com/svg/index.html
	// http://www.svgbasics.com/
	// http://apike.ca/prog_svg_text.html
	// http://www.w3.org/TR/SVG11/shapes.html
	// http://en.wikipedia.org/wiki/Scalable_Vector_Graphics

	// Animation:
	// http://srufaculty.sru.edu/david.dailey/svg/
	// Shadow:
	// http://www.svgbasics.com/filters3.html
	// http://www.w3schools.com/svg/svg_feoffset.asp
	// http://www.adobe.com/svg/demos/samples.html

	final private Document document;
	final private Element root;
	final private Element defs;
	final private Element gRoot;

	private String fill = "black";
	private String stroke = "black";
	private String strokeWidth;
	private String strokeDasharray = null;
	private final String backcolor;

	private int maxX = 10;
	private int maxY = 10;

	private final double scale;
	private final String filterUid;
	private final String shadowId;

	final protected void ensureVisible(double x, double y) {
		if (x > maxX) {
			maxX = (int) (x + 1);
		}
		if (y > maxY) {
			maxY = (int) (y + 1);
		}
	}

	public SvgGraphics(double scale) {
		this(null, scale);
	}

	public SvgGraphics(String backcolor, double scale) {
		try {
			this.scale = scale;
			this.document = getDocument();
			this.backcolor = backcolor;

			this.root = getRootNode();

			// Create a node named defs, which will be the parent
			// for a pair of linear gradient definitions.
			defs = simpleElement("defs");
			gRoot = simpleElement("g");
			strokeWidth = "" + scale;
			final Random rnd = new Random();
			this.filterUid = "b" + getRandomString(rnd);
			this.shadowId = "f" + getRandomString(rnd);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}

	private static String getRandomString(final Random rnd) {
		String result = Integer.toString(Math.abs(rnd.nextInt()), 36);
		while (result.length() < 6) {
			result = "0" + result;
		}
		return result;
	}

	public static void main(String[] args) {
		System.err.println(getRandomString(new Random()));
	}

	private Element pendingBackground;

	public void paintBackcolorGradient(ColorMapper mapper, HtmlColorGradient gr) {
		final String id = createSvgGradient(StringUtils.getAsHtml(mapper.getMappedColor(gr.getColor1())),
				StringUtils.getAsHtml(mapper.getMappedColor(gr.getColor2())), gr.getPolicy());
		setFillColor("url(#" + id + ")");
		setStrokeColor(null);
		pendingBackground = createRectangleInternal(0, 0, 0, 0);
		getG().appendChild(pendingBackground);
	}

	// This method returns a reference to a simple XML
	// element node that has no attributes.
	private Element simpleElement(String type) {
		final Element theElement = (Element) document.createElement(type);
		root.appendChild(theElement);
		return theElement;
	}

	private Document getDocument() throws ParserConfigurationException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		final DocumentBuilder builder = factory.newDocumentBuilder();
		final Document document = builder.newDocument();
		document.setXmlStandalone(true);
		return document;
	}

	// This method returns a reference to a root node that
	// has already been appended to the document.
	private Element getRootNode() {
		// Create the root node named svg and append it to
		// the document.
		final Element svg = (Element) document.createElement("svg");
		document.appendChild(svg);

		// Set some attributes on the root node that are
		// required for proper rendering. Note that the
		// approach used here is somewhat different from the
		// approach used in the earlier program named Svg01,
		// particularly with regard to the style.
		svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
		svg.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		svg.setAttribute("version", "1.1");

		return svg;
	}

	public void svgEllipse(double x, double y, double xRadius, double yRadius, double deltaShadow) {
		manageShadow(deltaShadow);
		if (hidden == false) {
			final Element elt = (Element) document.createElement("ellipse");
			elt.setAttribute("cx", format(x));
			elt.setAttribute("cy", format(y));
			elt.setAttribute("rx", format(xRadius));
			elt.setAttribute("ry", format(yRadius));
			elt.setAttribute("fill", fill);
			elt.setAttribute("style", getStyle());
			addFilterShadowId(elt, deltaShadow);
			getG().appendChild(elt);
		}
		ensureVisible(x + xRadius + deltaShadow * 2, y + yRadius + deltaShadow * 2);
	}

	public void svgArcEllipse(double rx, double ry, double x1, double y1, double x2, double y2) {
		if (hidden == false) {
			final String path = "M" + format(x1) + "," + format(y1) + " A" + format(rx) + "," + format(ry) + " 0 0 0 "
					+ format(x2) + " " + format(y2);
			final Element elt = (Element) document.createElement("path");
			elt.setAttribute("d", path);
			elt.setAttribute("fill", fill);
			elt.setAttribute("style", getStyle());
			getG().appendChild(elt);
		}
		ensureVisible(x1, y1);
		ensureVisible(x2, y2);
	}

	private Map<List<Object>, String> gradients = new HashMap<List<Object>, String>();

	public String createSvgGradient(String color1, String color2, char policy) {
		final List<Object> key = Arrays.asList((Object) color1, color2, policy);
		String id = gradients.get(key);
		if (id == null) {
			final Element elt = (Element) document.createElement("linearGradient");
			if (policy == '|') {
				elt.setAttribute("x1", "0%");
				elt.setAttribute("y1", "50%");
				elt.setAttribute("x2", "100%");
				elt.setAttribute("y2", "50%");
			} else if (policy == '\\') {
				elt.setAttribute("x1", "0%");
				elt.setAttribute("y1", "100%");
				elt.setAttribute("x2", "100%");
				elt.setAttribute("y2", "0%");
			} else if (policy == '-') {
				elt.setAttribute("x1", "50%");
				elt.setAttribute("y1", "0%");
				elt.setAttribute("x2", "50%");
				elt.setAttribute("y2", "100%");
			} else {
				elt.setAttribute("x1", "0%");
				elt.setAttribute("y1", "0%");
				elt.setAttribute("x2", "100%");
				elt.setAttribute("y2", "100%");
			}
			id = "gr" + gradients.size();
			gradients.put(key, id);
			elt.setAttribute("id", id);

			final Element stop1 = (Element) document.createElement("stop");
			stop1.setAttribute("stop-color", color1);
			stop1.setAttribute("offset", "0%");
			final Element stop2 = (Element) document.createElement("stop");
			stop2.setAttribute("stop-color", color2);
			stop2.setAttribute("offset", "100%");

			elt.appendChild(stop1);
			elt.appendChild(stop2);
			defs.appendChild(elt);
		}
		return id;
	}

	public final void setFillColor(String fill) {
		this.fill = fill == null ? "none" : fill;
	}

	public final void setStrokeColor(String stroke) {
		this.stroke = stroke == null ? "none" : stroke;
	}

	public final void setStrokeWidth(double strokeWidth, String strokeDasharray) {
		this.strokeWidth = "" + (scale * strokeWidth);
		this.strokeDasharray = strokeDasharray;
	}

	public void closeLink() {
		if (pendingLink2.size() > 0) {
			final Element element = pendingLink2.get(0);
			pendingLink2.remove(0);
			getG().appendChild(element);
		}
	}

	private final List<Element> pendingLink2 = new ArrayList<Element>();

	public void openLink(String url, String title, String target) {
		if (url == null) {
			throw new IllegalArgumentException();
		}

		if (pendingLink2.size() > 0) {
			closeLink();
		}

		pendingLink2.add(0, (Element) document.createElement("a"));
		pendingLink2.get(0).setAttribute("target", target);
		pendingLink2.get(0).setAttribute("xlink:href", url);
		if (title == null) {
			pendingLink2.get(0).setAttribute("xlink:title", url);
		} else {
			title = title.replaceAll("\\\\n", "\n");
			pendingLink2.get(0).setAttribute("xlink:title", title);
		}
	}

	public final Element getG() {
		if (pendingLink2.size() == 0) {
			return gRoot;
		}
		return pendingLink2.get(0);
	}

	public void svgRectangle(double x, double y, double width, double height, double rx, double ry, double deltaShadow) {
		if (height <= 0 || width <= 0) {
			throw new IllegalArgumentException();
		}
		manageShadow(deltaShadow);
		if (hidden == false) {
			final Element elt = createRectangleInternal(x, y, width, height);
			addFilterShadowId(elt, deltaShadow);
			if (rx > 0 && ry > 0) {
				elt.setAttribute("rx", format(rx));
				elt.setAttribute("ry", format(ry));
			}

			getG().appendChild(elt);
		}
		ensureVisible(x + width + 2 * deltaShadow, y + height + 2 * deltaShadow);
	}

	private Element createRectangleInternal(double x, double y, double width, double height) {
		final Element elt = (Element) document.createElement("rect");
		elt.setAttribute("x", format(x));
		elt.setAttribute("y", format(y));
		elt.setAttribute("width", format(width));
		elt.setAttribute("height", format(height));
		elt.setAttribute("fill", fill);
		elt.setAttribute("style", getStyle());
		return elt;
	}

	public void svgLine(double x1, double y1, double x2, double y2, double deltaShadow) {
		manageShadow(deltaShadow);
		if (hidden == false) {
			final Element elt = (Element) document.createElement("line");
			elt.setAttribute("x1", format(x1));
			elt.setAttribute("y1", format(y1));
			elt.setAttribute("x2", format(x2));
			elt.setAttribute("y2", format(y2));
			elt.setAttribute("style", getStyle());
			addFilterShadowId(elt, deltaShadow);
			getG().appendChild(elt);
		}
		ensureVisible(x1 + 2 * deltaShadow, y1 + 2 * deltaShadow);
		ensureVisible(x2 + 2 * deltaShadow, y2 + 2 * deltaShadow);
	}

	private String getStyle() {
		return getStyleInternal(stroke, strokeWidth, strokeDasharray);
	}

	private static String getStyleInternal(String color, String strokeWidth, String strokeDasharray) {
		final StringBuilder style = new StringBuilder("stroke: " + color + "; stroke-width: " + strokeWidth + ";");
		if (strokeDasharray != null) {
			style.append(" stroke-dasharray: " + strokeDasharray + ";");
		}
		return style.toString();
	}

	public void svgPolygon(double deltaShadow, double... points) {
		manageShadow(deltaShadow);
		if (hidden == false) {
			final Element elt = (Element) document.createElement("polygon");
			final StringBuilder sb = new StringBuilder();
			for (double coord : points) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(format(coord));
			}
			elt.setAttribute("points", sb.toString());
			elt.setAttribute("fill", fill);
			elt.setAttribute("style", getStyle());
			addFilterShadowId(elt, deltaShadow);
			getG().appendChild(elt);
		}

		for (int i = 0; i < points.length; i += 2) {
			ensureVisible(points[i] + 2 * deltaShadow, points[i + 1] + 2 * deltaShadow);
		}

	}

	public void text(String text, double x, double y, String fontFamily, int fontSize, String fontWeight,
			String fontStyle, String textDecoration, double textLength, Map<String, String> attributes,
			String textBackColor) {
		if (hidden == false) {
			final Element elt = (Element) document.createElement("text");
			elt.setAttribute("x", format(x));
			elt.setAttribute("y", format(y));
			elt.setAttribute("fill", fill);
			elt.setAttribute("font-size", format(fontSize));
			// elt.setAttribute("text-anchor", "middle");
			elt.setAttribute("lengthAdjust", "spacingAndGlyphs");
			elt.setAttribute("textLength", format(textLength));
			if (fontWeight != null) {
				elt.setAttribute("font-weight", fontWeight);
			}
			if (fontStyle != null) {
				elt.setAttribute("font-style", fontStyle);
			}
			if (textDecoration != null) {
				elt.setAttribute("text-decoration", textDecoration);
			}
			if (fontFamily != null) {
				elt.setAttribute("font-family", fontFamily);
			}
			if (textBackColor != null) {
				final String backFilterId = getFilterBackColor(textBackColor);
				elt.setAttribute("filter", "url(#" + backFilterId + ")");
			}
			for (Map.Entry<String, String> ent : attributes.entrySet()) {
				elt.setAttribute(ent.getKey(), ent.getValue());
			}
			elt.setTextContent(text);
			getG().appendChild(elt);

			if (textDecoration != null && textDecoration.contains("underline")) {
				final double delta = 2;
				final Element elt2 = (Element) document.createElement("line");
				elt2.setAttribute("x1", format(x));
				elt2.setAttribute("y1", format(y + delta));
				elt2.setAttribute("x2", format(x + textLength));
				elt2.setAttribute("y2", format(y + delta));
				elt2.setAttribute("style", getStyleInternal(fill, "1.0", null));
				getG().appendChild(elt2);
			}

		}
		ensureVisible(x, y);
		ensureVisible(x + textLength, y);
	}

	private final Map<String, String> filterBackColor = new HashMap<String, String>();

	private String getIdFilterBackColor(String color) {
		String result = filterBackColor.get(color);
		if (result == null) {
			result = filterUid + filterBackColor.size();
			filterBackColor.put(color, result);
		}
		return result;
	}

	private String getFilterBackColor(String color) {
		String id = filterBackColor.get(color);
		if (id != null) {
			return id;
		}
		id = getIdFilterBackColor(color);
		final Element filter = (Element) document.createElement("filter");
		filter.setAttribute("id", id);
		filter.setAttribute("x", "0");
		filter.setAttribute("y", "0");
		filter.setAttribute("width", "1");
		filter.setAttribute("height", "1");
		addFilter(filter, "feFlood", "flood-color", color);
		addFilter(filter, "feComposite", "in", "SourceGraphic");
		defs.appendChild(filter);
		return id;
	}

	private Transformer getTransformer() throws TransformerException {
		// Get a TransformerFactory object.
		final TransformerFactory xformFactory = TransformerFactory.newInstance();
		// try {
		// final Class<?> factoryClass = Class
		// .forName("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
		// xformFactory = (TransformerFactory) factoryClass.newInstance();
		// } catch (Exception e) {
		// xformFactory = TransformerFactory.newInstance();
		// }
		Log.info("TransformerFactory=" + xformFactory.getClass());

		// Get an XSL Transformer object.
		final Transformer transformer = xformFactory.newTransformer();
		Log.info("Transformer=" + transformer.getClass());

		// // Sets the standalone property in the first line of
		// // the output file.
		transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
		//
		// Properties proprietes = new Properties();
		// proprietes.put("standalone", "yes");
		// transformer.setOutputProperties(proprietes);
		//
		// transformer.setParameter(OutputKeys.STANDALONE, "yes");

		return transformer;
	}

	public void createXml(OutputStream os) throws TransformerException, IOException {
		if (images.size() == 0) {
			createXmlInternal(os);
			return;
		}
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		createXmlInternal(baos);
		String s = new String(baos.toByteArray());
		for (Map.Entry<String, String> ent : images.entrySet()) {
			final String k = "\\<" + ent.getKey() + "/\\>";
			s = s.replaceAll(k, ent.getValue());
		}
		os.write(s.getBytes());
	}

	private void createXmlInternal(OutputStream os) throws TransformerException {
		// // Add lines
		// for (Line l : lines) {
		// l.drawNow();
		// }

		// Get a DOMSource object that represents the
		// Document object
		final DOMSource source = new DOMSource(document);

		final int maxXscaled = (int) (maxX * scale);
		final int maxYscaled = (int) (maxY * scale);
		String style = "width:" + maxXscaled + "px;height:" + maxYscaled + "px;";
		if (backcolor != null) {
			style += "background:" + backcolor + ";";
		}
		root.setAttribute("style", style);
		root.setAttribute("width", format(maxX) + "px");
		root.setAttribute("height", format(maxY) + "px");
		root.setAttribute("viewBox", "0 0 " + maxXscaled + " " + maxYscaled);

		if (pendingBackground != null) {
			pendingBackground.setAttribute("width", format(maxX));
			pendingBackground.setAttribute("height", format(maxY));

		}

		// Get a StreamResult object that points to the
		// screen. Then transform the DOM sending XML to
		// the screen.
		final StreamResult scrResult = new StreamResult(os);
		getTransformer().transform(source, scrResult);
	}

	public void svgPath(double x, double y, UPath path, double deltaShadow) {
		manageShadow(deltaShadow);
		ensureVisible(x, y);
		final StringBuilder sb = new StringBuilder();
		for (USegment seg : path) {
			final USegmentType type = seg.getSegmentType();
			final double coord[] = seg.getCoord();
			if (type == USegmentType.SEG_MOVETO) {
				sb.append("M" + format(coord[0] + x) + "," + format(coord[1] + y) + " ");
				ensureVisible(coord[0] + x + 2 * deltaShadow, coord[1] + y + 2 * deltaShadow);
			} else if (type == USegmentType.SEG_LINETO) {
				sb.append("L" + format(coord[0] + x) + "," + format(coord[1] + y) + " ");
				ensureVisible(coord[0] + x + 2 * deltaShadow, coord[1] + y + 2 * deltaShadow);
			} else if (type == USegmentType.SEG_QUADTO) {
				sb.append("Q" + format(coord[0] + x) + "," + format(coord[1] + y) + " " + format(coord[2] + x) + ","
						+ format(coord[3] + y) + " ");
				ensureVisible(coord[0] + x + 2 * deltaShadow, coord[1] + y + 2 * deltaShadow);
				ensureVisible(coord[2] + x + 2 * deltaShadow, coord[3] + y + 2 * deltaShadow);
			} else if (type == USegmentType.SEG_CUBICTO) {
				sb.append("C" + format(coord[0] + x) + "," + format(coord[1] + y) + " " + format(coord[2] + x) + ","
						+ format(coord[3] + y) + " " + format(coord[4] + x) + "," + format(coord[5] + y) + " ");
				ensureVisible(coord[0] + x + 2 * deltaShadow, coord[1] + y + 2 * deltaShadow);
				ensureVisible(coord[2] + x + 2 * deltaShadow, coord[3] + y + 2 * deltaShadow);
				ensureVisible(coord[4] + x + 2 * deltaShadow, coord[5] + y + 2 * deltaShadow);
			} else if (type == USegmentType.SEG_ARCTO) {
				sb.append("A" + format(coord[0]) + "," + format(coord[1]) + " " + format(coord[2]) + ","
						+ format(coord[3]) + " " + format(coord[4]) + "," + format(coord[5] + x) + ","
						+ format(coord[6] + y) + " ");
				ensureVisible(coord[5] + coord[0] + x + 2 * deltaShadow, coord[6] + coord[1] + y + 2 * deltaShadow);
			} else if (type == USegmentType.SEG_CLOSE) {
				// Nothing
			} else {
				Log.println("unknown " + seg);
			}

		}
		if (hidden == false) {
			final Element elt = (Element) document.createElement("path");
			elt.setAttribute("d", sb.toString());
			elt.setAttribute("style", getStyle());
			elt.setAttribute("fill", fill);
			addFilterShadowId(elt, deltaShadow);
			getG().appendChild(elt);
		}
	}

	private void addFilterShadowId(final Element elt, double deltaShadow) {
		if (deltaShadow > 0) {
			elt.setAttribute("filter", "url(#" + shadowId + ")");
		}
	}

	private StringBuilder currentPath = null;

	public void newpath() {
		currentPath = new StringBuilder();

	}

	public void moveto(double x, double y) {
		currentPath.append("M" + format(x) + "," + format(y) + " ");
		ensureVisible(x, y);
	}

	public void lineto(double x, double y) {
		currentPath.append("L" + format(x) + "," + format(y) + " ");
		ensureVisible(x, y);
	}

	public void closepath() {
		currentPath.append("Z ");

	}

	public void curveto(double x1, double y1, double x2, double y2, double x3, double y3) {
		currentPath.append("C" + format(x1) + "," + format(y1) + " " + format(x2) + "," + format(y2) + " " + format(x3)
				+ "," + format(y3) + " ");
		ensureVisible(x1, y1);
		ensureVisible(x2, y2);
		ensureVisible(x3, y3);

	}

	public void quadto(double x1, double y1, double x2, double y2) {
		currentPath.append("Q" + format(x1) + "," + format(y1) + " " + format(x2) + "," + format(y2) + " ");
		ensureVisible(x1, y1);
		ensureVisible(x2, y2);
	}

	private String format(double x) {
		return EpsGraphics.format(x * scale);
	}

	public void fill(int windingRule) {
		if (hidden == false) {
			final Element elt = (Element) document.createElement("path");
			elt.setAttribute("d", currentPath.toString());
			// elt elt.setAttribute("style", getStyle());
			getG().appendChild(elt);
		}
		currentPath = null;

	}

	public void svgImage(BufferedImage image, double x, double y) throws IOException {
		if (hidden == false) {
			final Element elt = (Element) document.createElement("image");
			elt.setAttribute("width", format(image.getWidth()));
			elt.setAttribute("height", format(image.getHeight()));
			elt.setAttribute("x", format(x));
			elt.setAttribute("y", format(y));
			final String s = toBase64(image);
			elt.setAttribute("xlink:href", "data:image/png;base64," + s);
			getG().appendChild(elt);
		}
		ensureVisible(x, y);
		ensureVisible(x + image.getWidth(), y + image.getHeight());
	}

	private final Map<String, String> images = new HashMap<String, String>();

	public void svgImage(String svg, double x, double y) {
		if (svg.startsWith("<svg>") == false) {
			throw new IllegalArgumentException();
		}
		if (hidden == false) {
			final String pos = "<svg x=\"" + format(x) + "\" y=\"" + format(y) + "\">";
			svg = pos + svg.substring(5);
			// System.err.println("svg=" + svg);
			// System.err.println("x=" + x);
			// System.err.println("y=" + y);
			final String key = "imagesvginlined" + images.size();
			final Element elt = (Element) document.createElement(key);
			getG().appendChild(elt);
			images.put(key, svg);
		}
		ensureVisible(x, y);
	}

	private String toBase64(BufferedImage image) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		final byte data[] = baos.toByteArray();
		return new String(Base64Coder.encode(data));
	}

	// Shadow

	private boolean withShadow = false;

	private void manageShadow(double deltaShadow) {
		if (deltaShadow != 0) {
			if (withShadow == false) {
				// <filter id="f1" x="0" y="0" width="120%" height="120%">
				final Element filter = (Element) document.createElement("filter");
				filter.setAttribute("id", shadowId);
				filter.setAttribute("x", "-1");
				filter.setAttribute("y", "-1");
				filter.setAttribute("width", "300%");
				filter.setAttribute("height", "300%");
				addFilter(filter, "feGaussianBlur", "result", "blurOut", "stdDeviation", "" + (2 * scale));
				addFilter(filter, "feColorMatrix", "type", "matrix", "in", "blurOut", "result", "blurOut2", "values",
						"0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 .4 0");
				addFilter(filter, "feOffset", "result", "blurOut3", "in", "blurOut2", "dx", "" + (4 * scale), "dy", ""
						+ (4 * scale));
				addFilter(filter, "feBlend", "in", "SourceGraphic", "in2", "blurOut3", "mode", "normal");
				defs.appendChild(filter);

			}
			withShadow = true;
		}
	}

	private void addFilter(Element filter, String name, String... data) {
		final Element elt = (Element) document.createElement(name);
		for (int i = 0; i < data.length; i += 2) {
			elt.setAttribute(data[i], data[i + 1]);
		}
		filter.appendChild(elt);
	}

	private boolean hidden;

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public void addComment(String comment) {
		final Comment commentElement = document.createComment(comment);
		getG().appendChild(commentElement);
	}

}
