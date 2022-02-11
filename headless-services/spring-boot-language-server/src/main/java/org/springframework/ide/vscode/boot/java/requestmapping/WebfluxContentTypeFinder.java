/*******************************************************************************
 * Copyright (c) 2018, 2022 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.requestmapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Range;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J.FieldAccess;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType.Method;
import org.springframework.ide.vscode.boot.java.utils.ORAstUtils;
import org.springframework.ide.vscode.commons.util.BadLocationException;
import org.springframework.ide.vscode.commons.util.text.TextDocument;

/**
 * @author Martin Lippert
 */
public class WebfluxContentTypeFinder extends JavaIsoVisitor<ExecutionContext> {
	
	private List<WebfluxRouteElement> contentTypes;
	private TextDocument doc;
	
	public WebfluxContentTypeFinder(TextDocument doc) {
		this.doc = doc;
		this.contentTypes = new ArrayList<>();
	}
	
	public List<WebfluxRouteElement> getContentTypes() {
		return contentTypes;
	}
	
	@Override
	public MethodInvocation visitMethodInvocation(MethodInvocation methodInvocation, ExecutionContext p) {
		Method method = methodInvocation.getMethodType();
		if (method != null && WebfluxUtils.isRouteMethodInvocation(method)) {
			try {
				if (method.getDeclaringType() != null && WebfluxUtils.REQUEST_PREDICATES_TYPE.equals(method.getDeclaringType().getFullyQualifiedName())) {
					String name = method.getName();
					if (name != null && WebfluxUtils.REQUEST_PREDICATE_CONTENT_TYPE_METHOD.equals(name)) {
						FieldAccess nameArgument = WebfluxUtils.extractArgument(methodInvocation, FieldAccess.class);
						if (nameArgument != null && nameArgument.getTarget() != null) {
							org.openrewrite.marker.Range r = ORAstUtils.getRange(nameArgument);
							Range range = doc.toRange(r.getStart().getOffset(),  r.length());
							contentTypes.add(new WebfluxRouteElement(nameArgument.getSimpleName(), range));
						}
					}
				}
			}
			catch (BadLocationException e) {
				// ignore
			}
			return methodInvocation;
		}
		return super.visitMethodInvocation(methodInvocation, p);
	}
	
}
