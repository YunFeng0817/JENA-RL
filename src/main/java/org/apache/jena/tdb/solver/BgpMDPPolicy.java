package org.apache.jena.tdb.solver;

import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.space.ActionSpace;
import org.deeplearning4j.rl4j.space.Box;
import org.json.JSONArray;

public class BgpMDPPolicy<O, A, AS extends ActionSpace<A>> extends BgpMDP<O, A, AS> {

    BgpMDPPolicy(int dim, BasicPattern pattern, ExecutionContext execCxt) {
        super(dim, pattern, execCxt);
    }

    @Override
    public StepReply<O> step(A action) {
        for (int i = 0; i < tripleNum; i++) {
            if (tripleIndexes.get(i) == (Integer) action) {
                Result.add(i);
                break;
            } else if (i == tripleNum - 1) {
                Exception e = new Exception("Invalid Action" + action);
                e.printStackTrace();
            }
        }
        initInputIterator();
        state[(Integer) action] = 1;
        double r = 0;
        // Random R = new Random();
        // double r = R.nextDouble() * 10;
        if (isDone()) {
            r = -runQuery();
            System.out.println("Epoch finished: " + Result);
            System.out.println("Run time: " + -r);
        }
        return new StepReply(new Box(new JSONArray(state)), r, isDone(), null);
    }
}