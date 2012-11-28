
package com.nesting.maven2.mongodb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;

/**
 * Abstract mojo that all DB related mojos
 * inherit from.
 */
public abstract class AbstractMongoDBMojo
    extends AbstractMojo {    
    
    /**
     * The database connection settings for
     * the application.
     * @parameter
     * @required
     */
    private ConnectionSettings dbConnectionSettings;
    
    /**
     * The {@link Settings} object.
     * @parameter default-value="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;
    
    /**
     * Child mojos need to implement this.
     * @throws MojoExecutionException on error
     * @throws MojoFailureException on error
     */
    public abstract void executeInternal() 
        throws MojoExecutionException, 
        MojoFailureException;
    
    /**
     * {@inheritDoc}
     */
    public final void execute() 
        throws MojoExecutionException, 
        MojoFailureException {
        checkDbSettings(dbConnectionSettings, "dbConnectionSettings");
        executeInternal();
    }
    
    /**
     * Checks the given database connection settings.
     * @param dbSettings the settings to check
     * @param name the name of the settings
     * @throws MojoExecutionException on error
     * @throws MojoFailureException on error
     */
    private void checkDbSettings(
        ConnectionSettings dbSettings, String name) 
        throws MojoExecutionException, 
        MojoFailureException {
        
        // check server
        if (!StringUtils.isEmpty(dbSettings.getServerId())) {
            Server server = settings.getServer(dbSettings.getServerId());
            if (server==null) {
                throw new MojoFailureException(
                    "["+name+"] Server ID: "
                    +dbSettings.getServerId()+" not found!");
            } else if (StringUtils.isEmpty(server.getUsername())) {
                throw new MojoFailureException(
                    "["+name+"] Server ID: "+dbSettings.getServerId()+" found, "
                    +"but username is empty!");
            }
            
        // check non server settings
        } else if (StringUtils.isEmpty(dbSettings.getHostname())) {
            throw new MojoFailureException("["+name+"] No hostname defined!");
        }
        
    }
    
    /**
     * Executes all of the scripts in a given directory
     * using the given Mongo object.
     * @param directory the directory where the scripts reside
     * @param mongo the Mongo
     * @throws MojoFailureException on error
     * @throws MojoExecutionException on error
     * @throws IOException on error
     */
    protected void executeScriptsInDirectory(File directory, DB db) 
        throws MojoFailureException,
        MojoExecutionException,
        IOException {
        
        // talk a bit :)
        getLog().info("Executing scripts in: "+directory.getName());
        
        // make sure we can read it, and that it's 
        // a file and not a directory
        if (!directory.isDirectory()) {
            throw new MojoFailureException(
                directory.getName()+" is not a directory");
        }
        
        // get all files in directory
        File[] files = directory.listFiles();
        
        // sort
        Arrays.sort(files, new Comparator() {
            public int compare(Object arg0, Object arg1) {
                return ((File)arg0).getName().compareTo(((File)arg1).getName());
            } }
        );
        
        // loop through all the files and execute them
        for (int i = 0; i<files.length; i++) {
            if (!files[i].isDirectory() && files[i].isFile()) {
                double startTime = System.currentTimeMillis();
                executeScript(files[i], db);
                double endTime = System.currentTimeMillis();
                double elapsed = ((endTime-startTime)/1000.0);
                getLog().info(" script completed execution in "+elapsed+" second(s)");
            }
        }
        
    }
    
    /**
     * Executes the given script, using the given Mongo.
     * @param file the file to execute
     * @param mongo the db
     * @throws MojoFailureException on error
     * @throws MojoExecutionException on error
     * @throws IOException on error
     */
    protected void executeScript(File file, DB db) 
        throws MojoFailureException,
        MojoExecutionException,
        IOException {
        
        // talk a bit :)
        getLog().info("executing script: "+file.getName());
        
        // make sure we can read it, and that it's 
        // a file and not a directory
        if (!file.exists() || !file.canRead() 
            || file.isDirectory() || !file.isFile()) {
            throw new MojoFailureException(file.getName()+" is not a file");
        }
        
        // open input stream to file
        InputStream ips = new FileInputStream(file);
        
        // if it's a compressed file (gzip) then unzip as
        // we read it in
        if (file.getName().toUpperCase().endsWith("GZ")) {
            ips = new GZIPInputStream(ips);
            getLog().info(" file is gz compressed, using gzip stream");
        }
        
        // our file reader
        Reader reader;
        reader = new InputStreamReader(ips);
        
        StringBuffer data = new StringBuffer();
        String line;
        BufferedReader in = new BufferedReader(reader);

        // loop through the statements
        while ((line = in.readLine()) != null) {
            
            // append the line
            line.trim();
            data.append("\n").append(line);
        }
        reader.close();
        in.close();
        
        // execute last statement
        try {
        	CommandResult result = db.doEval("(function() {"+data.toString()+"})();", new Object[0]);
        	if (!result.ok()) {
            	getLog().warn(
            		"Error executing "+file.getName()+": "
            		+result.getErrorMessage(), result.getException());
        	} else {
        		getLog().info(" "+file.getName()+" executed successfully");
        	}
        } catch(Exception e) {
        	getLog().error(" error executing "+file.getName(), e);
        }
        
    }
    /**
     * Opens a connection using the given settings.
     * @param dbSettings the connection settings
     * @return the Connection
     * @throws MojoFailureException on error
     * @throws UnknownHostException 
     */
    protected Mongo openConnection() 
        throws MojoFailureException,
        UnknownHostException {

        // get server address
        ServerAddress serverAddr = (dbConnectionSettings.getPort()!=null)
        	? new ServerAddress(dbConnectionSettings.getHostname(), dbConnectionSettings.getPort().intValue())
        	: new ServerAddress(dbConnectionSettings.getHostname());

        // get Mongo
        Mongo mongo = (dbConnectionSettings.getOptions()!=null)
	    	? new Mongo(serverAddr, dbConnectionSettings.getOptions())
	    	: new Mongo(serverAddr);
        
        // we're good :)
        return mongo;
    }

    /**
     * Returns a DB from the given settings and mongo.
     * @param mongo the mongo
     * @param dbSettings the settings
     * @return the DB
     */
    protected DB getDatabase(Mongo mongo) {
    	
    	String username, password = null;
        
        // use settings to get authentication info for the given server
        if (!StringUtils.isEmpty(dbConnectionSettings.getServerId())) {
            Server server = settings.getServer(dbConnectionSettings.getServerId());
            username = server.getUsername();
            password = server.getPassword();
            
        // use settings in pom.xml
        } else {
            username = dbConnectionSettings.getUserName();
            password = dbConnectionSettings.getPassword();
        }

        // get the DB and optionaly authenticate
        DB db = mongo.getDB(dbConnectionSettings.getDatabase());
        if (username!=null && password!=null) {
        	db.authenticate(username, password.toCharArray());
        }
        return db;
    }

    /**
     * Drops the configured mongo database.
     * @param mongo the mogno
     */
    protected void dropDatabase(Mongo mongo) {
    	mongo.dropDatabase(dbConnectionSettings.getDatabase());
    }
}
