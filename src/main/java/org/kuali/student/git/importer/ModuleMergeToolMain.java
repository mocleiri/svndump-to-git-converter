/*
 * Copyright 2014 The Kuali Foundation
 * 
 * Licensed under the Educational Community License, Version 1.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.kuali.student.git.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.kuali.student.git.model.ExternalModuleInfo;
import org.kuali.student.git.model.JGitTreeData;
import org.kuali.student.git.model.SvnRevisionMapper;
import org.kuali.student.git.model.SvnRevisionMapper.SvnRevisionMapResults;
import org.kuali.student.git.model.tree.JGitTreeUtils;
import org.kuali.student.git.tools.GitRepositoryUtils;
import org.kuali.student.git.tools.SvnExternalsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Git supports a subtree merge strategy but we will take that further to a module merge strategy.
 * 
 * This command line tool can be used to rewrite a git commit or eventually a git branch that was constructed using svn:externals.
 * 
 * For each named external we will create an actual subdirectory and copy in the subtree from the seperate branch head.
 * 
 * 
 * 
 * @author Kuali Student Team
 * 
 */
public class ModuleMergeToolMain {

	private static final Logger log = LoggerFactory.getLogger(ModuleMergeToolMain.class);
	
	/**
	 * 
	 */
	public ModuleMergeToolMain() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length != 6 && args.length != 7) {
			System.err.println("USAGE: <git repository> <mode> <bare> <object id> <svn:externals containing file> <svn revision> [<ref prefix>]");
			System.err.println("\t<mode> : commit or branch");
			System.err.println("\t<bare> : 0 (false) or 1 (true)");
			System.err.println("\t<object id> : the sha1 of the commit or the name of the branch in branch mode");
			System.err.println("\t<svn:externals file> : contains the content of the svn:externals property for the target");
			
			System.err.println("\t<ref prefix> : refs/heads (default) or say refs/remotes/origin (test clone)");
			System.exit(-1);
		}

		boolean bare = false;
		
		if (args[2].trim().equals("1")) {
			bare = true;
		}
		
		boolean branchMode = false;
		boolean commitMode = false;
				
		if (args[1].equals("branch"))
			branchMode = true;
		else if (args[1].equals("commit"))
			commitMode = true;
		
		String reference = args[3].trim();
		
		String svnExternalsDataFile = args[4].trim();
		
		Long svnRevision = Long.parseLong(args[5].trim());
		
		String refPrefix = Constants.R_HEADS;
		
		if (args.length == 7)
			refPrefix = args[6].trim();
		
		try {
			
			Repository repo = GitRepositoryUtils.buildFileRepository(new File (args[0]).getAbsoluteFile(), false, bare);
			
			SvnRevisionMapper revisionMapper = new SvnRevisionMapper(repo);
			
			if (commitMode) {
				
				/*
				 * 
				 */
				
				List<ExternalModuleInfo> externals = SvnExternalsUtils.extractExternalModuleInfoFromInputStream(svnRevision, "https://svn.kuali.org/repos/student", new FileInputStream(svnExternalsDataFile));
			
				/*
				 * Take the existing content of the commit pointed at and then materialize the externals within it.
				 */
				
				RevWalk rw = new RevWalk(repo);
				
				ObjectInserter inserter = repo.newObjectInserter();
				
				RevCommit commit = rw.parseCommit(ObjectId.fromString(reference));
				
				CommitBuilder commitBuilder = new CommitBuilder();
				
				ObjectReader or;
				commitBuilder.setTreeId(SvnExternalsUtils.createFusedTree (or = repo.newObjectReader(), inserter, rw, commit, externals, revisionMapper));

				List<ObjectId>parentIds = new LinkedList<>();
				
				for (int i = 0; i < commit.getParentCount(); i++) {
					
					RevCommit parent = commit.getParent(i);
				
					parentIds.add(parent.getId());
					
				}
				
				commitBuilder.setParentIds(parentIds);

				commitBuilder.setAuthor(commit.getAuthorIdent());

				commitBuilder.setCommitter(commit.getCommitterIdent());
				
				commitBuilder.setMessage(commit.getFullMessage());
				

				ObjectId commitId = inserter
						.insert(commitBuilder);
				
				log.info("new commit id = " + commitId);
				
				rw.release();
				
				inserter.release();
				
				or.release();
			}
			
				
			
			
			
		} catch (Exception e) {
			
			log.error("unexpected Exception ", e);
		}
	}

	

}
