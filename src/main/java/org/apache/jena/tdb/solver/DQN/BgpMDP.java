package org.apache.jena.tdb.solver.DQN;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.iterator.QueryIterPeek;
import org.apache.jena.sparql.engine.iterator.QueryIterRoot;
import org.apache.jena.sparql.graph.NodeConst;
import org.apache.jena.tdb.solver.OpExecutorTDB1;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.space.ActionSpace;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.space.Box;
import org.deeplearning4j.rl4j.space.ObservationSpace;
import org.json.JSONArray;

public class BgpMDP<O, A, AS extends ActionSpace<A>> implements MDP<O, A, AS> {

    private BgpActionSpace actionSpace;
    private ObservationSpace<O> observationSpace;
    protected double[] state;
    protected ArrayList<Integer> Result; // store the final join sequence
    protected ArrayList<Integer> emptyIndexResult; // store the triples that are not in the DB
    private int dimension = 0;
    private BasicPattern pattern; // store all original triples
    protected List<Integer> tripleIndexes;
    private Op op;
    private QueryIterator input;
    private ExecutionContext execCxt;
    protected int tripleNum;

    BgpMDP(int dim, BasicPattern pattern, ExecutionContext execCxt) {
        this.dimension = dim;
        this.state = new double[dimension];
        observationSpace = new ArrayObservationSpace<>(new int[] { dimension });
        this.Result = new ArrayList<>();
        this.emptyIndexResult = new ArrayList<>();

        this.pattern = pattern;
        this.execCxt = execCxt;
        this.tripleNum = pattern.getList().size();
        preProcessingTriples();
        actionSpace = new BgpActionSpace(dimension);
        actionSpace.setState(state);
        actionSpace.setTripleIndexes(tripleIndexes);
    }

    void preProcessingTriples() {
        this.tripleIndexes = new ArrayList<>();
        for (int i = 0; i < tripleNum; i++) {
            int index = getTripleIndex(pattern.getList().get(i));
            this.tripleIndexes.add(index);
            // the triple isn't in the DB
            if (index == -1) {
                this.emptyIndexResult.add(i);
            }
        }
    }

    /**
     * get index of one triple
     * 
     * @param triple the triple
     * @return index. if index==-1, the triple isn't in DB
     */
    int getTripleIndex(Triple triple) {
        // e.g. ?x :type :student
        if (NodeConst.nodeRDFType.equals(triple.getPredicate())) {
            return DQN.encodeIndex(DQN.getIndexString(triple.getObject(), "Type"));
        } else if (triple.getPredicate().isConcrete())
            return DQN.encodeIndex(DQN.getIndexString(triple.getPredicate(), "Predicate"));
        else {
            Exception e = new Exception("Unknown triple type");
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public ObservationSpace<O> getObservationSpace() {
        return observationSpace;
    }

    @Override
    public AS getActionSpace() {
        return (AS) actionSpace;
    }

    /**
     * @return the tripleIndexes
     */
    public List<Integer> getTripleIndexes() {
        return tripleIndexes;
    }

    /**
     * @return the result
     */
    public ArrayList<Integer> getResult() {
        return Result;
    }

    /**
     * @return the tripleNum
     */
    public int getTripleNum() {
        return tripleNum;
    }

    @Override
    public O reset() {
        for (int i = 0; i < dimension; i++) {
            state[i] = 0;
        }
        Result.clear();
        // add triples that are not in the DB
        Result.addAll(emptyIndexResult);
        return (O) new Box(new JSONArray(state));
    }

    @Override
    public void close() {
        System.out.println(Result.toString());
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
        double r = -runQuery();
        r = Math.exp(r) * 10;
        // Random R = new Random();
        // double r = R.nextDouble() * 10;
        if (isDone()) {
            System.out.println("Epoch finished: " + Result);
        }
        System.out.println("Step: " + Result);
        System.out.println(action);
        return new StepReply(new Box(new JSONArray(state)), r, isDone(), null);
    }

    @Override
    public boolean isDone() {
        return Result.size() == tripleIndexes.size();
    }

    @Override
    public MDP<O, A, AS> newInstance() {
        return new BgpMDP<>(dimension, this.pattern, this.execCxt);
    }

    /**
     * run query to get execution time
     * 
     * @return execution time. unit: ms
     */
    long runQuery() {
        initInputIterator();
        List<Triple> triples = pattern.getList();
        BasicPattern newPattern = new BasicPattern();
        Result.forEach(o -> newPattern.add(triples.get(o)));
        op = new OpBGP(newPattern);
        QueryIterator q = OpExecutorTDB1.plainExecute(op, this.input, execCxt);
        long startTime = System.currentTimeMillis();
        for (; q.hasNext(); q.nextBinding())
            ;
        return System.currentTimeMillis() - startTime;
    }

    void initInputIterator() {
        this.input = QueryIterRoot.create(execCxt);
        QueryIterPeek peek = QueryIterPeek.create(this.input, execCxt);
        this.input = peek; // Must pass on
    }
}