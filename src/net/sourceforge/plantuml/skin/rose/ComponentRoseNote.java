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
 * Revision $Revision: 19109 $
 *
 */
package net.sourceforge.plantuml.skin.rose;

import net.sourceforge.plantuml.ISkinSimple;
import net.sourceforge.plantuml.creole.Stencil;
import net.sourceforge.plantuml.cucadiagram.Display;
import net.sourceforge.plantuml.graphic.FontConfiguration;
import net.sourceforge.plantuml.graphic.HorizontalAlignment;
import net.sourceforge.plantuml.graphic.StringBounder;
import net.sourceforge.plantuml.graphic.SymbolContext;
import net.sourceforge.plantuml.skin.AbstractTextualComponent;
import net.sourceforge.plantuml.skin.Area;
import net.sourceforge.plantuml.ugraphic.UGraphic;
import net.sourceforge.plantuml.ugraphic.UGraphicStencil;
import net.sourceforge.plantuml.ugraphic.ULine;
import net.sourceforge.plantuml.ugraphic.UPolygon;
import net.sourceforge.plantuml.ugraphic.UStroke;
import net.sourceforge.plantuml.ugraphic.UTranslate;

final public class ComponentRoseNote extends AbstractTextualComponent implements Stencil {

	private final int cornersize = 10;
	private final double paddingX;
	private final double paddingY;
	private final SymbolContext symbolContext;

	public ComponentRoseNote(SymbolContext symbolContext, FontConfiguration font, Display strings, double paddingX, double paddingY,
			ISkinSimple spriteContainer) {
		super(strings, font, HorizontalAlignment.LEFT, 6, 15, 5, spriteContainer, 0, true, null, null);
		this.paddingX = paddingX;
		this.paddingY = paddingY;
		this.symbolContext = symbolContext;
	}

	@Override
	final public double getPreferredWidth(StringBounder stringBounder) {
		final double result = getTextWidth(stringBounder) + 2 * getPaddingX() + symbolContext.getDeltaShadow();
		return result;
	}

	@Override
	final public double getPreferredHeight(StringBounder stringBounder) {
		return getTextHeight(stringBounder) + 2 * getPaddingY() + symbolContext.getDeltaShadow();
	}

	@Override
	public double getPaddingX() {
		return paddingX;
	}

	@Override
	public double getPaddingY() {
		return paddingY;
	}

	@Override
	protected void drawInternalU(UGraphic ug, Area area) {

		final StringBounder stringBounder = ug.getStringBounder();
		final int textHeight = (int) getTextHeight(stringBounder);

		int x2 = (int) getTextWidth(stringBounder);
		final double diffX = area.getDimensionToUse().getWidth() - getPreferredWidth(stringBounder);
		if (diffX < 0) {
			throw new IllegalArgumentException();
		}
		if (area.getDimensionToUse().getWidth() > getPreferredWidth(stringBounder)) {
			x2 = (int) (area.getDimensionToUse().getWidth() - 2 * getPaddingX());
		}

		final UPolygon polygon = new UPolygon();
		polygon.addPoint(0, 0);
		polygon.addPoint(0, textHeight);
		polygon.addPoint(x2, textHeight);
		polygon.addPoint(x2, cornersize);
		polygon.addPoint(x2 - cornersize, 0);
		polygon.addPoint(0, 0);
		polygon.setDeltaShadow(symbolContext.getDeltaShadow());

		ug = symbolContext.apply(ug);
		ug.draw(polygon);

		ug.apply(new UTranslate(x2 - cornersize, 0)).draw(new ULine(0, cornersize));
		ug.apply(new UTranslate(x2, cornersize)).draw(new ULine(-cornersize, 0));
		UGraphic ug2 = new UGraphicStencil(ug, this, new UStroke());
		ug2 = ug2.apply(new UTranslate(getMarginX1() + diffX / 2, getMarginY()));

		getTextBlock().drawU(ug2);

	}

	public double getStartingX(StringBounder stringBounder, double y) {
		return 0;
	}

	public double getEndingX(StringBounder stringBounder, double y) {
		return getTextWidth(stringBounder);
	}

}
