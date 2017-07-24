package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtLambdaExpression

class SpacingAroundCurlyRule : Rule("curly-spacing") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node is LeafPsiElement && !node.isPartOfString()) {
            val prevLeaf = PsiTreeUtil.prevLeaf(node, true)
            val nextLeaf = PsiTreeUtil.nextLeaf(node, true)
            val spacingBefore: Boolean
            val spacingAfter: Boolean
            if (node.textMatches("{")) {
                spacingBefore = prevLeaf is PsiWhiteSpace || (prevLeaf?.node?.elementType == KtTokens.LPAR &&
                    (node.parent is KtLambdaExpression || node.parent.parent is KtLambdaExpression))
                spacingAfter = nextLeaf is PsiWhiteSpace || nextLeaf?.node?.elementType == KtTokens.RBRACE
            } else
            if (node.textMatches("}")) {
                spacingBefore = prevLeaf is PsiWhiteSpace || prevLeaf?.node?.elementType == KtTokens.LBRACE
                spacingAfter = nextLeaf == null || nextLeaf is PsiWhiteSpace || shouldNotToBeSeparatedBySpace(nextLeaf)
                if (nextLeaf is PsiWhiteSpace && !nextLeaf.textContains('\n') &&
                        shouldNotToBeSeparatedBySpace(PsiTreeUtil.nextLeaf(nextLeaf, true))) {
                    emit(node.startOffset, "Unexpected space after \"${node.text}\"", true)
                    if (autoCorrect) {
                        nextLeaf.delete()
                    }
                }
            } else {
                return
            }
            when {
                !spacingBefore && !spacingAfter -> {
                    emit(node.startOffset, "Missing spacing around \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                        node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                }
                !spacingBefore -> {
                    emit(node.startOffset, "Missing spacing before \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.rawInsertBeforeMe(PsiWhiteSpaceImpl(" "))
                    }
                }
                !spacingAfter -> {
                    emit(node.startOffset + 1, "Missing spacing after \"${node.text}\"", true)
                    if (autoCorrect) {
                        node.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
                    }
                }
            }
        }
    }

    fun shouldNotToBeSeparatedBySpace(leaf: PsiElement?): Boolean {
        val nextElementType = leaf?.node?.elementType
        return nextElementType != null &&
            nextElementType == KtTokens.DOT ||
            nextElementType == KtTokens.COMMA ||
            nextElementType == KtTokens.RPAR ||
            nextElementType == KtTokens.SEMICOLON ||
            nextElementType == KtTokens.SAFE_ACCESS ||
            nextElementType == KtTokens.EXCLEXCL
    }

}
