package org.eclipse.app4mc.capra.dash;

import java.io.IOException;
import java.util.Optional;
import org.eclipse.app4mc.capra.generic.adapters.TracePersistenceAdapter;
import org.eclipse.app4mc.capra.generic.artifacts.ArtifactWrapper;
import org.eclipse.app4mc.capra.generic.artifacts.ArtifactWrapperContainer;
import org.eclipse.app4mc.capra.generic.artifacts.ArtifactsFactory;
import org.eclipse.app4mc.capra.generic.helpers.ExtensionPointHelper;
import org.eclipse.app4mc.capra.simpletrace.tracemetamodel.SimpleTraceModel;
import org.eclipse.app4mc.capra.simpletrace.tracemetamodel.TraceElement;
import org.eclipse.app4mc.capra.simpletrace.tracemetamodel.TracemetamodelFactory;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;


public class ResourceListener implements IResourceChangeListener{

	final static int ARTIFACT_RENAMED = 0;
	final static int ARTIFACT_MOVED = 1;
	final static int ARTIFACT_DELETED = 2;

	private ResourceSet resourceSet;
	private TracePersistenceAdapter tracePersistenceAdapter;
	private Optional<ArtifactWrapperContainer> awc;
	private ArtifactWrapper art;
	private URI uri;
	private Resource resourceForArtifacts;
	private ArtifactWrapperContainer container;

	@Override
	public void resourceChanged(IResourceChangeEvent event) {

		IResourceDelta delta = event.getDelta();
		IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {

			@Override
			public boolean visit(IResourceDelta delta) throws CoreException {

				IPath toPath = delta.getMovedToPath();

				if(delta.getKind() == IResourceDelta.REMOVED && toPath!=null) {
					if(delta.getFullPath().toFile().getName().equalsIgnoreCase(toPath.toFile().getName()))
						markupJob(delta, ARTIFACT_MOVED);
					else markupJob(delta, ARTIFACT_RENAMED);
				}
				if(delta.getKind() == IResourceDelta.REMOVED && toPath==null){
					markupJob(delta, ARTIFACT_DELETED);
				}
				return true;
			}
		};
		try {
			delta.accept(visitor);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void markupJob(IResourceDelta delta, int issueType){

		
		WorkspaceJob job = new WorkspaceJob("myJob") {

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				resourceSet = new ResourceSetImpl();
				tracePersistenceAdapter = ExtensionPointHelper.getTracePersistenceAdapter().get();
				Optional<EObject> tracemodel = tracePersistenceAdapter.getTraceModel(resourceSet);
				SimpleTraceModel stm = (SimpleTraceModel)tracemodel.orElse(TracemetamodelFactory.eINSTANCE.createSimpleTraceModel());
				EList<TraceElement> tm = stm.getTraces();
				awc = tracePersistenceAdapter.getArtifactWrappers(resourceSet);
				if(! tracemodel.isPresent() || ! awc.isPresent()) return Status.OK_STATUS;
				art = ArtifactsFactory.eINSTANCE.createArtifactWrapper();
				uri = EcoreUtil.getURI(awc.get()); 
				resourceForArtifacts = resourceSet.createResource(uri);
				EList<ArtifactWrapper> list = awc.get().getArtifacts();
				container = awc.get();
				int counter = -1;
				System.out.println("Trace: " + tm.get(0));
				
				for (ArtifactWrapper aw : list) {
					counter ++;
					
					if(aw.getUri().toString().equals(delta.getResource().getFullPath().toString())){

						if(issueType == ARTIFACT_RENAMED || issueType == ARTIFACT_MOVED){
							art.setArtifactHandler(aw.getArtifactHandler());
							art.setName(delta.getMovedToPath().toFile().getName());
							art.setUri(delta.getMovedToPath().toString());
							break;
						}else 
							if(issueType == ARTIFACT_DELETED){
								
								System.out.println("index: " + counter);
								art.setArtifactHandler("NULL");
								art.setName("NULL");
								art.setUri("NULL");
								break;
							}										
					}					
				}
				if(art.getUri() != null ){
					container.getArtifacts().set(counter, art);
					resourceForArtifacts.getContents().add(container);
		
					try {
						resourceForArtifacts.save(null);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}	
}
