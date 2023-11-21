/*******************************************************************************
 * Copyright (c) 2017, 2023 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.snippets;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public interface JavaSnippetContext {
	JavaSnippetContext BOOT_MEMBERS = (node) -> node instanceof TypeDeclaration || node instanceof SimpleName;
	
	JavaSnippetContext AT_ROOT_LEVEL = (node) -> {
		return (node instanceof TypeDeclaration) || (node instanceof SimpleName && node.getParent() != null && node.getParent() instanceof TypeDeclaration);
	};
	

	boolean appliesTo(ASTNode node);
}
