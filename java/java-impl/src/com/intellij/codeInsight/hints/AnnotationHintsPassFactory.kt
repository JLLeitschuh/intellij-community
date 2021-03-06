// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile

class AnnotationHintsPassFactory(registrar: TextEditorHighlightingPassRegistrar) : TextEditorHighlightingPassFactory {
  init {
    registrar.registerTextEditorHighlightingPass(this, null, null, false, -1)
  }

  override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
    if (editor.isOneLineMode ||
        file !is PsiJavaFile ||
        modificationStampHolder.isNotChanged(editor, file)) return null
    return AnnotationHintsPass(file, editor, modificationStampHolder)
  }

  companion object {
    val modificationStampHolder: ModificationStampHolder = ModificationStampHolder(Key.create<Long>("LAST_PASS_MODIFICATION_TIMESTAMP"))
  }
}