package org.eclipse.app4mc.capra.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.app4mc.capra.generic.adapters.Connection;
import org.eclipse.app4mc.capra.generic.adapters.TraceMetamodelAdapter;
import org.eclipse.app4mc.capra.generic.adapters.TracePersistenceAdapter;
import org.eclipse.app4mc.capra.simpletrace.tracemetamodel.EObjectToArtifact;
import org.eclipse.app4mc.capra.simpletrace.tracemetamodel.EObjectToEObject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;

public class TraceNodeContentProvider implements IStructuredContentProvider, IGraphEntityContentProvider {

	private TraceMetamodelAdapter metaModelAdapter;
	private TracePersistenceAdapter tracePersistenceAdapter;
	private ResourceSet resourceSet = new ResourceSetImpl();

	public TraceNodeContentProvider(TraceMetamodelAdapter metaModelAdapter,
			TracePersistenceAdapter tracePersistenceAdapter) {
		this.metaModelAdapter = metaModelAdapter;
		this.tracePersistenceAdapter = tracePersistenceAdapter;
	}

	@Override
	public Object[] getConnectedTo(Object entity) {

		if (entity instanceof EObject) {
			EObject selection = (EObject) entity;
			resourceSet = selection.eResource().getResourceSet();
			Optional<EObject> traceModel = tracePersistenceAdapter.getTraceModel(resourceSet);
			return getConnectedTo(selection, traceModel);
		}
		return null;
	}

	public Object[] getConnectedTo(EObject selection, Optional<EObject> traceModel) {
		List<EObject> list = new ArrayList<EObject>();
		List<Connection> connectedElements = metaModelAdapter.getConnectedElements(selection, traceModel);
		for (Connection c : connectedElements) {
			if (c.getTlink() instanceof EObjectToEObject) {
				EObjectToEObject trace = (EObjectToEObject) c.getTlink();
				// Only interested in elements this element connects to,
				// not elements that connect to this element
				if (trace.getSource().equals(selection)) {
					list.addAll(c.getTargets());
				} else if (trace.equals(selection)) {
					list.addAll(c.getTargets());
				}
			}

			if (c.getTlink() instanceof EObjectToArtifact) {
				EObjectToArtifact trace = (EObjectToArtifact) c.getTlink();
				// Only interested in elements this element connects to,
				// not elements that connect to this element
				if (trace.getSource().equals(selection)) {
					list.addAll(c.getTargets());
				} else if (trace.equals(selection)) {
					list.addAll(c.getTargets());
				}
			}
		}

		return list.toArray();
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object entity) {
		if (entity instanceof EObject) {
			EObject selection = (EObject) entity;
			Optional<EObject> traceModel = tracePersistenceAdapter.getTraceModel(resourceSet);
			return getElements(selection, traceModel);
		}
		return null;
	}

	public Object[] getElements(EObject selection, Optional<EObject> traceModel) {
		List<Object> accumulator = new ArrayList<>();
		List<Connection> traces = getTransitivelyConnectedElements(selection, traceModel, accumulator);
		List<EObject> list = new ArrayList<EObject>();
		for (Connection c : traces) {
			list.addAll(c.getTargets());
		}
		list.add(selection);
		return list.toArray();
	}

	private List<Connection> getTransitivelyConnectedElements(EObject element, Optional<EObject> traceModel,
			List<Object> accumulator) {
		List<Connection> directElements = metaModelAdapter.getConnectedElements(element, traceModel);
		List<Connection> allElements = new ArrayList<>();

		directElements.forEach(connection -> {
			if (!accumulator.contains(connection.getTlink())) {
				allElements.add(connection);
				accumulator.add(connection.getTlink());
				connection.getTargets().forEach(e -> {
					allElements.addAll(getTransitivelyConnectedElements(e, traceModel, accumulator));
				});
			}
		});

		return allElements;
	}

}