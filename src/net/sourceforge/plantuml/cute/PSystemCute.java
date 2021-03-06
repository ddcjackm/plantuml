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
 * Revision $Revision: 4041 $
 *
 */
package net.sourceforge.plantuml.cute;

import java.io.IOException;
import java.io.OutputStream;
import java.util.StringTokenizer;

import net.sourceforge.plantuml.AbstractPSystem;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.StringUtils;
import net.sourceforge.plantuml.core.DiagramDescription;
import net.sourceforge.plantuml.core.DiagramDescriptionImpl;
import net.sourceforge.plantuml.core.ImageData;
import net.sourceforge.plantuml.ugraphic.ColorMapperIdentity;
import net.sourceforge.plantuml.ugraphic.ImageBuilder;

public class PSystemCute extends AbstractPSystem {

	// private final List<Positionned> shapes = new ArrayList<Positionned>();
	// private final Map<String, Group> groups = new HashMap<String, Group>();
	private final Group root = Group.createRoot();
	private Group currentGroup = root;

	public PSystemCute() {
	}

	public DiagramDescription getDescription() {
		return new DiagramDescriptionImpl("(Cute)", getClass());
	}

	public void doCommandLine(String line) {
		line = StringUtils.trin(line);
		if (line.length()==0 || line.startsWith("'")) {
			return;
		}
		if (line.startsWith("group ")) {
			final StringTokenizer st = new StringTokenizer(line);
			st.nextToken();
			final String groupName = st.nextToken();
			currentGroup = currentGroup.createChild(groupName);
		} else if (line.startsWith("}")) {
			currentGroup = currentGroup.getParent();
		} else {
			final Positionned shape = new CuteShapeFactory(currentGroup.getChildren()).createCuteShapePositionned(line);
			// if (currentGroup == null) {
			// shapes.add(shape);
			// } else {
			currentGroup.add(shape);
			// }
		}
	}

	public ImageData exportDiagram(OutputStream os, int num, FileFormatOption fileFormat) throws IOException {
		final ImageBuilder builder = new ImageBuilder(new ColorMapperIdentity(), 1.0, null, null, null, 10, 10, null, false);
		builder.setUDrawable(root);
		return builder.writeImageTOBEMOVED(fileFormat, os);
	}
}
