/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.analysis;

import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.languages.java.ast.*;

import java.lang.Error;

import cuchaz.enigma.mapping.*;

public class SourceIndexBehaviorVisitor extends SourceIndexVisitor {

    private BehaviorEntry behaviorEntry;

    // TODO: Really fix Procyon index problem with inner classes
    private int argumentPosition;

    public SourceIndexBehaviorVisitor(BehaviorEntry behaviorEntry) {
        this.behaviorEntry = behaviorEntry;
        this.argumentPosition = 0;
    }

    @Override
    public Void visitInvocationExpression(InvocationExpression node, SourceIndex index) {
        MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);

        // get the behavior entry
        ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
        BehaviorEntry behaviorEntry = null;
        if (ref instanceof MethodReference) {
            MethodReference methodRef = (MethodReference) ref;
            if (methodRef.isConstructor()) {
                behaviorEntry = new ConstructorEntry(classEntry, new Signature(ref.getErasedSignature()));
            } else if (methodRef.isTypeInitializer()) {
                behaviorEntry = new ConstructorEntry(classEntry);
            } else {
                behaviorEntry = new MethodEntry(classEntry, ref.getName(), new Signature(ref.getErasedSignature()));
            }
        }
        if (behaviorEntry != null) {
            // get the node for the token
            AstNode tokenNode = null;
            if (node.getTarget() instanceof MemberReferenceExpression) {
                tokenNode = ((MemberReferenceExpression) node.getTarget()).getMemberNameToken();
            } else if (node.getTarget() instanceof SuperReferenceExpression) {
                tokenNode = node.getTarget();
            } else if (node.getTarget() instanceof ThisReferenceExpression) {
                tokenNode = node.getTarget();
            }
            if (tokenNode != null) {
                index.addReference(tokenNode, behaviorEntry, this.behaviorEntry);
            }
        }

        return recurse(node, index);
    }

    @Override
    public Void visitMemberReferenceExpression(MemberReferenceExpression node, SourceIndex index) {
        MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
        if (ref != null) {
            // make sure this is actually a field
            if (ref.getErasedSignature().indexOf('(') >= 0) {
                throw new Error("Expected a field here! got " + ref);
            }

            ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
            FieldEntry fieldEntry = new FieldEntry(classEntry, ref.getName(), new Type(ref.getErasedSignature()));
            index.addReference(node.getMemberNameToken(), fieldEntry, this.behaviorEntry);
        }

        return recurse(node, index);
    }

    @Override
    public Void visitSimpleType(SimpleType node, SourceIndex index) {
        TypeReference ref = node.getUserData(Keys.TYPE_REFERENCE);
        if (node.getIdentifierToken().getStartLocation() != TextLocation.EMPTY) {
            ClassEntry classEntry = new ClassEntry(ref.getInternalName());
            index.addReference(node.getIdentifierToken(), classEntry, this.behaviorEntry);
        }

        return recurse(node, index);
    }

    @Override
    public Void visitParameterDeclaration(ParameterDeclaration node, SourceIndex index) {
        ParameterDefinition def = node.getUserData(Keys.PARAMETER_DEFINITION);
        if (def.getMethod() instanceof MemberReference)
            if (def.getMethod() instanceof MethodReference)
                index.addDeclaration(node.getNameToken(),
                        new ArgumentEntry(ProcyonEntryFactory.getBehaviorEntry((MethodReference) def.getMethod()),
                                argumentPosition++, node.getName()));

        return recurse(node, index);
    }

    @Override
    public Void visitIdentifierExpression(IdentifierExpression node, SourceIndex index) {
        MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
        if (ref != null) {
            ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
            FieldEntry fieldEntry = new FieldEntry(classEntry, ref.getName(), new Type(ref.getErasedSignature()));
            index.addReference(node.getIdentifierToken(), fieldEntry, this.behaviorEntry);
        }

        return recurse(node, index);
    }

    @Override
    public Void visitObjectCreationExpression(ObjectCreationExpression node, SourceIndex index) {
        MemberReference ref = node.getUserData(Keys.MEMBER_REFERENCE);
        if (ref != null) {
            ClassEntry classEntry = new ClassEntry(ref.getDeclaringType().getInternalName());
            ConstructorEntry constructorEntry = new ConstructorEntry(classEntry, new Signature(ref.getErasedSignature()));
            if (node.getType() instanceof SimpleType) {
                SimpleType simpleTypeNode = (SimpleType) node.getType();
                index.addReference(simpleTypeNode.getIdentifierToken(), constructorEntry, this.behaviorEntry);
            }
        }

        return recurse(node, index);
    }
}
