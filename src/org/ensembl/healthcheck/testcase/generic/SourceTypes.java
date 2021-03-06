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


package org.ensembl.healthcheck.testcase.generic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.DatabaseType;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Species;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.Priority;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;
import org.ensembl.healthcheck.util.DBUtils;


/**
 * Check that all chromosomes have at least some genes with certain analyses.
 */
public class SourceTypes extends SingleDatabaseTestCase {

	/**
	 * Creates a new instance of AnalysisTypes
	 */
	public SourceTypes() {

		addToGroup("post_genebuild");
		addToGroup("pre-compara-handover");
		addToGroup("post-compara-handover");
                addToGroup("post-projection");
		setDescription("Check that all chromsosomes have at least some genes with certain sources.");
		setPriority(Priority.AMBER);
		setEffect("Some genes may have only Ensembl or Havana annotation.");
		setFix("Possibly indicates a problem with the Havana/Ensembl merge pipeline");
		setTeamResponsible(Team.GENEBUILD);

	}

	/**
	 * This only applies to core databases.
	 */
	public void types() {

		removeAppliesToType(DatabaseType.ESTGENE);
		removeAppliesToType(DatabaseType.CDNA);
		removeAppliesToType(DatabaseType.VEGA);
		removeAppliesToType(DatabaseType.SANGER_VEGA);
		removeAppliesToType(DatabaseType.OTHERFEATURES);
		removeAppliesToType(DatabaseType.RNASEQ);

	}

	/** 
	 * Run the test.
	 * 
	 * @param dbr
	 *          The database registry containing all the specified databases.
	 */
	public boolean run(DatabaseRegistryEntry dbre) {

		// only applies to human, mouse and zebrafish at the moment
		Species species = dbre.getSpecies();
                boolean is_merged = testMerged(species);

		if (!is_merged) {

			return true;

		}

		boolean result = true;

                Connection con = dbre.getConnection();

                result &= geneSources(con);
                result &= transcriptSources(con);
                result &= geneTranscriptSources(con);

                return result;

        } // run

        public boolean geneSources(Connection con) {

                boolean result = true;

		String[] sources = { "ensembl", "havana", "ensembl_havana" };

		// get all chromosomes, ignore LRG and MT
		String[] seqRegionNames = DBUtils.getColumnValues(con,
				"SELECT s.name FROM seq_region s, coord_system cs WHERE s.coord_system_id=cs.coord_system_id AND cs.name='chromosome' AND cs.attrib='default_version' AND s.name NOT LIKE 'LRG%' AND s.name != 'MT'");

		// filter out patches
		String[] patches = DBUtils.getColumnValues(con, "SELECT sr.name FROM seq_region sr, assembly_exception ae WHERE sr.seq_region_id=ae.seq_region_id AND ae.exc_type IN ('PATCH_NOVEL', 'PATCH_FIX')");

		List<String> nonPatchSeqRegions = ListUtils.removeAll(Arrays.asList(seqRegionNames), Arrays.asList(patches));

		// loop over each seq region, check that each logic name is represented
		try {

			PreparedStatement stmt = con
					.prepareStatement("SELECT COUNT(*) FROM gene g, seq_region sr WHERE g.seq_region_id=sr.seq_region_id AND sr.name=? AND g.source=?");

			for (String seqRegion : nonPatchSeqRegions) {

				for (String source : sources) {

					stmt.setString(1, seqRegion);
					stmt.setString(2, source);

					ResultSet rs = stmt.executeQuery();

					rs.first();
					int rows = rs.getInt(1);

					if (rows == 0) {

						result = false;
						ReportManager.problem(this, con, String.format("Chromosome %s has no genes with source %s", seqRegion, source));

					}

					rs.close();

				}

			}

			stmt.close();

		} catch (SQLException e) {

			System.err.println("Error executing SQL");
			e.printStackTrace();

		}

		return result;

	} // geneSources 


        public boolean transcriptSources(Connection con) {
        
               boolean result = true;

               String sql = "SELECT COUNT(*) FROM transcript WHERE isnull(source)";

               int rows = DBUtils.getRowCount(con, sql);

               if (rows > 0) {

                       result = false;
                       ReportManager.problem(this, con, "Some transcripts have no source");

               }

               sql = "SELECT COUNT(*) FROM transcript t, analysis a WHERE t.analysis_id = a.analysis_id AND source not in ('ensembl') AND logic_name = 'ensembl'";

               rows = DBUtils.getRowCount(con, sql);

               if (rows > 0) {

                       result = false;
                       ReportManager.problem(this, con, "Some transcripts of source ensembl do not have ensembl analysis");

               }

               sql = "SELECT COUNT(*) FROM transcript t, analysis a WHERE t.analysis_id = a.analysis_id AND source not in ('havana') AND logic_name = 'havana'";

               rows = DBUtils.getRowCount(con, sql);


               if (rows > 0) {

                       result = false;
                       ReportManager.problem(this, con, "Some transcripts of source havana do not have havana analysis");

               }

               sql = "SELECT COUNT(*) FROM transcript t, analysis a WHERE t.analysis_id = a.analysis_id AND source not in ('ensembl_havana') AND logic_name = 'ensembl_havana_transcript'";

               rows = DBUtils.getRowCount(con, sql);

               if (rows > 0) {

                       result = false;
                       ReportManager.problem(this, con, "Some transcripts of source ensembl_havana_transcript do not have ensembl_havana analysis");

               }

               return result;

        }

        public boolean geneTranscriptSources(Connection con) {

               boolean result = true;

               String sql = "SELECT COUNT(*) FROM gene g, transcript t where g.gene_id = t.gene_id and g.source = 'ensembl' and t.source not in ('ensembl')";

               int rows = DBUtils.getRowCount(con, sql);

               if (rows > 0) {

                       result = false;
                       ReportManager.problem(this, con, "Some ensembl genes have transcripts which are not ensembl");

               }

               sql = "SELECT COUNT(*) FROM gene g, transcript t where g.gene_id = t.gene_id and g.source = 'havana' and t.source not in ('havana')";

               rows = DBUtils.getRowCount(con, sql);

               if (rows > 0) {

                       result = false;
                       ReportManager.problem(this, con, "Some havana genes have transcripts which are not havana");

               }

               sql = "SELECT COUNT(*) FROM gene g, transcript t where g.gene_id = t.gene_id and g.source not in ('ensembl_havana') and t.source = 'ensembl_havana'";

               rows = DBUtils.getRowCount(con, sql);

               if (rows > 0) {

                       result = false;
                       ReportManager.problem(this, con, "Some ensembl_havana transcripts belong to genes which are ensembl_havana");

               }

               return result;

        }


  private boolean testMerged(Species s) {
    boolean result = false;
    int taxon = s.getTaxonID();
    int rows = DBUtils.getRowCount(getProductionDatabase().getConnection(), "SELECT count(*) FROM species s, attrib_type at WHERE at.attrib_type_id = s.attrib_type_id AND code = 'merged' AND taxon = " + taxon);
    if (rows > 0) {
      result = true;
    }
    return result;
  }


	// --------------------------------------------------------------------------

} // AnalysisTypes

