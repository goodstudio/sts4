/*******************************************************************************
 * Copyright (c) 2024 Broadcom
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Broadcom - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.beans.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.ide.vscode.boot.app.SpringSymbolIndex;
import org.springframework.ide.vscode.boot.bootiful.BootLanguageServerTest;
import org.springframework.ide.vscode.boot.bootiful.SymbolProviderTestConf;
import org.springframework.ide.vscode.boot.index.SpringMetamodelIndex;
import org.springframework.ide.vscode.commons.java.IJavaProject;
import org.springframework.ide.vscode.commons.languageserver.java.JavaProjectFinder;
import org.springframework.ide.vscode.commons.protocol.spring.AnnotationMetadata;
import org.springframework.ide.vscode.commons.protocol.spring.Bean;
import org.springframework.ide.vscode.commons.util.text.LanguageId;
import org.springframework.ide.vscode.languageserver.testharness.Editor;
import org.springframework.ide.vscode.project.harness.BootLanguageServerHarness;
import org.springframework.ide.vscode.project.harness.ProjectsHarness;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Martin Lippert
 */
@ExtendWith(SpringExtension.class)
@BootLanguageServerTest
@Import(SymbolProviderTestConf.class)
public class NamedCompletionProviderTest {

	@Autowired private BootLanguageServerHarness harness;
	@Autowired private JavaProjectFinder projectFinder;
	@Autowired private SpringMetamodelIndex springIndex;
	@Autowired private SpringSymbolIndex indexer;

	private File directory;
	private IJavaProject project;
	private Bean[] indexedBeans;
	private String tempJavaDocUri;
	private Bean bean1;
	private Bean bean2;

	@BeforeEach
	public void setup() throws Exception {
		harness.intialize(null);

		directory = new File(ProjectsHarness.class.getResource("/test-projects/test-annotation-symbols-for-jakarta-javax/").toURI());

		String projectDir = directory.toURI().toString();
		project = projectFinder.find(new TextDocumentIdentifier(projectDir)).get();

		CompletableFuture<Void> initProject = indexer.waitOperation();
		initProject.get(5, TimeUnit.SECONDS);
		
		indexedBeans = springIndex.getBeansOfProject(project.getElementName());
		
        tempJavaDocUri = directory.toPath().resolve("src/main/java/org/test/TempClass.java").toUri().toString();
        AnnotationMetadata annotationBean1 = new AnnotationMetadata("jakarta.inject.Named", false, Map.of("value", new String[] {"named1"}));
        AnnotationMetadata annotationBean2 = new AnnotationMetadata("jakarta.inject.Named", false, Map.of("value", new String[] {"named2"}));
        
		bean1 = new Bean("bean1", "type1", new Location(tempJavaDocUri, new Range(new Position(1,1), new Position(1, 20))), null, null, new AnnotationMetadata[] {annotationBean1});
		bean2 = new Bean("bean2", "type2", new Location(tempJavaDocUri, new Range(new Position(1,1), new Position(1, 20))), null, null, new AnnotationMetadata[] {annotationBean2});
		
		springIndex.updateBeans(project.getElementName(), new Bean[] {bean1, bean2});
	}
	
	@AfterEach
	public void restoreIndexState() {
		this.springIndex.updateBeans(project.getElementName(), indexedBeans);
	}
	
	@Test
	public void testQualifierCompletionWithoutQuotesWithoutPrefix() throws Exception {
		assertCompletions("@Named(<*>)", new String[] {"named1", "named2", "bean1", "bean2"}, 0, "@Named(\"named1\"<*>)");
	}

	@Test
	public void testQualifierCompletionWithoutQuotesWithPrefix() throws Exception {
		assertCompletions("@Named(be<*>)", 2, "@Named(\"bean1\"<*>)");
	}

	@Test
	public void testQualifierCompletionWithoutQuotesWithPrefixFromExistingQualifier() throws Exception {
		assertCompletions("@Named(na<*>)", new String[] {"named1", "named2"}, 0, "@Named(\"named1\"<*>)");
	}

	@Test
	public void testQualifierCompletionWithoutQuotesWithAttributeName() throws Exception {
		assertCompletions("@Named(value=<*>)", 4, "@Named(value=\"named1\"<*>)");
	}

	@Test
	public void testQualifierCompletionInsideOfQuotesWithoutPrefix() throws Exception {
		assertCompletions("@Named(\"<*>\")", 4, "@Named(\"named1<*>\")");
	}

	@Test
	public void testQualifierCompletionInsideOfQuotesWithPrefix() throws Exception {
		assertCompletions("@Named(\"be<*>\")", 2, "@Named(\"bean1<*>\")");
	}

	@Test
	public void testQualifierCompletionInsideOfQuotesWithPrefixButWithoutMatches() throws Exception {
		assertCompletions("@Named(\"XXX<*>\")", 0, null);
	}

	@Test
	public void testQualifierCompletionOutsideOfAnnotation1() throws Exception {
		assertCompletions("@Named(\"XXX\")<*>", 0, null);
	}

	@Test
	public void testQualifierCompletionOutsideOfAnnotation2() throws Exception {
		assertCompletions("@Named<*>(\"XXX\")", 0, null);
	}

	@Test
	public void testQualifierCompletionInsideOfQuotesWithPrefixAndReplacedPostfix() throws Exception {
		assertCompletions("@Named(\"be<*>xxx\")", 2, "@Named(\"bean1<*>\")");
	}
	
	private void assertCompletions(String completionLine, int noOfExpectedCompletions, String expectedCompletedLine) throws Exception {
		assertCompletions(completionLine, noOfExpectedCompletions, null, 0, expectedCompletedLine);
	}

	private void assertCompletions(String completionLine, String[] expectedCompletions, int chosenCompletion, String expectedCompletedLine) throws Exception {
		assertCompletions(completionLine, expectedCompletions.length, expectedCompletions, chosenCompletion, expectedCompletedLine);
	}

	private void assertCompletions(String completionLine, int noOfExcpectedCompletions, String[] expectedCompletions, int chosenCompletion, String expectedCompletedLine) throws Exception {
		String editorContent = """
				package org.test;

				import jakarta.inject.Named;

				""" +
				completionLine + "\n" +
				"""
				public class TestDependsOnClass {
				}
				""";
		
		Editor editor = harness.newEditor(LanguageId.JAVA, editorContent, tempJavaDocUri);

        List<CompletionItem> completions = editor.getCompletions();
        assertEquals(noOfExcpectedCompletions, completions.size());

        if (expectedCompletions != null) {
	        String[] completionItems = completions.stream()
	        	.map(item -> item.getLabel())
	        	.toArray(size -> new String[size]);
	        
	        assertArrayEquals(expectedCompletions, completionItems);
        }
        
        if (noOfExcpectedCompletions > 0) {
	        editor.apply(completions.get(chosenCompletion));
	        assertEquals("""
					package org.test;
	
					import jakarta.inject.Named;
	
					""" + expectedCompletedLine + "\n" +
					"""
					public class TestDependsOnClass {
					}
	        		""", editor.getText());
        }
	}


}