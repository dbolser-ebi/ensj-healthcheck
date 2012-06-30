/*
 Copyright (C) 2004 EBI, GRL
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.ensembl.healthcheck.testcase.compara;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.commons.lang.StringUtils;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.Repair;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;
import org.ensembl.healthcheck.testcase.compara.MethodLinkSpeciesSetTag;
import org.ensembl.healthcheck.util.DBUtils;

/**
 * An EnsEMBL Healthcheck test case that looks for broken foreign-key relationships.
 */

public class MLSSTagGERPMSA extends MethodLinkSpeciesSetTag {


	/**
	 * Create an ForeignKeyMethodLinkId that applies to a specific set of databases.
	 */
	public MLSSTagGERPMSA() {

		addToGroup("compara_genomic");
		setDescription("Tests that proper max_alignment_length have been defined.");
		setDescription("Check method_link_species_set_tag table for the right GERP <-> MSA links and max alignment lengths");
		setTeamResponsible(Team.COMPARA);
		tagToCheck = "msa_mlss_id";
	}


	/**
	 * Check that the each conservation score MethodLinkSpeciesSet obejct has a link to a multiple alignment MethodLinkSpeciesSet in
	 * the method_link_species_set_tag table.
	 */
	boolean doCheck(Connection con) {

		boolean result = true;

		// get all the links between conservation scores and multiple genomic alignments
		String sql = "SELECT mlss1.method_link_species_set_id," + " mlss2.method_link_species_set_id, ml1.type, ml2.type, count(*)"
				+ " FROM method_link ml1, method_link_species_set mlss1, method_link ml2," + " method_link_species_set mlss2 WHERE mlss1.method_link_id = ml1.method_link_id "
				+ " AND (ml1.class = \"ConservationScore.conservation_score\" OR ml1.class = \"ConstrainedElement.constrained_element\" )"
				+ " AND mlss1.species_set_id = mlss2.species_set_id AND mlss2.method_link_id = ml2.method_link_id"
				+ " AND (ml2.class = \"GenomicAlignBlock.multiple_alignment\" OR ml2.class LIKE \"GenomicAlignTree.%\") GROUP BY mlss1.method_link_species_set_id";

		try {
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if (rs.getInt(5) > 1) {
					ReportManager.problem(this, con, "MethodLinkSpeciesSet " + rs.getString(1) + " links to several multiple alignments!");
					result = false;
				} else if (rs.getString(3).equals("GERP_CONSERVATION_SCORE") || rs.getString(3).equals("GERP_CONSTRAINED_ELEMENT")) {
					MetaEntriesToAdd.put(new Integer(rs.getInt(1)).toString(), new Integer(rs.getInt(2)).toString());
				} else {
					ReportManager.problem(this, con, "Using " + rs.getString(3) + " method_link_type is not supported by this healthcheck");
					result = false;
				}
			}
			rs.close();
			stmt.close();
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
		}

		// get all the values currently stored in the DB
		sql = "SELECT method_link_species_set_id, value, COUNT(*) FROM method_link_species_set_tag WHERE tag = \"msa_mlss_id\" GROUP BY method_link_species_set_id";

		try {
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if (rs.getInt(3) != 1) {
					// Delete all current entries. The right entry will be added
					MetaEntriesToRemove.put(rs.getString(1), rs.getString(2));
					System.out.println("Too many entries for " + rs.getString(1));
				} else if (MetaEntriesToAdd.containsKey(rs.getString(1))) {
					// Entry matches one of the required entries. Update if needed.
					if (!MetaEntriesToAdd.get(rs.getString(1)).equals(rs.getString(2))) {
						MetaEntriesToUpdate.put(rs.getString(1), MetaEntriesToAdd.get(rs.getString(1)));
						System.out.println("Replace entries for " + rs.getString(1));
					}
					// Remove this entry from the set of entries to be added (as it already exits!)
					MetaEntriesToAdd.remove(rs.getString(1));
				} else {
					// Entry is out-to-date
					MetaEntriesToRemove.put(rs.getString(1), rs.getString(2));
					System.out.println("Remove entries for " + rs.getString(1));
				}
			}
			rs.close();
			stmt.close();
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
		}

		return result;

	} // ---------------------------------------------------------------------

} 
