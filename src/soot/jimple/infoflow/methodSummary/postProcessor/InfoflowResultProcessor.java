package soot.jimple.infoflow.methodSummary.postProcessor;

import heros.InterproceduralCFG;
import static soot.jimple.infoflow.methodSummary.sourceSinkFactory.SourceSinkFactory.createFieldSink;
import static soot.jimple.infoflow.methodSummary.sourceSinkFactory.SourceSinkFactory.createParamterSink;
import static soot.jimple.infoflow.methodSummary.sourceSinkFactory.SourceSinkFactory.createReturnSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.ArrayType;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.infoflow.InfoflowResults.SourceInfo;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.data.pathBuilders.IAbstractionPathBuilder;
import soot.jimple.infoflow.methodSummary.data.AbstractMethodFlow;
import soot.jimple.infoflow.methodSummary.data.DefaultMethodFlow;
import soot.jimple.infoflow.methodSummary.data.IFlowSink;
import soot.jimple.infoflow.methodSummary.data.MethodSummaries;
import soot.jimple.infoflow.methodSummary.sourceSink.IFlowSource;
import soot.jimple.infoflow.methodSummary.sourceSinkManager.SummarySourceSinkManager;

public class InfoflowResultProcessor {
	private final Logger logger = LoggerFactory.getLogger(InfoflowResultProcessor.class);

	private InterproceduralCFG<Unit, SootMethod> cfg;
	private Set<Abstraction> result;
	private boolean DEBUG = true;
	private String method;
	private int summaryAPLength;

	public InfoflowResultProcessor(Set<Abstraction> result2, InterproceduralCFG<Unit, SootMethod> cfg, String m,
			SummarySourceSinkManager manager, int sAPL) {
		this.result = result2;
		this.cfg = cfg;
		this.method = m;
		this.summaryAPLength = sAPL;
	}

	public MethodSummaries process() {
		MethodSummaries flows = new MethodSummaries();
		System.out.println();
		logger.info("start processing infoflow abstractions");
		final SootMethod m = Scene.v().getMethod(method);
		final PointsToAnalysis pTa = Scene.v().getPointsToAnalysis();

		IAbstractionPathBuilder pathBuilder = new DefaultPathBuilderFactory().createPathBuilder(Runtime.getRuntime()
				.availableProcessors());

		for (Abstraction a : result) {
			logger.debug("abstraction: " + a.toString());
			pathBuilder.getResults().clear();
			pathBuilder.computeTaintSources(Collections.singleton(new AbstractionAtSink(a, NullConstant.v(), Jimple.v()
					.newNopStmt())));
			for (Set<SourceInfo> sourceInfos : pathBuilder.getResults().getResults().values()) {
				//since we use 'points to' to identify sources with an apl > 0 it can happen
				//that we get more then one possible source
				for (SourceInfo si : sourceInfos) {
					if (si.getContext() == null || si.getSource() == null)
						continue;

					// Get the source
					List<IFlowSource> sources = new LinkedList<IFlowSource>();
					if (si.getUserData() instanceof IFlowSource) {
						IFlowSource source = (IFlowSource) si.getUserData();
						sources.add(source);
					} else{
						@SuppressWarnings("unchecked")
						List<IFlowSource> userData = (List<IFlowSource>) si.getUserData();
						sources = userData;
					}

					if (sources.size() == 0)
						throw new RuntimeException("Link to source missing");
					for (IFlowSource source : sources) {
						// Get the sink
						IFlowSink sink = null;

						PointsToSet basePT = pTa.reachingObjects(a.getAccessPath().getPlainValue());
						// The sink may be a parameter
						for (int i = 0; i < m.getParameterCount(); i++) {
							Local p = m.getActiveBody().getParameterLocal(i);
							// boolean isPrimitiveType = m.getParameterType(i) instanceof PrimType ;
							boolean isArrayType = m.getParameterType(i) instanceof ArrayType;
							PointsToSet pPT = pTa.reachingObjects(p);
							if (pPT.hasNonEmptyIntersection(basePT)) {
								if (a.getAccessPath().isLocal()) {
									if (isArrayType)
										sink = createParamterSink(m, i, java.util.Collections.<SootField> emptyList(),
												a.getAccessPath().getTaintSubFields());
								} else if (a.getAccessPath().getFieldCount() < summaryAPLength)
									sink = createParamterSink(m, i, a.getAccessPath().getFields(), a.getAccessPath()
											.getTaintSubFields());
								else {

									sink = createParamterSink(m, i, cutAPLength(a.getAccessPath().getFields()), true);
								}

							}

							if (source != null && sink != null) {
								addFlow(source, sink, flows);
								sink = null;
							}

						}

						// check field sink
						if (a.getAccessPath().isInstanceFieldRef() && !m.isStatic()
								&& a.getAccessPath().getPlainValue() == m.getActiveBody().getThisLocal()) {
							if (a.getAccessPath().getFieldCount() < summaryAPLength) {
								// we can save the complete ap in the summary file
								sink = createFieldSink(a.getAccessPath().getFields(), a.getAccessPath()
										.getTaintSubFields());

							} else {
								// we have to cut the ap sience the ap is longer then the set limit
								sink = createFieldSink(cutAPLength(a.getAccessPath().getFields()), true);
							}

							if (source != null && sink != null) {
								addFlow(source, sink, flows);
								sink = null;
							}
						}

						// check return sink
						for (Unit u : m.getActiveBody().getUnits())
							if (cfg.isExitStmt(u))
								for (ValueBox vb : u.getUseBoxes())
									if (vb.getValue() == a.getAccessPath().getPlainValue())
										if (a.getAccessPath().isLocal())
											sink = createReturnSink(a.getAccessPath().getTaintSubFields());
										else if (a.getAccessPath().getFieldCount() < summaryAPLength) {
											sink = createReturnSink(a.getAccessPath().getFields(), a.getAccessPath()
													.getTaintSubFields());
										} else {
											sink = createReturnSink(cutAPLength(a.getAccessPath().getFields()), true);
										}

						if (source != null && sink != null) {
							addFlow(source, sink, flows);
							sink = null;
						}

					}
				}
			}
		}

		logger.info("Result processing finished");
		return flows;
	}

	private SootField[] cutAPLength(SootField[] fields) {
		SootField f[] = new SootField[summaryAPLength];
		for (int i = 0; i < summaryAPLength; i++) {
			f[i] = fields[i];
		}
		return f;
	}

	private boolean isIdentityFlow(IFlowSource source, IFlowSink sink) {
		if (sink.isReturn())
			return false;
		if (sink.isField()) {
			if (source.isParamter())
				return false;
		}
		if (sink.isParamter()) {
			if (source.isField() || source.isThis())
				return false;
		}
		if (sink.isThis()) {
			if (source.isParamter())
				return false;
		}
		if (sink.getParamterIndex() != source.getParamterIndex())
			return false;
		if (sink.getFieldCount() != source.getFieldCount())
			return false;
		for (int i = 0; i < sink.getFieldCount(); i++) {
			if (!source.getFields().get(i).equals(sink.getFields().get(i)))
				return false;
		}
		return true;

		// if (source.isParamter() != sink.isParamter())
		// return false;
		// if (source.isField() != sink.isField())
		// return false;
		// if (source.isThis() != sink.isThis())
		// return false;
		//
		// if (source.getParamterIndex() != sink.getParamterIndex())
		// return false;
		// if (!safeEquals(source.getParaType(), sink.getParaType()))
		// return false;
		//
		// if (!safeEquals(source.getField(), sink.getField()))
		// return false;
		//
		// if (source.hasAccessPath() != sink.hasAccessPath())
		// return false;
		// if(source.getAccessPath() == null && sink.getAccessPath() != null ||
		// source.getAccessPath() != null && sink.getAccessPath() == null)
		// return false;
		//
		// if (source.getAccessPath() != null && sink.getAccessPath() != null &&
		// !safeEquals(source.getAccessPath().toString(), sink.getAccessPath().toString()))
		// return false;
		//
		// return true;
	}

	private void addFlow(IFlowSource source, IFlowSink sink, MethodSummaries summaries) {
		// Ignore identity flows
		if (isIdentityFlow(source, sink))
			return;

		AbstractMethodFlow mFlow = new DefaultMethodFlow(method, source, sink);
		if (summaries.addFlowForMethod(method, mFlow))
			debugMSG(source, sink);
	}

	private boolean safeEquals(String accessPath, String accessPath2) {
		if (accessPath == accessPath2)
			return true;
		if (accessPath == null || accessPath2 == null)
			return false;
		return accessPath.equals(accessPath2);
	}

	private void debugMSG(IFlowSource source, IFlowSink sink) {
		if (DEBUG) {
			System.out.println("\nmethod: " + method);
			System.out.println("source: " + source.toString());
			System.out.println("sink  : " + sink.toString());

			System.out.println("------------------------------------");
		}
	}

}