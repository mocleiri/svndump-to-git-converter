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
package io.github.svndump_to_git.model;

import org.eclipse.jgit.lib.PersonIdent;

/**
 * @author Kuali Student Team
 *
 */
public class GitCommitData {

	private String commitMessage;

	private PersonIdent personIdent;


	public GitCommitData(PersonIdent personIdent, String commitMessage) {
		this.personIdent = personIdent;
		this.commitMessage = commitMessage;
	}

	


	/**
	 * @return the personIdent
	 */
	public PersonIdent getPersonIdent() {
		return personIdent;
	}

	/**
	 * @return the commitMessage
	 */
	public String getCommitMessage() {
		return commitMessage;
	}
	
	
	

}
