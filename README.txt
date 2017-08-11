README
======

License:
========

	Copyright 2013-2014 Kuali Foundation
	Copyright 2017 Michael O'Cleirigh
	Licensed under the
	Educational Community License, Version 2.0 (the "License"); you may
	not use this file except in compliance with the License. You may
	obtain a copy of the License at

	http://www.osedu.org/licenses/ECL-2.0

	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an "AS IS"
	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
	or implied. See the License for the specific language governing
	permissions and limitations under the License.

The Java code uses FileInputStream's and FileOutputStream's and some code copied from SVNKit that allows us to stream.readLine() in a way similiar to how a BufferedReader works.

See org.kuali.student.common.io.IOUtils for the usage of the methods.  It is a copy of org.tmatesoft.svn.core.internal.wc.SVNUtil version 1.7.8 which is licenced by the SVN Kit Licence.

http://svnkit.com/license.html

Subversion and Git Respository Tools
====================================

Background:
-----------

As part of https://jira.kuali.org/browse/KSENROLL-3907 the SvnDumpFilter was created.  It was used to rewrite several thousand subversion revisions to inject copyfrom data where it previously didn't exist.

This allowed history to be restored after a patch based remodularization.

This SvnDumpFilter was generalized to call methods on the IParseOptions interface during dump file processing.

This allowed specific searches to be written.

To support the Git migration a GitImporterParseOptions class was created which is notified for each revision, file and directory change and inserts the data into a git repository.

The original history repair occured with Subversion version 3 dump streams since they are smaller and no file contents were being changed only copyfrom node attributes.

For the Git conversion the contents of the blob's needed to be extracted and it was much simpler to use Subversion version 2 dump streams since they contain the full file content on adds and modifies.

Running the Git Importer
========================

The maven-shade-plugin is used to create an uber jar that contains all of the necessary dependencies to run.

See git-importer/scripts/run-importer.sh for an example of how to launch the importer.

Branch detection can be customized per repository.

See git-importer-student-plugin/scripts/run-importer.sh for how this works.  
The plugin itself uses a spring profile and a bean override to insert its custom branch detection logic (plugin detector first, falling back to the standard detector).

How copyfrom data is tracked
============================

When the importer runs there is a jsvn directory created in the git meta data directory.  It is used to store a list of branches (and pointers to the git object id of the commits) after each subversion revision has been imported.

Another file holds the svn:mergeinfo equivelant data for each branch after each revision.  This is used to include additional merge parents in the generated git commit.

In Git each commit stores the full tree of files and history following is a computation that is performed.  Even though a single cherry pick might not normally be seen as a merge without including the merge parent git will never look down the originating path for changes.  So the importer will over merge in some cases to not remove this ability to follow changes later.


Preparing Svn Dump Files for the Importer
=========================================

The Git importer reads Subersion version 2 dump files and writes their content into a git repository.

In order to create a version 2 dump you need to have shell access to the directory containing the subversion repository.

An easy way to do this is to mirror the real svn repository and then dump out the files from the local mirror

Create svn mirror:
------------------
1. svnadmin create mirror

2. create a file called mirror/hooks/pre-revprop-change

2.1 add the following into the file

#!/bin/bash
exit 0

2.2 make the file executable (chmod +x mirror/hooks/pre-revprop-change)

3. initialize the sync

svnsync init file:///path/to/mirror https://url/of/remote/repo

svnsync sync file:///path/to/mirror

(wait)

It can take a long time to since a big repository so leave this running over night and come back to it later.

The sync can be added into the crontab later to automatically keep your mirror upto date.


Create an incremental dump file:
--------------------------------

Modify the script in scripts/run-svn-export.sh for the location of your local mirror and export the revisions.

Perhaps export in 10,000 rev chunks so that if there is a problem you will be able to dump only the difference to the next block instead of to the end of the repository.

Validating a Git Conversion
===========================

Checkout the svn path for a commit (like release tags or the tip of the trunk)

Import that tree into git and then look to make sure there are no differences.
look at the exported url in the git commit

1. svn export $URL branchName-at-rXYZ

2. cd branchName-at-rXYZ

3. find . -name .gitignore | xargs rm

4. git init

5. git add .

6. git commit -m "initial import of branchName-at-rXYZ"

7. cd /path/to/converted/git/repo

8. git remote add svn ../branchName-at-rXYZ

9. git fetch svn

10. git diff-tree -r -w -b svn/master HEAD | less

11. If there are differences research what we are missing.  If a line has all
    zeros it means the file does not exist on that branch.

 
