/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python.refactoring.classes.extractSuperclass;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtil;
import com.jetbrains.python.PyNames;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyElement;
import com.jetbrains.python.refactoring.classes.PyClassRefactoringTest;
import com.jetbrains.python.refactoring.classes.membersManager.MembersManager;
import com.jetbrains.python.refactoring.classes.membersManager.PyMemberInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dennis.Ushakov
 */
public class PyExtractSuperclassTest extends PyClassRefactoringTest {

  public PyExtractSuperclassTest() {
    super("extractsuperclass");
  }

  // Checks that moving methods between files moves imports as well
  public void testImportMultiFile() throws Throwable {
    multiFileTestHelper(".do_useful_stuff");
  }

  // Checks that moving methods between files moves superclass expressions as well
  public void testMoveExtends() throws Throwable {
    multiFileTestHelper("TheParentOfItAll");
  }

  /**
   * Moves member from class <pre>MyClass</pre> in module <pre>source_module</pre> to class <pre>NewParent</pre> in module <pre>dest_module</pre>.
   * Ensures it is moved correctly.
   *
   * @param memberToMove name of the member to move
   */
  private void multiFileTestHelper(@NotNull final String memberToMove) {
    final String[] modules = {"dest_module", "source_module"};
    configureMultiFile(ArrayUtil.mergeArrays(modules, "shared_module"));
    myFixture.configureByFile("source_module.py");
    final String sourceClass = "MyClass";
    final PyMemberInfo member = findMemberInfo(sourceClass, memberToMove);
    final String destUrl = myFixture.getFile().getVirtualFile().getParent().findChild("dest_module.py").getUrl();
    new WriteCommandAction.Simple(myFixture.getProject()) {
      @Override
      protected void run() throws Throwable {
        PyExtractSuperclassHelper.extractSuperclass(findClass(sourceClass), Collections.singleton(member), "NewParent", destUrl);
      }
    }.execute();
    checkMultiFile(modules);
  }

  public void testSimple() throws Exception {
    doSimpleTest("Foo", "Suppa", null, true, ".foo");
  }

  public void testWithSuper() throws Exception {
    doSimpleTest("Foo", "Suppa", null, true, ".foo");
  }

  public void testWithImport() throws Exception {
    doSimpleTest("A", "Suppa", null, false, ".foo");
  }

  public void testMoveFields() throws Exception {
    doSimpleTest("FromClass", "ToClass", null, true, "#instance_field", "#CLASS_FIELD");
  }

  private void doSimpleTest(final String className,
                            final String superclassName,
                            final String expectedError,
                            final boolean sameFile,
                            final String... membersName) throws Exception {
    try {
      String baseName = "/refactoring/extractsuperclass/" + getTestName(true);
      myFixture.configureByFile(baseName + ".before.py");
      final PyClass clazz = findClass(className);
      final List<PyMemberInfo> members = new ArrayList<PyMemberInfo>();
      for (String memberName : membersName) {
        final PyElement member = findMember(className, memberName);
        members.add(MembersManager.findMember(clazz, member));
      }

      new WriteCommandAction.Simple(myFixture.getProject()) {
        @Override
        protected void run() throws Throwable {
          //noinspection ConstantConditions
          final String url = sameFile ? myFixture.getFile().getVirtualFile().getUrl() :
                             myFixture.getFile().getVirtualFile().getParent().getUrl();
          PyExtractSuperclassHelper.extractSuperclass(clazz, members, superclassName, url);
        }
      }.execute();
      myFixture.checkResultByFile(baseName + ".after.py");
    }
    catch (Exception e) {
      if (expectedError == null) throw e;
      assertEquals(expectedError, e.getMessage());
    }
  }


  public void testMultifileNew() {
    String baseName = "/refactoring/extractsuperclass/multifile/";
    myFixture.configureByFile(baseName + "source.py");
    final String className = "Foo";
    final String superclassName = "Suppa";
    final PyClass clazz = findClass(className);
    final List<PyMemberInfo> members = new ArrayList<PyMemberInfo>();
    final PyElement member = findMember(className, ".foo");
    members.add(MembersManager.findMember(clazz, member));
    final VirtualFile base_dir = myFixture.getFile().getVirtualFile().getParent();

    new WriteCommandAction.Simple(myFixture.getProject()) {
      @Override
      protected void run() throws Throwable {
        //noinspection ConstantConditions
        final String path = base_dir.getPath() + "/a/b";
        PyExtractSuperclassHelper.extractSuperclass(clazz, members, superclassName, path);
      }
    }.execute();
    final PsiManager psi_mgr = PsiManager.getInstance(myFixture.getProject());
    VirtualFile vfile = base_dir.findChild("a");
    assertTrue(vfile.isDirectory());
    vfile = vfile.findChild(PyNames.INIT_DOT_PY);
    assertNotNull(vfile);

    vfile = base_dir.findChild("a").findChild("b");
    assertTrue(vfile.isDirectory());
    vfile = vfile.findChild(PyNames.INIT_DOT_PY);
    assertNotNull(vfile);

    PsiFile psi_file = psi_mgr.findFile(vfile);
    String result = psi_file.getText().trim();
    File expected_file = new File(getTestDataPath() + baseName, "target.new.py");
    String expected = psi_mgr.findFile(LocalFileSystem.getInstance().findFileByIoFile(expected_file)).getText().trim();
    assertEquals(expected, result);
  }

  public void testMultifileAppend() {
    // this is half-copy-paste of testMultifileNew. generalization won't make either easier to follow.
    String baseName = "/refactoring/extractsuperclass/multifile/";
    myFixture.configureByFiles(
      baseName + "source.py",
      baseName + "a/__init__.py",
      baseName + "a/b/__init__.py",
      baseName + "a/b/foo.py"
    );
    final String className = "Foo";
    final String superclassName = "Suppa";
    final PyClass clazz = findClass(className);
    final List<PyMemberInfo> members = new ArrayList<PyMemberInfo>();
    final PyElement member = findMember(className, ".foo");
    members.add(MembersManager.findMember(clazz, member));
    final VirtualFile base_dir = myFixture.getFile().getVirtualFile().getParent();

    new WriteCommandAction.Simple(myFixture.getProject()) {
      @Override
      protected void run() throws Throwable {
        //TODO: Test via presenter
        //noinspection ConstantConditions
        final String path = base_dir.getPath() + "/a/b";
        PyExtractSuperclassHelper.extractSuperclass(clazz, members, superclassName, path + "/foo.py");
      }
    }.execute();
    final PsiManager psi_mgr = PsiManager.getInstance(myFixture.getProject());
    VirtualFile vfile = base_dir.findChild("a");
    assertTrue(vfile.isDirectory());
    vfile = vfile.findChild(PyNames.INIT_DOT_PY);
    assertNotNull(vfile);

    vfile = base_dir.findChild("a").findChild("b");
    assertTrue(vfile.isDirectory());
    assertNotNull(vfile.findChild(PyNames.INIT_DOT_PY));
    vfile = vfile.findChild("foo.py");
    assertNotNull(vfile);

    PsiFile psi_file = psi_mgr.findFile(vfile);
    String result = psi_file.getText().trim();
    File expected_file = new File(getTestDataPath() + baseName, "target.append.py");
    String expected = psi_mgr.findFile(LocalFileSystem.getInstance().findFileByIoFile(expected_file)).getText().trim();
    assertEquals(expected, result);
  }
}
