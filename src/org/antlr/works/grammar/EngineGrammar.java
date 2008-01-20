/*

[The "BSD licence"]
Copyright (c) 2005 Jean Bovet
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/


package org.antlr.works.grammar;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import org.antlr.Tool;
import org.antlr.analysis.NFAState;
import org.antlr.tool.*;
import org.antlr.works.ate.syntax.misc.ATEToken;
import org.antlr.works.components.grammar.CEditorGrammar;
import org.antlr.works.syntax.element.ElementGrammarName;
import org.antlr.works.syntax.element.ElementRule;
import org.antlr.works.utils.Console;
import org.antlr.works.utils.ErrorListener;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EngineGrammar {

    private Grammar parserGrammar;
    private Grammar lexerGrammar;
    private List<EngineGrammarError> errors;

    private boolean needsToCreateGrammar;
    private boolean needsToAnalyzeGrammar;

    private EngineGrammarResult createGrammarResult = new EngineGrammarResult();
    private EngineGrammarResult analyzeResult = new EngineGrammarResult();

    private CEditorGrammar editor;
    private EngineGrammarDelegate delegate;

    public EngineGrammar(CEditorGrammar editor) {
        this.editor = editor;
        errors = new ArrayList<EngineGrammarError>();
        markDirty();
    }

    public void close() {
        delegate = null;
        editor = null;
        errors = null;
    }

    public EngineGrammarDelegate getDelegate() {
        return delegate;
    }

    public void setDelegate(EngineGrammarDelegate delegate) {
        this.delegate = delegate;
    }

    public void markDirty() {
        needsToCreateGrammar = true;
        needsToAnalyzeGrammar = true;
    }

    public Grammar getParserGrammar() {
        return parserGrammar;
    }

    public Grammar getLexerGrammar() {
        return lexerGrammar;
    }

    public NFAState getRuleStartState(String name) throws Exception {
        Grammar g;
        createGrammars();
        if(ATEToken.isLexerName(name))
            g = getLexerGrammar();
        else
            g = getParserGrammar();

        return g == null ? null:g.getRuleStartState(name);
    }

    public Grammar getGrammarForRule(String name) throws Exception {
        createGrammars();
        if(ATEToken.isLexerName(name))
            return getLexerGrammar();
        else
            return getParserGrammar();
    }

    public List<EngineGrammarError> getErrors() {
        return errors;
    }

    public boolean isTreeParserGrammar() {
        return getType() == ElementGrammarName.TREEPARSER;
    }

    public boolean hasGrammar() {
        switch(getType()) {
            case ElementGrammarName.COMBINED:
                return parserGrammar != null;
            case ElementGrammarName.TREEPARSER:
            case ElementGrammarName.PARSER:
                return parserGrammar != null;
            case ElementGrammarName.LEXER:
                return lexerGrammar != null;
        }
        return false;
    }

    public Grammar getANTLRGrammar() {
        switch(getType()) {
            case ElementGrammarName.COMBINED:
                return parserGrammar;
            case ElementGrammarName.TREEPARSER:
            case ElementGrammarName.PARSER:
                return parserGrammar;
            case ElementGrammarName.LEXER:
                return lexerGrammar;
        }
        return null;
    }

    public Tool getANTLRTool() {
        return delegate.getANTLRTool();
    }

    public String getName() {
        ElementGrammarName name = editor.parserEngine.getName();
        if(name == null)
            return null;
        else
            return name.getName();
    }

    public int getType() {
        ElementGrammarName name = editor.parserEngine.getName();
        if(name == null)
            return ElementGrammarName.COMBINED;
        else
            return name.getType();
    }

    public String getFileName() {
        String fileName = delegate.getFileName();
        return fileName==null?"<notsaved>":fileName;
    }

    public void createGrammars() throws Exception {
        if(!needsToCreateGrammar) return;

        ErrorListener el = ErrorListener.getThreadInstance();
        ErrorManager.setErrorListener(el);

        parserGrammar = null;
        lexerGrammar = null;

        try {
            switch(getType()) {
                case ElementGrammarName.COMBINED:
                    createCombinedGrammar();
                    break;
                case ElementGrammarName.TREEPARSER:
                case ElementGrammarName.PARSER:
                    createParserGrammar();
                    break;
                case ElementGrammarName.LEXER:
                    createLexerGrammar();
                    break;
            }

            // if no exception, then assume create grammar was successful
            needsToCreateGrammar = false;
        } finally {
            // store the result of creating the grammars
            createGrammarResult.setErrors(el.errors);
            createGrammarResult.setWarnings(el.warnings);

            el.clear();
        }
    }

    private Grammar createNewGrammar(String filename, String content) throws TokenStreamException, RecognitionException {
        Grammar g = new Grammar();
        g.setTool(getANTLRTool());
        g.setFileName(filename);
        g.setGrammarContent(content);
        g.composite.createNFAs();
        return g;
    }

    private void createCombinedGrammar() throws Exception {
        createParserGrammar();
        lexerGrammar = createLexerGrammarFromCombinedGrammar(parserGrammar);
    }

    private Grammar createLexerGrammarFromCombinedGrammar(Grammar grammar) throws Exception {
        String lexerGrammarStr = grammar.getLexerGrammar();
        if(lexerGrammarStr == null)
            return null;

        Grammar lexerGrammar = new Grammar();
        lexerGrammar.implicitLexer = true;
        lexerGrammar.setTool(getANTLRTool());
        lexerGrammar.setFileName("<internally-generated-lexer>");
        lexerGrammar.importTokenVocabulary(grammar);

        lexerGrammar.setGrammarContent(lexerGrammarStr);
        lexerGrammar.composite.createNFAs();

        return lexerGrammar;
    }

    private void createParserGrammar() throws TokenStreamException, RecognitionException {
        parserGrammar = createNewGrammar(getFileName(), delegate.getText());
    }

    private void createLexerGrammar() throws TokenStreamException, RecognitionException {
        lexerGrammar = createNewGrammar(getFileName(), editor.getText());
    }

    public void printLeftRecursionToConsole(List rules) {
        StringBuffer info = new StringBuffer();
        info.append("Aborting because the following rules are mutually left-recursive:");
        for (Object rule : rules) {
            Set rulesSet = (Set) rule;
            info.append("\n    ");
            info.append(rulesSet);
        }
        editor.getConsole().println(info.toString(), Console.LEVEL_ERROR);
    }

    public void markLeftRecursiveRules(List rules) {
        // 'rules' is a list of set of rules given by ANTLR
        for (Object rule : rules) {
            Set rulesSet = (Set) rule;
            for (Object aRulesSet : rulesSet) {
                String name = (String) aRulesSet;
                ElementRule r = editor.rules.getRuleWithName(name);
                if (r == null)
                    continue;
                r.setLeftRecursiveRulesSet(rulesSet);
            }
        }
    }

    public EngineGrammarResult analyze() throws Exception {
        // if there is no need to analyze the grammar, return the previous result
        if(!needsToAnalyzeGrammar) {
            return analyzeCompleted(null);
        }

        // Set the error listener
        ErrorListener el = ErrorListener.getThreadInstance();
        ErrorManager.setErrorListener(el);

        createGrammars();

        Grammar g = getANTLRGrammar();
        if(g == null) {
            return analyzeCompleted(el);
        }

        List rules = g.checkAllRulesForLeftRecursion();
        if(!rules.isEmpty()) {
            printLeftRecursionToConsole(rules);
            markLeftRecursiveRules(rules);
        }

        if(ErrorManager.doNotAttemptAnalysis()) {
            return analyzeCompleted(el);
        }

        try {
            if ( g.nfa==null ) {
                g.composite.createNFAs();
            }
            g.createLookaheadDFAs();
            if(getType() == ElementGrammarName.COMBINED) {
                // If the grammar is combined, analyze also the lexer
                if(lexerGrammar != null) {
                    lexerGrammar.composite.createNFAs();
                    lexerGrammar.createLookaheadDFAs();
                }
            }

            buildNonDeterministicErrors(el);
            markRulesWithWarningsOrErrors();
        } catch(Exception e) {
            // ignore
        }

        return analyzeCompleted(el);
    }

    public EngineGrammarResult analyzeCompleted(ErrorListener el) throws InvocationTargetException, InterruptedException {
        if(SwingUtilities.isEventDispatchThread()) {
            editor.engineGrammarDidAnalyze();
        } else {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    editor.engineGrammarDidAnalyze();
                }
            });
        }

        if(el != null) {
            // no need to analyze the grammar
            needsToAnalyzeGrammar = false;

            // store the analyze result
            analyzeResult.setErrors(el.errors);
            analyzeResult.setWarnings(el.warnings);

            // clear the error listener
            el.clear();
        }

        return getCompleteResult();
    }

    private EngineGrammarResult getCompleteResult() {
        EngineGrammarResult result = new EngineGrammarResult();
        result.errors.clear();
        result.errors.addAll(createGrammarResult.errors);
        result.errors.addAll(analyzeResult.errors);

        result.warnings.clear();
        result.warnings.addAll(createGrammarResult.warnings);
        result.warnings.addAll(analyzeResult.warnings);

        createGrammarResult.errors.clear();
        analyzeResult.errors.clear();

        return result;
    }

    public void cancel() {
        Grammar g = getANTLRGrammar();
        if(g != null)
            g.externallyAbortNFAToDFAConversion();
    }

    protected void buildNonDeterministicErrors(ErrorListener el) {
        errors.clear();
        for (Message warning : el.warnings) {
            buildError(warning);
        }
        for (Message error : el.errors) {
            buildError(error);
        }
    }

    protected void buildError(Object o) {
        if(o instanceof GrammarUnreachableAltsMessage)
            errors.add(buildUnreachableAltsError((GrammarUnreachableAltsMessage)o));
        else if(o instanceof GrammarNonDeterminismMessage)
            errors.add(buildNonDeterministicError((GrammarNonDeterminismMessage)o));
        else if(o instanceof NonRegularDecisionMessage)
            errors.add(buildNonRegularDecisionError((NonRegularDecisionMessage)o));
    }

    protected EngineGrammarError buildNonDeterministicError(GrammarNonDeterminismMessage message) {
        EngineGrammarError error = new EngineGrammarError();
        error.setLine(message.probe.dfa.getDecisionASTNode().getLine()-1);

        List labels = message.probe.getSampleNonDeterministicInputSequence(message.problemState);
        error.setLabels(labels);

        String input = message.probe.getInputSequenceDisplay(labels);
        error.setMessageText("Decision can match input such as \""+input+"\" using multiple alternatives");
        error.setMessage(message);

        return error;
    }

    protected EngineGrammarError buildUnreachableAltsError(GrammarUnreachableAltsMessage message) {
        EngineGrammarError error = new EngineGrammarError();

        error.setLine(message.probe.dfa.getDecisionASTNode().getLine()-1);
        error.setMessageText("The following alternatives are unreachable: "+message.alts);
        error.setMessage(message);

        return error;
    }

    protected EngineGrammarError buildNonRegularDecisionError(NonRegularDecisionMessage message) {
        EngineGrammarError error = new EngineGrammarError();

        error.setLine(message.probe.dfa.getDecisionASTNode().getLine()-1);
        error.setMessageText(message.toString());
        error.setMessage(message);

        return error;
    }

    protected void markRulesWithWarningsOrErrors() throws Exception {
        // Clear graphic cache because we have to redraw each rule again
        editor.visual.clearCacheGraphs();
        for (ElementRule rule : editor.getParserEngine().getRules()) {
            updateRuleWithErrors(rule, fetchErrorsForRule(rule));
        }

        editor.rules.refreshRules();
    }

    protected void updateRuleWithErrors(ElementRule rule, List<EngineGrammarError> errors) throws Exception {
        rule.setErrors(errors);
        rule.setNeedsToBuildErrors(true);
    }

    protected List<EngineGrammarError> fetchErrorsForRule(ElementRule rule) {
        List<EngineGrammarError> errors = new ArrayList<EngineGrammarError>();
        for (EngineGrammarError error : getErrors()) {
            if (error.line >= rule.start.startLineNumber && error.line <= rule.end.startLineNumber)
                errors.add(error);
        }
        return errors;
    }

    public void computeRuleErrors(ElementRule rule) {
        List<EngineGrammarError> errors = rule.getErrors();
        for (EngineGrammarError error : errors) {
            Object o = error.getMessage();
            if (o instanceof GrammarUnreachableAltsMessage)
                computeRuleError(rule, error, (GrammarUnreachableAltsMessage) o);
            else if (o instanceof GrammarNonDeterminismMessage)
                computeRuleError(rule, error, (GrammarNonDeterminismMessage) o);
            else if (o instanceof NonRegularDecisionMessage)
                computeRuleError(rule, error, (NonRegularDecisionMessage) o);
        }

        try {
            editor.visual.createGraphsForRule(rule);
        } catch (Exception e) {
            // ignore
        }

        rule.setNeedsToBuildErrors(false);
    }

    public void computeRuleError(ElementRule rule, EngineGrammarError error, GrammarNonDeterminismMessage message) {
        List nonDetAlts = message.probe.getNonDeterministicAltsForState(message.problemState);
        Set disabledAlts = message.probe.getDisabledAlternatives(message.problemState);

        int firstAlt = 0;

        for (Object nonDetAlt : nonDetAlts) {
            Integer displayAltI = (Integer) nonDetAlt;
            NFAState nfaStart = message.probe.dfa.getNFADecisionStartState();

            int tracePathAlt = nfaStart.translateDisplayAltToWalkAlt(displayAltI);
            if (firstAlt == 0)
                firstAlt = tracePathAlt;

            List path =
                    message.probe.getNFAPathStatesForAlt(firstAlt,
                            tracePathAlt,
                            error.getLabels());

            error.addPath(path, disabledAlts.contains(displayAltI));
            error.addStates(path);

            // Find all rules enclosing each state (because a path can extend over multiple rules)
            for (Object aPath : path) {
                NFAState state = (NFAState) aPath;
                error.addRule(state.enclosingRule.name);
            }
        }
    }

    public void computeRuleError(ElementRule rule, EngineGrammarError error, GrammarUnreachableAltsMessage message) {
        NFAState state = message.probe.dfa.getNFADecisionStartState();
        for (Object alt : message.alts) {
            error.addUnreachableAlt(state, (Integer) alt);
            error.addStates(state);
            error.addRule(state.enclosingRule.name);
        }
    }

    public void computeRuleError(ElementRule rule, EngineGrammarError error, NonRegularDecisionMessage message) {
        NFAState state = message.probe.dfa.getNFADecisionStartState();
        for (Object alt : message.altsWithRecursion) {
            // Use currently the unreachable alt for display purpose only
            error.addUnreachableAlt(state, (Integer) alt);
            error.addStates(state);
            error.addRule(state.enclosingRule.name);
        }
    }

}
