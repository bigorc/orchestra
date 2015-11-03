package org.oc.orchestra.parser;

import java.util.ArrayList;
import java.util.List;

import org.orchestra.constraint.AsyncRemoteResourceObjectConstraint;
import org.orchestra.constraint.AsyncRemoteStateMachineConstraint;
import org.orchestra.constraint.BeforeConstraint;
import org.orchestra.constraint.Constraint;
import org.orchestra.constraint.DistributedSMConstraint;
import org.orchestra.constraint.Proable;
import org.orchestra.constraint.RemoteResourceObjectConstraint;
import org.orchestra.constraint.RemoteStateMachineConstraint;
import org.orchestra.constraint.ResourceObjectConstraint;
import org.orchestra.constraint.SMable;
import org.orchestra.constraint.StateMachineConstraint;
import org.orchestra.coordinate.Curator;

public class ConstraintsVisitor extends RulesBaseVisitor<Constraint>{
	private String connectString;
	private List<Constraint> cons = new ArrayList<Constraint>();
	
	public List<Constraint> getConstraints() {
		return cons;
	}
	
	@Override 
	public Constraint visitLine(RulesParser.LineContext ctx) { 
		Constraint result = visitChildren(ctx);
		if(result != null) cons.add(result);
		return result; 
	}
	
	@Override 
	public Constraint visitBefore_cons(RulesParser.Before_consContext ctx) {
		Constraint before = visitChildren(ctx.cons(0));
		Constraint after = visitChildren(ctx.cons(1));
		BeforeConstraint bc = new BeforeConstraint(before, after);
		return bc;
	}
	
	@Override 
	public Constraint visitRjo_cons(RulesParser.Rjo_consContext ctx) {
		String filename = ctx.STRING().getText();
		System.out.println(filename);
		return new ResourceObjectConstraint(filename.substring(1, filename.length() - 1)); 
	}
	
	@Override 
	public Constraint visitRjo_host_cons(RulesParser.Rjo_host_consContext ctx) {
		Constraint cons = visitChildren(ctx);
		String filename = ctx.STRING().getText();
		((Proable)cons).setFilename(filename.substring(1, filename.length() - 1));
		return cons;
	}
	
	@Override
	public Constraint visitSm_cons(RulesParser.Sm_consContext ctx) {
		List<String> args = new ArrayList<String>();
		int wdCount = ctx.WORD().size();
		int iBe = ctx.children.indexOf(ctx.BE());
		StringBuffer state = new StringBuffer();
		for(int i = 1; i < wdCount;i++) {
			if(ctx.children.indexOf(ctx.WORD(i)) < iBe) {
				args.add(ctx.WORD(i).getText());
				System.out.println("args:" + ctx.WORD(i).getText());
			} else {
				state.append(ctx.WORD(i));
				if(i != wdCount - 1) state.append(' ');
				System.out.println("state:" + ctx.WORD(i).getText());
			}
		}
		if(ctx.LIKE() != null) {
			args.add("like");
			System.out.println("like regex:" + ctx.LIKE());
		}
		System.out.println("sm:" + ctx.WORD(0).getText());
		
		Constraint cons = new StateMachineConstraint(ctx.WORD(0).getText(), 
				args, state.toString());
		return cons; 
	}
	
	@Override 
	public Constraint visitOn_host(RulesParser.On_hostContext ctx) {
		DistributedSMConstraint cons = null;
		if(ctx.parent instanceof RulesParser.Sm_host_consContext) {
			if(ctx.parent.parent.parent instanceof RulesParser.Before_consContext) {
				cons = new RemoteStateMachineConstraint();
			} else {
				cons = new AsyncRemoteStateMachineConstraint();
			}
		} else {
			if(ctx.parent.parent.parent instanceof RulesParser.Before_consContext) {
				cons = new RemoteResourceObjectConstraint();
			} else {
				cons = new AsyncRemoteResourceObjectConstraint();
			}
		}
		cons.setCoordinator(new Curator());
		cons.setClient(ctx.WORD().getText());
		return cons;
	}
	
	@Override 
	public Constraint visitSm_host_cons(RulesParser.Sm_host_consContext ctx) {
		Constraint cons = visitChildren(ctx);
		((SMable)cons).setSM(ctx.WORD(0).getText());
		List<String> args = new ArrayList<String>();
		int wdCount = ctx.WORD().size();
		for(int i = 1; i < wdCount - 1;i++) {
			args.add(ctx.WORD(i).getText());
			System.out.println("args:" + ctx.WORD(i).getText());
		}
		if(ctx.LIKE() != null) {
			args.add("like");
			System.out.println("like regex:" + ctx.LIKE());
		}
		System.out.println(ctx.WORD(0).getText() + ' ' +
				ctx.WORD(wdCount -1).getText());
		((SMable)cons).setArgs(args);
		((SMable)cons).setState(ctx.WORD(wdCount - 1).getText());
		return cons;
	}
	
	@Override
	protected Constraint aggregateResult(Constraint aggregate, Constraint nextResult) {
		if(aggregate != null) return aggregate;
		return nextResult;
	}

	public String getConnectString() {
		return connectString;
	}

	public void setConnectString(String connectString) {
		this.connectString = connectString;
	}
	
}
