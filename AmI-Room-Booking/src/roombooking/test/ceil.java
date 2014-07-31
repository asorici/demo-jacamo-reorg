package roombooking.test;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;

public class ceil extends DefaultInternalAction {
	@Override public int getMinArgs() { return 3; }
    @Override public int getMaxArgs() { return 3; }

	@Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        
        checkArguments(args);
        
        NumberTerm t1 = (NumberTerm)args[0];
        NumberTerm t2 = (NumberTerm)args[1];
        
        double dividend = t1.solve();
        double divisor = t2.solve();
        
        int ceil = (int)Math.ceil(dividend / divisor);
        
        return un.unifies(args[2], new NumberTermImpl(ceil));
    }
}
