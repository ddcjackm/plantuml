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
 * Revision $Revision: 6170 $
 *
 */
package net.sourceforge.plantuml.braille;

import net.sourceforge.plantuml.graphic.HtmlColorUtils;
import net.sourceforge.plantuml.graphic.UDrawable;
import net.sourceforge.plantuml.ugraphic.UChangeBackColor;
import net.sourceforge.plantuml.ugraphic.UChangeColor;
import net.sourceforge.plantuml.ugraphic.UEllipse;
import net.sourceforge.plantuml.ugraphic.UGraphic;
import net.sourceforge.plantuml.ugraphic.UTranslate;

public class BrailleDrawer implements UDrawable {

	private final BrailleGrid grid;
	private final double step = 9;
	private final double spotSize = 5;

	public BrailleDrawer(BrailleGrid grid) {
		this.grid = grid;
	}

	public void drawU(UGraphic ug) {
		ug = ug.apply(new UChangeColor(HtmlColorUtils.BLACK)).apply(new UChangeBackColor(HtmlColorUtils.BLACK));
		for (int x = grid.getMinX(); x <= grid.getMaxX(); x++) {
			for (int y = grid.getMinY(); y <= grid.getMaxY(); y++) {
				if (grid.getState(x, y)) {
					drawCircle(ug, x, y);
				}
			}
		}
	}

	private void drawCircle(UGraphic ug, int x, int y) {
		final double cx = x * step;
		final double cy = y * step;
		ug.apply(new UTranslate(cx, cy)).draw(new UEllipse(spotSize, spotSize));
	}

}
