/**
 * autoMate - yet another automaton library for Java
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Julian Thome <julian.thome.de@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 **/

package com.github.julianthome.automate.ext;

import com.github.julianthome.automate.core.Automaton;
import com.github.julianthome.automate.core.AutomatonFactory;
import com.github.julianthome.automate.core.AutomatonInterface;
import com.github.julianthome.automate.core.DotSerializer;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class TrackAutomaton extends DirectedAcyclicGraph<MemAutomatonNode,
        MemAutomatonEdge> implements DotSerializer,
        AutomatonInterface<TrackAutomaton>{

    final static Logger LOGGER = LoggerFactory.getLogger(AutomatonFactory.class);

    private MemAutomatonNode root = null;
    private int id = 0;

    public TrackAutomaton(String name, String rexp) {
        super(MemAutomatonEdge.class);
        MemAutomatonNode mn  = getNewNodeOfKind(MemAutomatonNode.Kind.LEAF,
                AutomatonFactory.getInstance().getNewAutomaton(rexp));
        root = mn;
        root.getAutomaton().setName(name);
    }


    public TrackAutomaton(String name) {
        this();
        root.getAutomaton().setName(name);
    }

    public TrackAutomaton(){
        this(AutomatonFactory.getInstance().getAllAccepting());
    }

    public TrackAutomaton(Automaton a){
        super(MemAutomatonEdge.class);
        root = getNewNodeOfKind(MemAutomatonNode.Kind.LEAF, a);
        addVertex(root);
    }

    protected TrackAutomaton(MemAutomatonNode.Kind kind, Automaton a){
        super(MemAutomatonEdge.class);
        root = getNewNodeOfKind(kind, a);
        addVertex(root);
    }


    protected TrackAutomaton(TrackAutomaton m) {
        super(MemAutomatonEdge.class);
        Map<MemAutomatonNode,MemAutomatonNode> smap = new HashMap<>();

        for(MemAutomatonNode s : m.vertexSet()) {
            MemAutomatonNode mn = getNewNodeOfKind(s.getKind(), s.getAutomaton());
            smap.put(s, mn);
        }

        for(MemAutomatonEdge s : m.edgeSet()) {
            try {
                addDagEdge(smap.get(s.getSource()),smap.get(s.getTarget()));
            } catch (CycleFoundException e) {
                assert false;
            }
        }

        root = smap.get(m.root);
        id = m.id;
    }

    private MemAutomatonNode getNewNodeOfKind(MemAutomatonNode.Kind kind,
                                              Automaton a) {
        return new MemAutomatonNode(kind, a, id++);
    }




    public MemAutomatonNode getRoot() {
        return root;
    }

    public void setRoot(MemAutomatonNode root) {
        this.root = root;
    }


    public TrackAutomaton union(TrackAutomaton a) {
        return performBinOp(MemAutomatonNode.Kind.UNION, this, a);
    }

    @Override
    public TrackAutomaton intersect(TrackAutomaton other) {
       return performBinOp(MemAutomatonNode.Kind.INTERSECTION, this, other);
    }

    @Override
    public TrackAutomaton complement() {
        return performUnaryOp(MemAutomatonNode.Kind.COMPLEMENT, this);
    }

    @Override
    public TrackAutomaton minus(TrackAutomaton other) {
        return performBinOp(MemAutomatonNode.Kind.MINUS, this, other);
    }

    @Override
    public TrackAutomaton concat(TrackAutomaton other) {
        return performBinOp(MemAutomatonNode.Kind.CONCAT, this, other);
    }


    private static TrackAutomaton performBinOp(MemAutomatonNode.Kind kind,
                                         TrackAutomaton a1, TrackAutomaton a2) {

        Automaton a1root = a1.getRoot().getAutomaton();
        Automaton a2root = a2.getRoot().getAutomaton();

        Automaton result = null;

        switch(kind) {
            case UNION:
                result = a1root.union(a2root);
                break;
            case INTERSECTION:
                result = a1root.union(a2root);
                break;
            case MINUS:
                result = a1root.minus(a2root);
                break;
            case CONCAT:
                result = a1root.concat(a2root);
                break;
        }

        TrackAutomaton ta = new TrackAutomaton(kind, result);
        ta.addSubGraph(a1);
        ta.addSubGraph(a2);

        return ta;
    }

    private static TrackAutomaton performUnaryOp(MemAutomatonNode.Kind kind,
                                               TrackAutomaton a1) {

        Automaton a1root = a1.getRoot().getAutomaton();

        Automaton result = null;

        switch(kind) {
            case COMPLEMENT:
                result = a1root.complement();
                break;
        }

        TrackAutomaton ta = new TrackAutomaton(kind, result);
        ta.addSubGraph(a1);

        return ta;
    }



    private void addSubGraph(TrackAutomaton other) {

        Map<MemAutomatonNode,MemAutomatonNode> smap = new HashMap<>();

        for(MemAutomatonNode s : other.getOrderedVertices()) {
            MemAutomatonNode mn = getNewNodeOfKind(s.getKind(), new Automaton(s.getAutomaton()));
            smap.put(s, mn);
        }

        for(MemAutomatonNode n : smap.values()) {
            LOGGER.debug("add v {}", n.getId());
            addVertex(n);
        }

        for(MemAutomatonEdge s : other.edgeSet()) {
            try {
                addDagEdge(smap.get(s.getSource()),smap.get(s.getTarget()));
            } catch (CycleFoundException e) {
                assert false;
            }
        }

        try {
            addDagEdge(root, smap.get(other.root));
        } catch (CycleFoundException e) {
            assert false;
        }

    }


    private Set<MemAutomatonNode> getOrderedVertices() {
        Set<MemAutomatonNode> tset = new TreeSet<>();
        tset.addAll(vertexSet());
        return tset;
    }



    public String toDot() {

        StringBuilder sb = new StringBuilder();
        sb.append("digraph {\n" +
                "\trankdir=TB;\n");

        sb.append("\tnode [fontname=Helvetica,fontsize=11];\n");
        sb.append("\tedge [fontname=Helvetica,fontsize=10];\n");


        for (MemAutomatonNode n : getOrderedVertices()) {
            String shape = "";
            String color = "";

            String name = n.getAutomaton().hasName() ? "\\n" + n.getAutomaton()
                    .getName() : "";

            sb.append("\tn" + n.getId() + " [label=\"" + n.getKind().toString
                    () +"\\n[" + n.getId() + "]" + name + "\"," +
                    "shape=\"" +
                    shape +
                    "\", color=\"" +
                    color + "\"];\n");
        }


        for (MemAutomatonEdge e : this.edgeSet())  {

            MemAutomatonNode src = e.getSource();
            MemAutomatonNode dst = e.getTarget();


            sb.append("\tn" + src.getId() + " -> n" + dst.getId() +"\n");
        }
        sb.append("}\n");

        return sb.toString();
    }



}
