/*
 Copyright (C) 2003 EBI, GRL
 
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
package org.ensembl.healthcheck;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.ensembl.healthcheck.util.DBUtils;

/**
 * Class that stores information about which databases are available.
 */
public class DatabaseRegistry {

    // Entries is explicitly specified as an ArrayList rather than the list because the order is
    // important
    ArrayList entries = new ArrayList();

    protected static Logger logger = Logger.getLogger("HealthCheckLogger");

    // -----------------------------------------------------------------
    /**
     * Create a new DatabaseRegistry. DatabaseRegistryEntry objects for the databases matching
     * regexp are created and added to the registry.
     * 
     * @param regexp The regular expression matching the databases to use.
     */
    public DatabaseRegistry(String regexp) {

        Connection con = DBUtils.openConnection(System.getProperty("driver"), System.getProperty("databaseURL"), System
                .getProperty("user"), System.getProperty("password"));

        String[] names = DBUtils.listDatabases(con, regexp);

        addEntriesToRegistry(names);

    }

    //	-----------------------------------------------------------------
    /**
     * Create a new DatabaseRegistry. DatabaseRegistryEntry objects for the databases matching
     * regexp are created and added to the registry.
     * 
     * @param regexps The regular expressions matching the databases to use.
     */
    public DatabaseRegistry(List regexps) {

        Connection con = DBUtils.openConnection(System.getProperty("driver"), System.getProperty("databaseURL"), System
                .getProperty("user"), System.getProperty("password"));

        Iterator it = regexps.iterator();
        while (it.hasNext()) {

            String[] names = DBUtils.listDatabases(con, (String) it.next());
            addEntriesToRegistry(names);

        }

    }

    // -------------------------------------------------------------------------
    /**
     * Create a new DatabaseRegistry from a set of DatabaseRegistryEntries.
     */
    public DatabaseRegistry(DatabaseRegistryEntry[] dbres) {

        for (int i = 0; i < dbres.length; i++) {

            entries.add(dbres[i]);

        }

    }

    // -----------------------------------------------------------------

    private void addEntriesToRegistry(String[] names) {

        for (int i = 0; i < names.length; i++) {
            DatabaseRegistryEntry dbre = new DatabaseRegistryEntry(names[i]);
            entries.add(dbre);
            logger.finest(dbre.getName() + " appears to be type " + dbre.getType() + " and species " + dbre.getSpecies());
            logger.finest("Added DatabaseRegistryEntry for " + names[i] + " to DatabaseRegistry");
        }

    }

    // -----------------------------------------------------------------
    /**
     * Add a new DatabaseRegistryEntry to this registry.
     * 
     * @param dbre The new DatabaseRegistryEntry.
     */
    public void add(DatabaseRegistryEntry dbre) {

        entries.add(dbre);

    }

    // -----------------------------------------------------------------
    /**
     * Get all of the DatabaseRegistryEntries stored in this DatabaseRegistry.
     * 
     * @return The DatabaseRegistryEntries stored in this DatabaseRegistry.
     */
    public DatabaseRegistryEntry[] getAll() {

        return (DatabaseRegistryEntry[]) entries.toArray(new DatabaseRegistryEntry[entries.size()]);

    }

    //	-----------------------------------------------------------------
    /**
     * Get all of the DatabaseRegistryEntries for a particular species
     * 
     * @param species The species to look for.
     * @return The DatabaseRegistryEntries for species..
     */
    public DatabaseRegistryEntry[] getAll(Species species) {

        List result = new ArrayList();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            DatabaseRegistryEntry dbre = (DatabaseRegistryEntry) it.next();
            if (dbre.getSpecies().equals(species)) {
                result.add(dbre);
            }
        }

        return (DatabaseRegistryEntry[]) result.toArray(new DatabaseRegistryEntry[result.size()]);

    }

    //	-----------------------------------------------------------------
    /**
     * Get all of the DatabaseRegistryEntries for a particular database type.
     * 
     * @param type The type to look for.
     * @return The DatabaseRegistryEntries for type.
     */
    public DatabaseRegistryEntry[] getAll(DatabaseType type) {

        List result = new ArrayList();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            DatabaseRegistryEntry dbre = (DatabaseRegistryEntry) it.next();
            if (dbre.getType().equals(type)) {
                result.add(dbre);
            }
        }

        return (DatabaseRegistryEntry[]) result.toArray(new DatabaseRegistryEntry[result.size()]);

    }

    //	-----------------------------------------------------------------
    /**
     * Get all of the DatabaseRegistryEntries for a particular database type and species.
     * 
     * @param type The type to look for.
     * @param species The Species to look for.
     * @return The DatabaseRegistryEntries that match type and species..
     */
    public DatabaseRegistryEntry[] getAll(DatabaseType type, Species species) {

        List result = new ArrayList();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            DatabaseRegistryEntry dbre = (DatabaseRegistryEntry) it.next();
            if (dbre.getType().equals(type) && dbre.getSpecies().equals(species)) {
                result.add(dbre);
            }
        }

        return (DatabaseRegistryEntry[]) result.toArray(new DatabaseRegistryEntry[result.size()]);

    }

    // -----------------------------------------------------------------
    /**
     * Sets the type of every DatabaseRegistryEntry.
     * 
     * @param type The type to set.
     */
    public void setTypeOfAll(DatabaseType type) {

        List result = new ArrayList();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            DatabaseRegistryEntry dbre = (DatabaseRegistryEntry) it.next();
            dbre.setType(type);
        }

    }

    // -----------------------------------------------------------------
    /**
     * Sets the species of every DatabaseRegistryEntry.
     * 
     * @param species The species to set.
     */
    public void setSpeciesOfAll(Species species) {

        List result = new ArrayList();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            DatabaseRegistryEntry dbre = (DatabaseRegistryEntry) it.next();
            dbre.setSpecies(species);
        }

    }

    //---------------------------------------------------------------------
    /**
     * Get a single, named DatabaseRegistryEntry.
     * 
     * @param name The name to look for.
     * @return The matching DatabaseRegistryEntry, or null if none is found.
     */
    public DatabaseRegistryEntry getByExactName(String name) {

        Iterator it = entries.iterator();
        while (it.hasNext()) {
            DatabaseRegistryEntry dbre = (DatabaseRegistryEntry) it.next();
            if (dbre.getName().equals(name)) { return dbre; }
        }

        return null;
    }

    // -------------------------------------------------------------------------
    /**
     * Get a list of the types of databases in the registry.
     * 
     * @return An array containing each DatabaseType found in the registry.
     */
    public DatabaseType[] getTypes() {

        List types = new ArrayList();

        Iterator it = entries.iterator();
        while (it.hasNext()) {
            DatabaseRegistryEntry dbre = (DatabaseRegistryEntry) it.next();
            if (!types.contains(dbre.getType())) {
                types.add(dbre.getType());
            }
        }

        return (DatabaseType[]) types.toArray(new DatabaseType[types.size()]);

    }

    //	 -------------------------------------------------------------------------
    /**
     * Get a list of the species in the registry.
     * 
     * @return An array containing each Species found in the registry.
     */
    public Species[] getSpecies() {

        List species = new ArrayList();

        Iterator it = entries.iterator();
        while (it.hasNext()) {
            DatabaseRegistryEntry dbre = (DatabaseRegistryEntry) it.next();
            if (!species.contains(dbre.getSpecies())) {
                species.add(dbre.getSpecies());
            }
        }

        return (Species[]) species.toArray(new Species[species.size()]);

    }

    // -----------------------------------------------------------------
    /**
     * @return The number of DatabaseRegistryEntries in this registry.
     */
    public int getEntryCount() {

        return entries.size();

    }

    // -------------------------------------------------------------------------

} // DatabaseRegistry
