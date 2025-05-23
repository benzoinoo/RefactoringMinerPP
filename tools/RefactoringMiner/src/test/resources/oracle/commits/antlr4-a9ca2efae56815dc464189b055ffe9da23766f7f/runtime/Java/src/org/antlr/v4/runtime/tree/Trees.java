/*
 * [The "BSD license"]
 *  Copyright (c) 2012 Terence Parr
 *  Copyright (c) 2012 Sam Harwell
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.antlr.v4.runtime.tree;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Predicate;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.gui.TreePostScriptGenerator;
import org.antlr.v4.runtime.tree.gui.TreeTextProvider;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** A set of utility routines useful for all kinds of ANTLR trees. */
public class Trees {

	public static String getPS(Tree t, List<String> ruleNames,
							   String fontName, int fontSize)
	{
		TreePostScriptGenerator psgen =
			new TreePostScriptGenerator(ruleNames, t, fontName, fontSize);
		return psgen.getPS();
	}

	public static String getPS(Tree t, List<String> ruleNames) {
		return getPS(t, ruleNames, "Helvetica", 11);
	}

	public static void writePS(Tree t, List<String> ruleNames,
							   String fileName,
							   String fontName, int fontSize)
		throws IOException
	{
		String ps = getPS(t, ruleNames, fontName, fontSize);
		FileWriter f = new FileWriter(fileName);
		BufferedWriter bw = new BufferedWriter(f);
		try {
			bw.write(ps);
		}
		finally {
			bw.close();
		}
	}

	public static void writePS(Tree t, List<String> ruleNames, String fileName)
		throws IOException
	{
		writePS(t, ruleNames, fileName, "Helvetica", 11);
	}

	/** Print out a whole tree in LISP form. {@link #getNodeText} is used on the
	 *  node payloads to get the text for the nodes.  Detect
	 *  parse trees and extract data appropriately.
	 */
	public static String toStringTree(Tree t) {
		return toStringTree(t, (List<String>)null);
	}

	/** Print out a whole tree in LISP form. {@link #getNodeText} is used on the
	 *  node payloads to get the text for the nodes.  Detect
	 *  parse trees and extract data appropriately.
	 */
	public static String toStringTree(Tree t, Parser recog) {
		String[] ruleNames = recog != null ? recog.getRuleNames() : null;
		List<String> ruleNamesList = ruleNames != null ? Arrays.asList(ruleNames) : null;
		return toStringTree(t, ruleNamesList);
	}

	/** Print out a whole tree in LISP form. Arg nodeTextProvider is used on the
	 *  node payloads to get the text for the nodes.
	 *
	 *  @since 4.5.1
	 */
	public static String toStringTree(Tree t, TreeTextProvider nodeTextProvider) {
		if ( t==null ) return "null";
		String s = Utils.escapeWhitespace(nodeTextProvider.getText(t), false);
		if ( t.getChildCount()==0 ) return s;
		StringBuilder buf = new StringBuilder();
		buf.append("(");
		s = Utils.escapeWhitespace(nodeTextProvider.getText(t), false);
		buf.append(s);
		buf.append(' ');
		for (int i = 0; i<t.getChildCount(); i++) {
			if ( i>0 ) buf.append(' ');
			buf.append(toStringTree(t.getChild(i), nodeTextProvider));
		}
		buf.append(")");
		return buf.toString();
	}

	/** Print out a whole tree in LISP form. {@link #getNodeText} is used on the
	 *  node payloads to get the text for the nodes.
	 */
	public static String toStringTree(final Tree t, final List<String> ruleNames) {
		return toStringTree(t, new TreeTextProvider() {
			@Override
			public String getText(Tree node) {
				return getNodeText(node, ruleNames);
			}
		});
	}

	public static String getNodeText(Tree t, Parser recog) {
		String[] ruleNames = recog != null ? recog.getRuleNames() : null;
		List<String> ruleNamesList = ruleNames != null ? Arrays.asList(ruleNames) : null;
		return getNodeText(t, ruleNamesList);
	}

	public static String getNodeText(Tree t, List<String> ruleNames) {
		if ( ruleNames!=null ) {
			if ( t instanceof RuleNode ) {
				int ruleIndex = ((RuleNode)t).getRuleContext().getRuleIndex();
				String ruleName = ruleNames.get(ruleIndex);
				return ruleName;
			}
			else if ( t instanceof ErrorNode) {
				return t.toString();
			}
			else if ( t instanceof TerminalNode) {
				Token symbol = ((TerminalNode)t).getSymbol();
				if (symbol != null) {
					String s = symbol.getText();
					return s;
				}
			}
		}
		// no recog for rule names
		Object payload = t.getPayload();
		if ( payload instanceof Token ) {
			return ((Token)payload).getText();
		}
		return t.getPayload().toString();
	}

	/** Return ordered list of all children of this node */
	public static List<Tree> getChildren(Tree t) {
		List<Tree> kids = new ArrayList<Tree>();
		for (int i=0; i<t.getChildCount(); i++) {
			kids.add(t.getChild(i));
		}
		return kids;
	}

	/** Return a list of all ancestors of this node.  The first node of
	 *  list is the root and the last is the parent of this node.
	 *
	 *  @since 4.5.1
	 */
	public static List<? extends Tree> getAncestors(Tree t) {
		if ( t.getParent()==null ) return Collections.emptyList();
		List<Tree> ancestors = new ArrayList<Tree>();
		t = t.getParent();
		while ( t!=null ) {
			ancestors.add(0, t); // insert at start
			t = t.getParent();
		}
		return ancestors;
	}

	/** Return true if t is u's parent or a node on path to root from u.
	 *  Use == not equals().
	 *
	 *  @since 4.5.1
	 */
	public static boolean isAncestorOf(Tree t, Tree u) {
		if ( t==null || u==null || t.getParent()==null ) return false;
		Tree p = u.getParent();
		while ( p!=null ) {
			if ( t == p ) return true;
			p = p.getParent();
		}
		return false;
	}

	public static Collection<ParseTree> findAllTokenNodes(ParseTree t, int ttype) {
		return findAllNodes(t, ttype, true);
	}

	public static Collection<ParseTree> findAllRuleNodes(ParseTree t, int ruleIndex) {
		return findAllNodes(t, ruleIndex, false);
	}

	public static List<ParseTree> findAllNodes(ParseTree t, int index, boolean findTokens) {
		List<ParseTree> nodes = new ArrayList<ParseTree>();
		_findAllNodes(t, index, findTokens, nodes);
		return nodes;
	}

	public static void _findAllNodes(ParseTree t, int index, boolean findTokens,
									 List<? super ParseTree> nodes)
	{
		// check this node (the root) first
		if ( findTokens && t instanceof TerminalNode ) {
			TerminalNode tnode = (TerminalNode)t;
			if ( tnode.getSymbol().getType()==index ) nodes.add(t);
		}
		else if ( !findTokens && t instanceof ParserRuleContext ) {
			ParserRuleContext ctx = (ParserRuleContext)t;
			if ( ctx.getRuleIndex() == index ) nodes.add(t);
		}
		// check children
		for (int i = 0; i < t.getChildCount(); i++){
			_findAllNodes(t.getChild(i), index, findTokens, nodes);
		}
	}

	/** Get all descendents; includes t itself.
	 *
	 * @since 4.5.1
 	 */
	public static List<ParseTree> getDescendants(ParseTree t) {
		List<ParseTree> nodes = new ArrayList<ParseTree>();
		nodes.add(t);

		int n = t.getChildCount();
		for (int i = 0 ; i < n ; i++){
			nodes.addAll(getDescendants(t.getChild(i)));
		}
		return nodes;
	}

	/** @deprecated */
	public static List<ParseTree> descendants(ParseTree t) {
		return getDescendants(t);
	}

	/** Find smallest subtree of t enclosing range startTokenIndex..stopTokenIndex
	 *  inclusively using postorder traversal.  Recursive depth-first-search.
	 *
	 *  @since 4.5.1
	 */
	public static ParserRuleContext getRootOfSubtreeEnclosingRegion(ParseTree t,
																	int startTokenIndex, // inclusive
																	int stopTokenIndex)  // inclusive
	{
		int n = t.getChildCount();
		for (int i = 0; i<n; i++) {
			ParseTree child = t.getChild(i);
			ParserRuleContext r = getRootOfSubtreeEnclosingRegion(child, startTokenIndex, stopTokenIndex);
			if ( r!=null ) return r;
		}
		if ( t instanceof ParserRuleContext ) {
			ParserRuleContext r = (ParserRuleContext) t;
			if ( startTokenIndex>=r.getStart().getTokenIndex() && // is range fully contained in t?
				 (r.getStop()==null || stopTokenIndex<=r.getStop().getTokenIndex()) )
			{
				// note: r.getStop()==null likely implies that we bailed out of parser and there's nothing to the right
				return r;
			}
		}
		return null;
	}

	/** Replace any subtree siblings of root that are completely to left
	 *  or right of lookahead range with a CommonToken(Token.INVALID_TYPE,"...")
	 *  node. The source interval for t is not altered to suit smaller range!
	 *
	 *  WARNING: destructive to t.
	 *
	 *  @since 4.5.1
	 */
	public static void stripChildrenOutOfRange(ParserRuleContext t,
											   ParserRuleContext root,
											   int startIndex,
											   int stopIndex)
	{
		if ( t==null ) return;
		for (int i = 0; i < t.getChildCount(); i++) {
			ParseTree child = t.getChild(i);
			Interval range = child.getSourceInterval();
			if ( child instanceof ParserRuleContext && (range.b < startIndex || range.a > stopIndex) ) {
				if ( isAncestorOf(child, root) ) { // replace only if subtree doesn't have displayed root
					CommonToken abbrev = new CommonToken(Token.INVALID_TYPE, "...");
					t.children.set(i, new TerminalNodeImpl(abbrev));
				}
			}
		}
	}

	/** Return first node satisfying the pred
	 *
 	 *  @since 4.5.1
	 */
	public static Tree findNodeSuchThat(Tree t, Predicate<Tree> pred) {
		if ( pred.apply(t) ) return t;

		int n = t.getChildCount();
		for (int i = 0 ; i < n ; i++){
			Tree u = findNodeSuchThat(t.getChild(i), pred);
			if ( u!=null ) return u;
		}
		return null;
	}

	private Trees() {
	}
}
