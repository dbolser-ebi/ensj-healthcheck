/*
 * Copyright (C) 2003 EBI, GRL
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.ensembl.healthcheck.testcase.generic;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.DatabaseType;
import org.ensembl.healthcheck.testcase.Priority;
import org.ensembl.healthcheck.DatabaseType;

/**
 * Compare the CCDS in the current database with those from the equivalent
 * database on the secondary server.
 */

public class ComparePreviousVersionCCDS extends ComparePreviousVersionBase {

	/**
	 * Create a new testcase.
	 */
	public ComparePreviousVersionCCDS() {

		addToGroup("post_genebuild");
		addToGroup("release");
		addToGroup("core_xrefs");
		setDescription("Compare the CCDS in the current database with those from the equivalent database on the secondary server");
		setPriority(Priority.AMBER);
		setEffect("Indicates that the CCDS object xrefs have changed between releases; may be due to a problem, or be expected, in which case the result should be annotated appropritately");
                setTeamResponsible("Core Genebuilders")

	}

	// ----------------------------------------------------------------------

	/**
	 * This only applies to core databases.
	 */
	public void types() {

		removeAppliesToType(DatabaseType.OTHERFEATURES);
		removeAppliesToType(DatabaseType.CDNA);
		removeAppliesToType(DatabaseType.VEGA);
		removeAppliesToType(DatabaseType.SANGER_VEGA);
		removeAppliesToType(DatabaseType.RNASEQ);

	}

	// ----------------------------------------------------------------------

	protected Map<String,Integer> getCounts(DatabaseRegistryEntry dbre) {

		Connection con = dbre.getConnection();

		Map<String, Integer> counts = new HashMap<String, Integer>();

		// and total number of associations with genes, transcripts and translations
		String sql = " FROM gene g, transcript tr, translation tl, object_xref ox, xref x, external_db e WHERE x.xref_id=ox.xref_id AND x.external_db_id=e.external_db_id AND e.db_name='CCDS' AND g.gene_id =tr.gene_id AND tl.translation_id=ox.ensembl_id AND ox.ensembl_object_type='Translation' and tl.transcript_id=tr.transcript_id";
		int genes = getRowCount(con, "SELECT COUNT(DISTINCT(g.gene_id))" + sql);
		counts.put("CCDS-gene associations", genes);

		int transcripts = getRowCount(con, "SELECT COUNT(DISTINCT(tr.transcript_id))" + sql);
		counts.put("CCDS-transcript associations", transcripts);

		int translations = getRowCount(con, "SELECT COUNT(DISTINCT(tl.translation_id))" + sql);
		counts.put("CCDS-translation associations", translations);

		return counts;

	}

	// ------------------------------------------------------------------------

	protected String entityDescription() {

		return "";

	}

	// ------------------------------------------------------------------------

	protected double threshold() {

		return 1;

	}

	// ------------------------------------------------------------------------

} // ComparePreviousVersionCCDS
