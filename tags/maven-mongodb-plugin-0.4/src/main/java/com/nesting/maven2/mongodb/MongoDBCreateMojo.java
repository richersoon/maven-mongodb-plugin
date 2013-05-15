package com.nesting.maven2.mongodb;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * Mojo for filling databases with data.
 * @goal create
 */
public class MongoDBCreateMojo 
    extends AbstractMongoDBMojo {

    /**
     * The directory that contains create scripts.
     * @parameter
     * @required
     */
    private File[] dbCreateScriptsDirectory;
    
    /**
     * {@inheritDoc}
     */
    public void executeInternal() 
        throws MojoExecutionException, 
        MojoFailureException {
        
        try {
            Mongo mongo = openConnection();
            DB db = getDatabase(mongo);
            for (int i=0; i<dbCreateScriptsDirectory.length; i++) {
                executeScriptsInDirectory(
                	dbCreateScriptsDirectory[i], db);
            }
            
        } catch(IOException ioe) {
            throw new MojoExecutionException(
                "Error executing create database scripts", ioe);
        }
    }
}
