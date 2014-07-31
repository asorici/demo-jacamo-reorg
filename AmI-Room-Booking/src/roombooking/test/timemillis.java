package roombooking.test;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;

import java.util.Calendar;

public class timemillis extends DefaultInternalAction {
	
	@Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 1; }

	@Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        
        checkArguments(args);
        	
        Calendar now = Calendar.getInstance();
        return un.unifies(args[0], new NumberTermImpl(now.getTimeInMillis()));
        
    }
}
