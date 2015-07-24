# maven plugins #
  * [maven-db-plugin](http://code.google.com/p/maven-db-plugin/) (sql)
  * [maven-mongodb-plugin](http://code.google.com/p/maven-mongodb-plugin/) (MongoDB)

# repository configuration: #

```
    <repository>
        <id>maven-mongodb-plugin-repo</id>
        <name>maven mongodb plugin repository</name>
        <url>http://maven-mongodb-plugin.googlecode.com/svn/maven/repo</url>
        <layout>default</layout>
    </repository>
```

# goals #
  * mongodb:create - executes the create database scripts
  * mongodb:update - execute all of the update scripts

# common usage #
to get started:
```
mvn mongodb:create mongodb:update
```

to update database:
```
mvn mongodb:update
```

# sample development database configuration #

this sample assumes the following:
  * database is located at localhost and there are no authentication requirements
  * database name is someDatabase
  * create scripts are in src/main/mongo/create
  * update scripts are in src/main/mongo/update

```
<profiles>

	<!-- 
	 | Developer profile
	 +-->
	<profile>
		<id>dev</id>
		<activation><activeByDefault>true</activeByDefault></activation>
		<build>
			<plugins>
				<plugin>
					<groupId>com.googlecode</groupId>
					<artifactId>maven-mongodb-plugin</artifactId>
					<version>0.4</version>
					<configuration>
							<dbConnectionSettings>
								<hostname>localhost</hostname>
								<database>someDatabase</database>
							</dbConnectionSettings>
							<dbUpdateScriptsDirectory><param>src/main/mongo/update</param></dbUpdateScriptsDirectory>
							<dbCreateScriptsDirectory><param>src/main/mongo/create</param></dbCreateScriptsDirectory>
							<!-- optional <scriptEncoding>UTF-8</scriptEncoding> -->
					</configuration>
				</plugin>
			</plugins>
		</build>
	</profile>
</profiles>
```

The plugin also supports the `<serverId>` attribute in case you'd rather store the host/usr/pass in your maven `settings.xml` instead.  Script files can have any extension but should contain javascript code.  Any script file ending in the .gz extension is assumed to be a compressed script and will be decompressed in memory before executing it on the mongodb server.  Scripts are run using the db.eval (or db.doEval) operation.