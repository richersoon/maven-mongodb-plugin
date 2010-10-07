package com.nesting.maven2.mongodb;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * Mojo for filling databases with data.
 * @goal update
 */
public class MongoDBUpdateMojo 
    extends AbstractMongoDBMojo {

    /**
     * The directory that contains update scripts.
     * @parameter
     * @required
     */
    private File[] dbUpdateScriptsDirectory;
    
    /**
     * {@inheritDoc}
     */
    public void executeInternal() 
        throws MojoExecutionException, 
        MojoFailureException {
        
        try {
            Mongo mongo = openConnection();
            DB db = getDatabase(mongo);
            for (int i=0; i<dbUpdateScriptsDirectory.length; i++) {
                executeScriptsInDirectory(
                	dbUpdateScriptsDirectory[i], db);
            }
            
        } catch(IOException ioe) {
            throw new MojoExecutionException(
                "Error executing update scripts", ioe);
        }
    }
}
