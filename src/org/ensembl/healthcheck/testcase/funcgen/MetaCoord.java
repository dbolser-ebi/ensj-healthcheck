/*
 * Copyright [1999-2014] Wellcome Trust Sanger Institute and the EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.ensembl.healthcheck.testcase.funcgen;

/** Can we not just modify the generic version to take a schema/dbtype type
	And store the variables in a hash? Set groups varaible dependant on dbtype?
	What is calling this?
*/

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;
import org.ensembl.healthcheck.util.DBUtils;

/**
 * Check that meta_coord table contains entries for all the coordinate systems
 * that all the features are stored in.
 */
public class MetaCoord extends SingleDatabaseTestCase {

	private String[] featureTables = getFuncgenFeatureTables();

	/**
	 * Create a new instance of MetaCoord.
	 */
	public MetaCoord() {

		addToGroup("funcgen");
		addToGroup("funcgen-release");
		setDescription("Check that meta_coord table contains entries for all the coordinate systems that all the features are stored in");
                setTeamResponsible(Team.FUNCGEN);
	}

	/**
	 * Run the test.
	 * 
	 * @param dbre
	 *          The database to use.
	 * @return true if the test passed.
	 * 
	 */
	public boolean run(DatabaseRegistryEntry dbre) {

		boolean result = true;

		Connection con = dbre.getConnection();

		// coordSystems is a hash of lists of coordinate systems that each feature
		// table contains
		Map coordSystems = new HashMap(); 

		try {

			Statement stmt = con.createStatement();

			// build up a list of all the coordinate systems that are in the various
			// feature tables
			for (int tableIndex = 0; tableIndex < featureTables.length; tableIndex++) {

				String tableName = featureTables[tableIndex];
				String sql = "SELECT DISTINCT(sr.coord_system_id) FROM seq_region sr, " + tableName
						+ " f WHERE sr.seq_region_id = f.seq_region_id";

				logger.finest("Getting feature coordinate systems for " + tableName);
				ResultSet rs = stmt.executeQuery(sql);

				while (rs.next()) {
					String coordSystemID = rs.getString(1);
					logger.finest("Added feature coordinate system for " + tableName + ": " + coordSystemID);
					// check that the meta_coord table has an entry corresponding to this
					int mc = DBUtils.getRowCount(con, "SELECT COUNT(*) FROM meta_coord WHERE coord_system_id=" + coordSystemID + " AND table_name='"
							+ tableName + "'");
					if (mc == 0) {
						ReportManager.problem(this, con, "No entry for coordinate system with ID " + coordSystemID + " for " + tableName
								+ " in meta_coord");
						result = false;
					} else if (mc > 1) {
						ReportManager.problem(this, con, "Coordinate system with ID " + coordSystemID + " duplicated for " + tableName
								+ " in meta_coord");
						result = false;
					} else {
						ReportManager.correct(this, con, "Coordinate system with ID " + coordSystemID + " for table " + tableName
								+ " has an entry in meta_coord");
					}

					// store in coordSystems map - create List if necessary
					List csList = (ArrayList) coordSystems.get(tableName);
					if (csList == null) {
						csList = new ArrayList();
					}
					csList.add(coordSystemID);
					coordSystems.put(tableName, csList);
				}

				rs.close();

			}

			// check that every meta_coord table entry refers to a coordinate system
			// that is used in a feature
			// if this isn't true it's not fatal but should be flagged
			String sql = "SELECT * FROM meta_coord";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String tableName = rs.getString("table_name");
				String csID = rs.getString("coord_system_id");
				logger.finest("Checking for coord_system_id " + csID + " in " + tableName);
				List featureCSs = (ArrayList) coordSystems.get(tableName);
				if (featureCSs != null && !featureCSs.contains(csID)) {
					ReportManager.problem(this, con, "meta_coord has entry for coord_system ID " + csID + " in " + tableName
							+ " but this coordinate system is not actually used in " + tableName);
					result = false;
				}

			}

			rs.close();
			stmt.close();

			// check that there are no null max_length entries
			result &= checkNoNulls(con, "meta_coord", "max_length");

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result;

	}

}
