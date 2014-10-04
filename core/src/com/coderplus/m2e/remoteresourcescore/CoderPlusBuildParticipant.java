package com.coderplus.m2e.remoteresourcescore;
/*******************************************************************************
 * Copyright (c) 2014 Aneesh Joseph
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aneesh Joseph(coderplus.com)
 *******************************************************************************/
import java.io.File;
import java.util.Set;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.sonatype.plexus.build.incremental.BuildContext;

public class CoderPlusBuildParticipant extends MojoExecutionBuildParticipant {



	private static final String BUNDLE_GOAL = "bundle";
	private static final String RESOURCES_DIRECTORY = "resourcesDirectory";
	private static final String PROCESS_GOAL = "process";
	private static final String ATTACHED_TO_TEST = "attachedToTest";
	private static final String ATTACHED_TO_MAIN = "attachedToMain";
	private static final String ATTACHED = "attached";
	private static final String OUTPUT_DIRECTORY = "outputDirectory";

	public CoderPlusBuildParticipant(MojoExecution execution) {

		super(execution, true);
	}

	@Override
	public Set<IProject> build(final int kind, final IProgressMonitor monitor) throws Exception {


		final MojoExecution execution = getMojoExecution();

		if (execution == null) {
			return null;
		}
		IMaven maven = MavenPlugin.getMaven();	
		MavenProject project = getMavenProjectFacade().getMavenProject();
		final BuildContext buildContext = getBuildContext();
		final IFile pomFile = (IFile) getMavenProjectFacade().getProject().findMember("pom.xml");
		
		if(buildContext.isIncremental()){
			if(!buildContext.hasDelta(pomFile.getLocation().toFile()) && PROCESS_GOAL.equals(execution.getGoal())) {
				//ignore if there were no changes to the pom and was an incremental build
				return null;
			} else if(BUNDLE_GOAL.equals(execution.getGoal())){
				File resourcesDirectory = maven.getMojoParameterValue(project, execution, RESOURCES_DIRECTORY, File.class, new NullProgressMonitor());
				Scanner ds = buildContext.newScanner(resourcesDirectory); 
				if (ds != null) {
					ds.scan();
					String[] includedResourceFiles = ds.getIncludedFiles();
					if(includedResourceFiles == null || includedResourceFiles.length == 0 ){
						//ignore if there were no changes to the resources and was an incremental build
						return null;
					}
				}
			}
		}

		File outputDirectory = maven.getMojoParameterValue(project, execution, OUTPUT_DIRECTORY,File.class, new NullProgressMonitor());
		boolean attached = Boolean.TRUE.equals(maven.getMojoParameterValue(project, execution, ATTACHED,Boolean.class, new NullProgressMonitor()));
		boolean attachedToMain = Boolean.TRUE.equals(maven.getMojoParameterValue(project, execution, ATTACHED_TO_MAIN,Boolean.class, new NullProgressMonitor()));
		boolean attachedToTest = Boolean.TRUE.equals(maven.getMojoParameterValue(project, execution, ATTACHED_TO_TEST,Boolean.class, new NullProgressMonitor()));
		
		setTaskName(monitor);
		//execute the maven mojo
		final Set<IProject> result = executeMojo(kind, monitor);

		if(PROCESS_GOAL.equals(execution.getGoal()) && (attached || attachedToMain || attachedToTest)){
			Resource resource = new Resource();
			//FIXME: Do we have to do this here? Will the plugin automatically take care of this?
			resource.setDirectory(outputDirectory.getAbsolutePath());
			if(attachedToTest){
				project.addTestResource(resource );
			} else{
				project.addResource(resource);
			}
		}

		if(outputDirectory.exists()){
			buildContext.refresh(outputDirectory);
		}

		return result;
	}

	private void setTaskName(IProgressMonitor monitor) throws CoreException {

		if (monitor != null) {
			final String taskName = String.format("CoderPlus M2E: Invoking %s on %s", getMojoExecution().getMojoDescriptor()
					.getFullGoalName(), getMavenProjectFacade().getProject().getName());
			monitor.setTaskName(taskName);
		}
	}

	private Set<IProject> executeMojo(final int kind, final IProgressMonitor monitor) throws Exception {

		return super.build(kind, monitor);
	}


}